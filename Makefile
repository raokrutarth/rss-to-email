KCTL ?= minikube kubectl --
NS ?= rte-ns

HRK_APP ?= --app sentipeg

DC_BUILD_TAG ?= "newssnips-datacruncher:$$(date +%H-%m-%d-%Y)"

init-minikube:
	minikube start \
		--driver=docker \
		--cpus='6' \
		--memory='8g' \
		--kubernetes-version=latest
	minikube addons enable metrics-server

aws:
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

app-logs:
	heroku logs $(HRK_APP) --num=500 --tail

redeploy-app:
	./scripts/build.sh $(DC_BUILD_TAG)
	docker tag $(DC_BUILD_TAG) registry.heroku.com/sentipeg/web
	docker push registry.heroku.com/sentipeg/web
	heroku container:release web $(HRK_APP)
	make -s app-logs
