FROM navikt/java:11

ENV APP_BINARY=sparkel
COPY build/install/sparkel/ .
