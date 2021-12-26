FROM gcr.io/datamechanics/spark:jvm-only-3.1.1-hadoop-3.2.0-java-11-scala-2.12-latest


WORKDIR /tmp
RUN wget https://dlcdn.apache.org/spark/spark-3.1.2/spark-3.1.2-bin-hadoop3.2.tgz -O spark.tgz
RUN tar xvf spark.tgz && \
  rm -rf ${SPARK_HOME}/* && \
  mv spark-3.1.2-bin-hadoop3.2/* $SPARK_HOME && \
  rm -rf spark.tgz

WORKDIR /opt/docker

COPY --chown=daemon:root opt /opt
# COPY --chown=daemon:root 2/opt /opt
# COPY --chown=daemon:root 4/opt /opt

EXPOSE 9000
USER daemon
ENV RUNTIME_ENV "docker"
ENV TF_CPP_MIN_LOG_LEVEL 2
ARG BINARY_FILE
ENV BINARY_FILE=${BINARY_FILE}
# ENV JAVA_OPTS="-Xms100M -Xmx512M -XX:+UseG1GC"
ENTRYPOINT ${BINARY_FILE}
CMD []

