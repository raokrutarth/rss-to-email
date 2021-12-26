KCTL ?= minikube kubectl --
NS ?= rte-ns

HRK_APP ?= --app newssnips

DC_BUILD_TAG ?= "newssnips-datacruncher:$$(date +%H-%m-%d-%Y)"
WA_BUILD_TAG ?= "newssnips-webapp:$$(date +%H-%m-%d-%Y)"

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

logs-webapp:
	# heroku logs $(HRK_APP) --num=500 --tail
	gcloud logging read --freshness=30m \
		"resource.type=cloud_run_revision AND resource.labels.service_name=webapp" --project newssnips --limit 10

# redeploy-wa:
# 	./scripts/build-webapp.sh $(WA_BUILD_TAG)
# 	docker tag $(WA_BUILD_TAG) registry.heroku.com/newssnips/web
# 	docker push registry.heroku.com/newssnips/web
# 	heroku container:release web $(HRK_APP)
# 	make -s logs-webapp

gcp-login:
	# stores the creds in host.
	docker run \
		--rm \
		-ti \
		-v /home/zee/.gcp:/root/.config/gcloud \
		gcr.io/google.com/cloudsdktool/cloud-sdk \
		gcloud auth login

gcp-secrets-update:
	echo TODO
	# https://cloud.google.com/sdk/gcloud/reference/secrets/create

gcp-status:
	echo TODO
	# gcloud run services describe SERVICE
	# https://cloud.google.com/run/docs/managing/services

gcp-cleanup:
	# https://cloud.google.com/container-registry/docs/managing#deleting_images
	echo "delete run-app, image registry, secrets"

gcp-deploy:
	gcloud run deploy webapp \
		--project newssnips \
		--image us-west1-docker.pkg.dev/newssnips/newssnips/webapp:latest \
		--memory 2Gi \
		--cpu 2 \
		--region us-west1 \
		--allow-unauthenticated \
		--max-instances=1 
	
	gcloud beta run services update webapp --region us-west1 --cpu-throttling 

	docker run \
		--rm \
		-ti \
		-v /home/zee/.gcp:/root/.config/gcloud \
		-v /var/run/docker.sock:/var/run/docker.sock \
		gcr.io/google.com/cloudsdktool/cloud-sdk \
		bash -c \
		"gcloud auth configure-docker us-west1-docker.pkg.dev && \
			gcloud run deploy --no-cpu-throttling --image IMAGE_URL\
			--update-secrets=SECRETS_B64=newssnips-webapp-secrets:latest --memory 1Gi --cpu 2"
	
	docker tag $(WA_BUILD_TAG) us-west1-docker.pkg.dev/newssnips/newssnips/webapp:latest

redeploy-dc:
	./scripts/build.sh $(DC_BUILD_TAG)
	docker tag $(DC_BUILD_TAG) registry.heroku.com/sentipeg/web
	docker push registry.heroku.com/sentipeg/web
	heroku container:release web $(HRK_APP)
	make -s app-logs
