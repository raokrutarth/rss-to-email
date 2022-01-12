KCTL ?= minikube kubectl --
NS ?= rte-ns

HRK_APP ?= --app newssnips
TIME_NOW := $(shell date +%Hh-%b-%d-%Y)

DC_BUILD_TAG ?= "newssnips-datacruncher:$(TIME_NOW)"
WA_BUILD_TAG ?= "newssnips-webapp:$(TIME_NOW)"
HEROKU_BUILD_TAG ?= "registry.heroku.com/newssnips/web"

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
	docker tag $(WA_BUILD_TAG) $(HEROKU_BUILD_TAG)

heroku-push-release:
	docker push $(HEROKU_BUILD_TAG)
	heroku container:release web $(HRK_APP)

heroku-logs:
	-heroku logs $(HRK_APP) --num=500 --tail

heroku-request-logs:
	# in the last 10k logs, count requests per endpoint
	- heroku logs $(HRK_APP) \
		--num=10000 | grep path | grep -v -E "(asset|well)" | \
		grep -oP 'path=".*" h' | sort | uniq -c | sort -n

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
