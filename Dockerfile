FROM navikt/java:8

ENV APP_BINARY=helse-oppslag
COPY build/install/helse-oppslag/ .
