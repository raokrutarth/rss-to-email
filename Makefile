KCTL ?= minikube kubectl --
NS ?= rte-ns

HRK_APP ?= --app newssnips
TIME_NOW := $(shell date +%Hh-%b-%d-%Y)

DC_BUILD_TAG ?= "newssnips-datacruncher:$(TIME_NOW)"
WA_BUILD_TAG ?= "newssnips-webapp:$(TIME_NOW)"

GCLD = docker run --rm -ti \
	-v /home/zee/.gcp:/root/.config/gcloud \
	-v /var/run/docker.sock:/var/run/docker.sock \
	-v ${PWD}:/home/host \
	gcr.io/google.com/cloudsdktool/cloud-sdk \
	bash -c

GCP_WA_IMAGE="us-west1-docker.pkg.dev/newssnips/newssnips/webapp:$(TIME_NOW)"

init-minikube:
	minikube start \
		--driver=docker \
		--cpus='6' \
		--memory='8g' \
		--kubernetes-version=latest
	minikube addons enable metrics-server

aws-users:
	-docker run \
		--rm -it \
		-v ~/.aws:/root/.aws \
		-v ${PWD}:/aws \
		amazon/aws-cli:2.3.0 iam list-users

aws-s3:
	# upload public to s3 bucket
	-docker run \
		--rm -it \
		-v ~/.aws:/root/.aws \
		-v ${PWD}:/aws \
		amazon/aws-cli:2.3.0 s3 \
		cp ./webapp/public s3://newssnips-fyi/public \
		--exclude "*.md" --acl public-read --recursive

heroku-redis:
	# heroku redis:maxmemory $(HRK_APP) --policy allkeys-lfu
	heroku redis:info $(HRK_APP)
	heroku redis:cli $(HRK_APP)

redis-config-show:
	heroku config:get $(HRK_APP) REDIS_URL

# start:
# 	$(KCTL) apply -f deploy/rte-ns.yaml
# 	$(KCTL) apply -f deploy/app.yaml

# stop:
# 	$(KCTL) delete namespace $(NS)

# status:
# 	$(KCTL) -n $(NS) get deployments
# 	$(KCTL) -n $(NS) get pods

# $(KCTL) -n $(NS) rollout restart deployment rss-to-email-dep

# test-app:
# 	set -x && curl "$$(minikube service --url -n $(NS) rss-to-email-svc)"

# stores the creds in host and sets project.
gcp-init:
	$(GCLD) "gcloud auth login && gcloud config set project newssnips"
	$(GCLD) "gcloud config set run/region us-west1"

gcp-cleanup:
	# https://cloud.google.com/container-registry/docs/managing#deleting_images
	echo "delete run-app, image registry, secrets"
	@# https://cloud.google.com/resource-manager/docs/creating-managing-projects#shutting_down_projects

logs-wa:
	$(GCLD) "gcloud logging read --freshness=30m \
		\"resource.type=cloud_run_revision AND resource.labels.service_name=webapp\" \
		--limit 100" | grep textPayload

wa-secrets-update:
	./webapp/scripts/gcp-config-str.sh > gcp.secrets.env
	$(GCLD) "gcloud run services update webapp --region us-west1 --update-env-vars $$(cat gcp.secrets.env)"
	rm -rf gcp.secrets.env

wa-status:
	$(GCLD) "gcloud run services list"
	$(GCLD) "gcloud run services describe webapp"

wa-build:
	./scripts/build-webapp.sh $(WA_BUILD_TAG)
	docker tag $(WA_BUILD_TAG) $(GCP_WA_IMAGE)

wa-push-and-launch:
	$(GCLD) "gcloud auth configure-docker us-west1-docker.pkg.dev --quiet \
	&& docker push $(GCP_WA_IMAGE)"
	
	$(GCLD) "gcloud run deploy webapp \
		--image $(GCP_WA_IMAGE) \
		--allow-unauthenticated"

# https://cloud.google.com/sdk/gcloud/reference/run/services/update
wa-deploy: wa-build wa-push-and-launch

wa-scale-update:
	$(GCLD) "gcloud run services update webapp \
		--cpu=4 --memory=2Gi \
		--min-instances=1 --max-instances=1"
	$(GCLD) "gcloud beta run services update webapp --no-cpu-throttling"

# https://cloud.google.com/run/docs/mapping-custom-domains
# check on https://console.cloud.google.com/run/domains?project=newssnips
domain-mapping:
	$(GCLD) "gcloud domains list-user-verified"
	# $(GCLD) "gcloud domains verify newssnips.fyi"
	# $(GCLD) "gcloud beta run domain-mappings create --service webapp --domain newssnips.fyi"

redeploy-dc:
	./scripts/build.sh $(DC_BUILD_TAG)
	docker tag $(DC_BUILD_TAG) registry.heroku.com/sentipeg/web
	docker push registry.heroku.com/sentipeg/web
	heroku container:release web $(HRK_APP)
	make -s app-logs
