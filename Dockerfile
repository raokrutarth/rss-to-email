##### Stage 1 #####
FROM pypy:3.7 AS builder

ENV APP_ROOT_DIR="/opt/rss-to-email"
RUN mkdir -p $APP_ROOT_DIR
WORKDIR $APP_ROOT_DIR

# poetry env vars
ENV POETRY_NO_INTERACTION=1
ENV POETRY_HOME="/opt/poetry"
ENV POETRY_VIRTUALENVS_IN_PROJECT=true
# Install Poetry
RUN curl -sSL https://raw.githubusercontent.com/python-poetry/poetry/master/get-poetry.py | python

# Update path with poetry executables
ENV PATH "${POETRY_HOME}/bin:${APP_ROOT_DIR}/.venv/bin:${PATH}"

# install deps in specific folder so they can be copied
COPY ./pyproject.toml ./poetry.lock ${APP_ROOT_DIR}/
RUN poetry install --no-dev

############### Stage 2  ####################
FROM builder as execution

# add non-root user and give access to source code.
RUN groupadd -r reader \
    && useradd -r -s /bin/false -g reader reader

WORKDIR ${APP_ROOT_DIR}

COPY --from=builder --chown=reader:reader ${POETRY_HOME} ${POETRY_HOME}
COPY --from=builder --chown=reader:reader ${APP_ROOT_DIR}/.venv ${APP_ROOT_DIR}/.venv

COPY --chown=reader:reader ./app ./app
COPY --chown=reader:reader ./scripts/start.sh ./start.sh

USER reader
ENV RUNTIME_MODE "docker"
ENV TZ "US/Pacific"

# ENV RUN_GUNICORN true
EXPOSE 5000
ENTRYPOINT ["/opt/rss-to-email/start.sh"]
