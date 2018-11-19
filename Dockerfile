FROM navikt/java:11

ENV APP_BINARY=helse-oppslag
COPY build/install/helse-oppslag/ .
