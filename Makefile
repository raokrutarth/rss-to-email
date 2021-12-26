KCTL ?= minikube kubectl --
NS ?= rte-ns

stop:
	$(KCTL) delete namespace $(NS)
	
redeploy-app:
	# ./scripts/build.sh
	$(KCTL) -n $(NS) rollout restart deployment rss-to-email-dep

test-app:
	curl http://192.168.49.2:31900/

app-logs:
	$(KCTL) -n $(NS) logs deployment/rss-to-email-dep

redeploy-spark-worker:
	$(KCTL) apply -f deploy/spark.yaml
	$(KCTL) -n $(NS) rollout restart deployments/spark-worker-dep

spark-worker-logs:
	$(KCTL) -n $(NS) describe deployments/spark-worker-dep
	$(KCTL) -n $(NS) logs deployments/spark-worker-dep

spark-master-logs:
	$(KCTL) -n $(NS) describe deployments/spark-master-dep
	$(KCTL) -n $(NS) logs deployments/spark-master-dep

stop-spark:
	$(KCTL) delete -f deploy/spark.yaml

