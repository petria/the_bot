FROM golang:1.25-bookworm AS wacli-builder

ARG WACLI_VERSION=v0.8.1
ENV CGO_ENABLED=1
ENV CGO_CFLAGS="-Wno-error=missing-braces"

RUN apt-get update \
  && apt-get install -y --no-install-recommends build-essential ca-certificates \
  && rm -rf /var/lib/apt/lists/*

RUN go install -tags sqlite_fts5 github.com/openclaw/wacli/cmd/wacli@${WACLI_VERSION}

FROM python:3.13-slim

ENV TZ=Europe/Helsinki
ENV WACLI_STORE_DIR=/wacli
ENV BOT_WHATSAPP_PORT=8095
ENV WACLI_SYNC_MAX_DB_SIZE=2GB

RUN apt-get update \
  && apt-get install -y --no-install-recommends ca-certificates sqlite3 \
  && rm -rf /var/lib/apt/lists/*

COPY --from=wacli-builder /go/bin/wacli /usr/local/bin/wacli
COPY server.py /opt/bot-whatsapp/server.py
COPY supervisor.py /opt/bot-whatsapp/supervisor.py
COPY entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh

VOLUME /wacli
EXPOSE 8095

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD python -c "import sys, urllib.request; urllib.request.urlopen('http://127.0.0.1:8095/health', timeout=8); sys.exit(0)"

ENTRYPOINT ["/entrypoint.sh"]
CMD ["serve"]
