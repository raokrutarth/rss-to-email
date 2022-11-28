TIME_NOW := $(shell date +%Hh-%b-%d-%Y)

DC_BUILD_TAG ?= "newssnips-datacruncher:$(TIME_NOW)"
WA_BUILD_TAG ?= "krk91/personal:newssnips-webapp-$(TIME_NOW)"

DC_CONTAINER_NAME ?= "rss-dc"

KOYEB_API_KEY := $(shell cat koyeb.secrets)
KOYEB_APP_NAME = "disappointed-ilyse"
KOYEB_SERVICE_NAME = "personal"

KOYEB = docker run --rm -it \
	koyeb/koyeb-cli:v2.10.0 --token $(KOYEB_API_KEY)

wa-build:
	./scripts/build-webapp.sh $(WA_BUILD_TAG)

wa-push-release:
	# docker push $(WA_BUILD_TAG)
	# $(KOYEB) apps update $(KOYEB_APP_NAME)
	# services redeploy NAME
	# wait, get new deployment, tail logs

wa-logs:
	# $(KOYEB) instances logs $(KOYEB_APP_NAME)
	$(KOYEB) services logs $(KOYEB_APP_NAME)/$(KOYEB_SERVICE_NAME)

wa-init:
	# services create NAME
	# app init my-app --docker wa/demo --ports 3000:http --routes /:3000
	# services update

wa-ls:
	$(KOYEB) apps list
	$(KOYEB) services list
	$(KOYEB) instances list
	$(KOYEB) app describe $(KOYEB_APP_NAME)

wa-secrets-update:
	./webapp/scripts/prod-deploy-config-str.sh > deploy.secrets.env
	$(KOYEB) service update $(KOYEB_APP_NAME)/$(KOYEB_SERVICE_NAME) $$(cat deploy.secrets.env)
	rm -rf deploy.secrets.env

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

dc-stop:
	docker rm -f $(DC_CONTAINER_NAME)

dc-logs:
	-docker logs --tail=50 -f $(DC_CONTAINER_NAME)

dc-redeploy:
	./scripts/build-datacruncher.sh $(DC_BUILD_TAG)
	
	-docker rm -f $(DC_CONTAINER_NAME)
	
	docker run \
		--detach \
		--name $(DC_CONTAINER_NAME) \
		--restart "unless-stopped" \
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
