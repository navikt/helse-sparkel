FROM navikt/java:10

ENV APP_BINARY=helse-oppslag
COPY build/install/helse-oppslag/ .
