KCTL ?= minikube kubectl --
NS ?= rte-ns

HRK_APP ?= --app newssnips
TIME_NOW := $(shell date +%Hh-%b-%d-%Y)

DC_BUILD_TAG ?= "newssnips-datacruncher:$(TIME_NOW)"
WA_BUILD_TAG ?= "newssnips-webapp:$(TIME_NOW)"
HEROKU_BUILD_TAG ?= "registry.heroku.com/newssnips/web"

GCLD = docker run --rm -ti \
	-v /home/zee/.gcp:/root/.config/gcloud \
	-v /var/run/docker.sock:/var/run/docker.sock \
	-v ${PWD}:/home/host \
	gcr.io/google.com/cloudsdktool/cloud-sdk \
	bash -c

GCP_WA_IMAGE="us-west1-docker.pkg.dev/newssnips/newssnips/webapp:$(TIME_NOW)"

DC_CONTAINER_NAME ?= "rss-dc"

init-minikube:
	minikube start \
		--driver=docker \
		--cpus='6' \
		--memory='8g' \
		--kubernetes-version=latest
	minikube addons enable metrics-server

wa-build:
	./scripts/build-webapp.sh $(WA_BUILD_TAG)
	# docker tag $(WA_BUILD_TAG) $(GCP_WA_IMAGE)
	docker tag $(WA_BUILD_TAG) $(HEROKU_BUILD_TAG)

heroku-push-release:
	docker push $(HEROKU_BUILD_TAG)
	heroku container:release web $(HRK_APP)

heroku-logs:
	-heroku logs $(HRK_APP) --num=500 --tail

heroku-secrets-update:
	./webapp/scripts/prod-deploy-config-str.sh > heroku.secrets.env
	heroku config:set $(HRK_APP) $$(cat heroku.secrets.env)
	rm -rf heroku.secrets.env

heroku-deploy: wa-build heroku-push-release heroku-logs

aws-init:
	-docker run \
		--rm -it \
		-v ~/.aws:/root/.aws \
		-v ${PWD}:/aws \
		amazon/aws-cli:2.3.0 iam list-users

aws-s3-update:
	# upload public to s3 bucket
	-docker run \
		--rm -it \
		-v ~/.aws:/root/.aws \
		-v ${PWD}:/aws \
		amazon/aws-cli:2.3.0 s3 \
		cp ./webapp/public s3://newssnips-fyi/public \
		--exclude "*.md" --acl public-read --recursive

heroku-redis-enter:
	# heroku redis:maxmemory $(HRK_APP) --policy allkeys-lfu
	heroku redis:info $(HRK_APP)
	heroku redis:cli $(HRK_APP)

heroku-redis-show:
	heroku config:get $(HRK_APP) REDIS_URL

gcp-init:
	# stores the creds in host and sets project.
	$(GCLD) "gcloud auth login && gcloud config set project newssnips"
	$(GCLD) "gcloud config set run/region us-west1"

gcp-cleanup:
	# https://cloud.google.com/container-registry/docs/managing#deleting_images
	echo "delete run-app, image registry, secrets"
	@# https://cloud.google.com/resource-manager/docs/creating-managing-projects#shutting_down_projects

gcp-logs:
	$(GCLD) "gcloud logging read --freshness=30m \
		\"resource.type=cloud_run_revision AND resource.labels.service_name=webapp\" \
		--limit 100" | grep textPayload

gcp-secrets-update:
	gcp=1 ./webapp/scripts/prod-deploy-config-str.sh > gcp.secrets.env
	$(GCLD) "gcloud run services update webapp --region us-west1 --update-env-vars $$(cat gcp.secrets.env)"
	rm -rf gcp.secrets.env

gcp-status:
	$(GCLD) "gcloud run services list"
	$(GCLD) "gcloud run services describe webapp"

wa-push-and-launch:
	$(GCLD) "gcloud auth configure-docker us-west1-docker.pkg.dev --quiet \
	&& docker push $(GCP_WA_IMAGE)"
	
	./webapp/scripts/gcp-config-str.sh > gcp.secrets.env
	$(GCLD) "gcloud run deploy webapp \
		--image $(GCP_WA_IMAGE) \
		--allow-unauthenticated \
		--set-env-vars=$$(cat gcp.secrets.env)"
	rm -rf gcp.secrets.env

# https://cloud.google.com/sdk/gcloud/reference/run/services/update
gcp-deploy: gcp-build gcp-push-and-launch

gcp-scale-update:
	$(GCLD) "gcloud run services update webapp \
		--cpu=2 --memory=1Gi \
		--min-instances=1 --max-instances=1"
	$(GCLD) "gcloud beta run services update webapp --no-cpu-throttling"

# https://cloud.google.com/run/docs/mapping-custom-domains
# check on https://console.cloud.google.com/run/domains?project=newssnips
gcp-domain-mapping:
	$(GCLD) "gcloud domains list-user-verified"
	# $(GCLD) "gcloud domains verify newssnips.fyi"
	# $(GCLD) "gcloud beta run domain-mappings create --service webapp --domain newssnips.fyi"

dc-stop:
	docker rm -f $(DC_CONTAINER_NAME)

dc-logs:
	-docker logs --tail=50 -f $(DC_CONTAINER_NAME)

dc-redeploy:
	./scripts/build-datacruncher.sh $(DC_BUILD_TAG)
	
	-docker rm -f $(DC_CONTAINER_NAME)
	
	docker run -it \
		--detach \
		--name $(DC_CONTAINER_NAME) \
		-e SECRETS_FILE_PATH=/etc/secrets.conf \
		-v "/home/zee/sharp/rss-to-email/datacruncher/secrets.conf":/etc/secrets.conf:ro \
		-e SHARED_SECRETS_FILE_PATH=/etc/shared.secrets.conf \
		-v "/home/zee/sharp/rss-to-email/shared.secrets.conf":/etc/shared.secrets.conf:ro \
		-e PG_CERT_PATH=/etc/pg.crt \
		-v "/home/zee/sharp/rss-to-email/cockroachdb_db.crt":/etc/pg.crt:ro \
		-v "/home/zee/sharp/rss-to-email/datacruncher/models":/etc/models:ro \
		-v "/etc/timezone:/etc/timezone:ro" \
		-v "/etc/localtime:/etc/localtime:ro" \
		$(DC_BUILD_TAG)
	
	docker update --memory=8Gi --cpus=6 $(DC_CONTAINER_NAME)
	make -s dc-logs
