# from https://github.com/jupyter/docker-stacks/tree/master/base-notebook
# and https://raw.githubusercontent.com/jupyter/docker-stacks/master/pyspark-notebook/Dockerfile
# https://docs.datamechanics.co/docs/docker-images

# FROM bitnami/spark:3.1.2
# FROM gcr.io/datamechanics/spark:platform-3.1-latest
FROM gcr.io/datamechanics/spark:jvm-only-3.1.1-hadoop-3.2.0-java-11-scala-2.12-latest

ARG NB_USER="dev"
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
  wget \
  ca-certificates \
  sudo \
  locales \
  vim \
  unzip \
  curl \
  git \
  time \
  fonts-liberation \
  dnsutils \
  lsof

# Configure environment
ENV SHELL=/bin/bash \
  NB_USER="${NB_USER}" \
  NB_UID=${NB_UID} \
  NB_GID=${NB_GID} 

ENV HOME="/home/${NB_USER}"

# Copy a script that we will use to correct permissions after running certain commands
COPY fix-permissions /usr/local/bin/fix-permissions
RUN chmod a+rx /usr/local/bin/fix-permissions

# Enable prompt color in the skeleton .bashrc before creating the default NB_USER
# hadolint ignore=SC2016
RUN sed -i 's/^#force_color_prompt=yes/force_color_prompt=yes/' /etc/skel/.bashrc

# Create NB_USER with given username user with UID=1000 and in the 'users' group
# and make sure these dirs are writable by the `users` group.
RUN echo "auth requisite pam_deny.so" >> /etc/pam.d/su && \
  sed -i.bak -e 's/^%admin/#%admin/' /etc/sudoers && \
  sed -i.bak -e 's/^%sudo/#%sudo/' /etc/sudoers && \
  useradd -l -m -s /bin/bash -N -u "${NB_UID}" "${NB_USER}" && \
  chmod g+w /etc/passwd && \
  fix-permissions "${HOME}"

RUN echo "${NB_USER}":"${NB_USER}" | chpasswd
# Add new user to sudo group
RUN adduser "${NB_USER}" sudo
# Ensure sudo group users are not asked for 
# a password when using  sudo command by ammending sudoers file
RUN echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers

# Configure Spark
ENV SPARK_OPTS="--driver-java-options=-Dlog4j.logLevel=info" \
  PATH="${PATH}:${SPARK_HOME}/bin"

RUN fix-permissions "${HOME}"
USER ${NB_UID}

# scala-coursier
RUN set -x && curl -fLo /tmp/cs https://git.io/coursier-cli-"$(uname | tr LD ld)" && \
  chmod +x /tmp/cs && \
  /tmp/cs install cs && \
  rm /tmp/cs && \
  /home/${NB_USER}/.local/share/coursier/bin/cs setup -y --jvm adopt:11

ENV PATH="/home/${NB_USER}/.local/share/coursier/bin:$PATH"

# Setup work directory for backward-compatibility
RUN mkdir "/home/${NB_USER}/work" && \
  fix-permissions "${HOME}"


USER root
WORKDIR /tmp
RUN wget https://dlcdn.apache.org/spark/spark-3.1.2/spark-3.1.2-bin-hadoop3.2.tgz -O spark.tgz
RUN tar xvf spark.tgz && rm -rf ${SPARK_HOME}/* && mv spark-3.1.2-bin-hadoop3.2/* $SPARK_HOME && rm -rf spark.tgz

USER ${NB_UID}
ENV TF_CPP_MIN_LOG_LEVEL=2

RUN set -x && \
  fix-permissions "${HOME}"

# Configure container startup
ENTRYPOINT ["/bin/bash"]
CMD [ "-c", "while true; do echo started; sleep 365d; done" ]
WORKDIR "${HOME}"