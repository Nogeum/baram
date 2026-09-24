ARG BASE_IMAGE=portal:local
FROM ${BASE_IMAGE}
USER 0
RUN mkdir -p /app/data && chown 10001:10001 /app/data
USER 10001:10001
