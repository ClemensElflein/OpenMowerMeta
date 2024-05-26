FROM python:3.11-alpine as build
LABEL authors="Clemens Elfine"
WORKDIR /app

ENV POETRY_NO_INTERACTION=1 \
    POETRY_VIRTUALENVS_IN_PROJECT=1 \
    POETRY_VIRTUALENVS_CREATE=1 \
    POETRY_CACHE_DIR=/tmp/poetry_cache

RUN pip install poetry==1.8.3
COPY pyproject.toml poetry.* ./
RUN poetry install --no-root && rm -rf $POETRY_CACHE_DIR

FROM python:3.11-alpine as run

ENV VIRTUAL_ENV=/app/.venv \
    PATH="/app/.venv/bin:$PATH"

COPY --from=build ${VIRTUAL_ENV} ${VIRTUAL_ENV}

COPY **.py ./app
WORKDIR /app
ENTRYPOINT ["uvicorn", "main:app", "--port", "8000", "--host", "0.0.0.0"]