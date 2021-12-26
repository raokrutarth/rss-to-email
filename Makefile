KCTL ?= minikube kubectl --
NS ?= rte-ns

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

start:
	$(KCTL) apply -f deploy/rte-ns.yaml
	$(KCTL) apply -f deploy/app.yaml

stop:
	$(KCTL) delete namespace $(NS)

status:
	$(KCTL) -n $(NS) get deployments
	$(KCTL) -n $(NS) get pods

redeploy-app:
	./scripts/build.sh
	$(KCTL) -n $(NS) rollout restart deployment rss-to-email-dep

test-app:
	set -x && curl "$$(minikube service --url -n $(NS) rss-to-email-svc)"

app-logs:
	$(KCTL) -n $(NS) logs deployment/rss-to-email-dep

