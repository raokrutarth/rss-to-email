KCTL ?= minikube kubectl --
NS ?= rte-ns

HRK_APP ?= --app sentipeg

BUILD_TAG ?= "rss-to-email:$$(date +%H-%m-%d-%Y)"

init-minikube:
	minikube config set cpus 4
	minikube config set driver docker
	minikube config set memory 16G
	minikube start \
		--driver=docker \
		--cpus='6' \
		--memory='8g' \
		--kubernetes-version=latest
	minikube addons enable metrics-server

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
	./scripts/build.sh $(BUILD_TAG)
	docker tag $(BUILD_TAG) registry.heroku.com/sentipeg/web
	docker push registry.heroku.com/sentipeg/web
	heroku container:release web $(HRK_APP)
	make -s app-logs
