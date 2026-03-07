#!/usr/bin/env sh
set -e

CACERTS="/usr/lib/jvm/java-25-amazon-corretto/lib/security/cacerts"
ALIAS="ymparisto"

CERT_PATH=""
if [ -f "/runtime/ymparisto.crt" ]; then
  CERT_PATH="/runtime/ymparisto.crt"
elif [ -f "/config/ymparisto.crt" ]; then
  CERT_PATH="/config/ymparisto.crt"
fi

if [ -n "$CERT_PATH" ]; then
  if keytool -list -keystore "$CACERTS" -storepass changeit -alias "$ALIAS" >/dev/null 2>&1; then
    echo "Certificate alias '$ALIAS' already exists in truststore"
  else
    echo "Importing certificate from $CERT_PATH"
    keytool -importcert \
      -trustcacerts \
      -alias "$ALIAS" \
      -file "$CERT_PATH" \
      -keystore "$CACERTS" \
      -storepass changeit \
      -noprompt
  fi
else
  echo "No ymparisto.crt found in /runtime or /config, starting without custom cert import"
fi

exec java -jar /app.jar
