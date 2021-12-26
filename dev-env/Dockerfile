# slightly bloated image with decent spark installation and 
# file permissions for mounted volumes.
FROM jupyter/all-spark-notebook

USER root

RUN apt-get -y update && \
    apt-get install -y default-jdk scala vim curl

USER jovyan

RUN curl -fLo cs https://git.io/coursier-cli-"$(uname | tr LD ld)" && \
    chmod +x cs && \
    ./cs install cs && \
    rm cs && \
    echo "export PATH=\"$PATH:/home/jovyan/.local/share/coursier/bin\"" >> /home/jovyan/.bashrc

# Freeze JDK version
RUN source /home/jovyan/.bashrc && \
    /home/jovyan/.local/share/coursier/bin/cs setup -y --jvm adopt:11