# from https://github.com/jupyter/docker-stacks/tree/master/base-notebook
# and https://raw.githubusercontent.com/jupyter/docker-stacks/master/pyspark-notebook/Dockerfile
ARG ROOT_CONTAINER=ubuntu:focal

FROM $ROOT_CONTAINER

ARG NB_USER="prod"
ARG NB_UID="1000"
ARG NB_GID="100"

# Fix DL4006
SHELL ["/bin/bash", "-o", "pipefail", "-c"]

USER root

# Install all OS dependencies for notebook server that starts but lacks all
# features (e.g., download as all possible file formats)
# Install tini: init for containers
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update --yes && \
    apt-get install --yes --no-install-recommends \
    tini \
    ca-certificates \
    locales \
    default-jdk \
    scala \
    vim \
    curl \
    wget

RUN apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    echo "en_US.UTF-8 UTF-8" > /etc/locale.gen && \
    locale-gen

# Configure environment
ENV SHELL=/bin/bash \
    NB_USER="${NB_USER}" \
    NB_UID=${NB_UID} \
    NB_GID=${NB_GID} \
    LC_ALL=en_US.UTF-8 \
    LANG=en_US.UTF-8 \
    LANGUAGE=en_US.UTF-8

RUN useradd -l -m -s /bin/bash -N -u "${NB_UID}" "${NB_USER}"

ENV HOME="/home/${NB_USER}"

# Spark dependencies
# Default values can be overridden at build time
# (ARGS are in lower case to distinguish them from ENV)
ARG spark_version="3.1.2"
ARG hadoop_version="3.2"
ARG openjdk_version="11"

ENV APACHE_SPARK_VERSION="${spark_version}" \
    HADOOP_VERSION="${hadoop_version}"

RUN apt-get install --yes --no-install-recommends \
    "openjdk-${openjdk_version}-jre-headless" \
    ca-certificates-java && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Spark installation
WORKDIR /tmp
RUN wget "https://archive.apache.org/dist/spark/spark-${APACHE_SPARK_VERSION}/spark-${APACHE_SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz" && \
    tar xzf "spark-${APACHE_SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz" -C /usr/local --owner root --group root --no-same-owner && \
    rm "spark-${APACHE_SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz"

WORKDIR /usr/local

# Configure Spark
ENV SPARK_HOME=/usr/local/spark
ENV SPARK_OPTS="--driver-java-options=-Xms1024M --driver-java-options=-Xmx4096M --driver-java-options=-Dlog4j.logLevel=info" \
    PATH="${PATH}:${SPARK_HOME}/bin"

RUN ln -s "spark-${APACHE_SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}" spark && \
    # Add a link in the before_notebook hook in order to source automatically PYTHONPATH
    mkdir -p /usr/local/bin/before-notebook.d && \
    ln -s "${SPARK_HOME}/sbin/spark-config.sh" /usr/local/bin/before-notebook.d/spark-config.sh

# Fix Spark installation for Java 11 and Apache Arrow library
# see: https://github.com/apache/spark/pull/27356, https://spark.apache.org/docs/latest/#downloading
RUN cp -p "${SPARK_HOME}/conf/spark-defaults.conf.template" "${SPARK_HOME}/conf/spark-defaults.conf" && \
    echo 'spark.driver.extraJavaOptions -Dio.netty.tryReflectionSetAccessible=true' >> "${SPARK_HOME}/conf/spark-defaults.conf" && \
    echo 'spark.executor.extraJavaOptions -Dio.netty.tryReflectionSetAccessible=true' >> "${SPARK_HOME}/conf/spark-defaults.conf"

# scala-coursier
ENV COURSIER_INSTALL_DIR /home/${NB_USER}/coursier
RUN mkdir -p "${COURSIER_INSTALL_DIR}"
RUN set -x && \
    curl -fLo /tmp/cs https://git.io/coursier-cli-"$(uname | tr LD ld)" && \
    chmod +x /tmp/cs && \
    ls ${COURSIER_INSTALL_DIR} && \
    /tmp/cs install cs && \
    rm /tmp/cs && \
    echo "export PATH=\"$PATH:${COURSIER_INSTALL_DIR}\"" >> /home/${NB_USER}/.bashrc && \
    ${COURSIER_INSTALL_DIR}/cs setup -y --jvm adopt:11

WORKDIR /opt/rss-to-email
RUN chown ${NB_USER} /opt/rss-to-email
EXPOSE 9000

COPY ./app ./app
COPY ./conf ./conf
COPY ./project ./project
COPY ./public ./public
COPY ./test ./test
# COPY ./target .
COPY ./build.sbt .

# USER ${NB_UID}
# Configure container startup
ENTRYPOINT ["/bin/bash"]
# CMD [ "compile", "run" ]
