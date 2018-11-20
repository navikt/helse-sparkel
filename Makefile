DOCKER   := docker
GRADLE   := ./gradlew -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000
VERSION  := $(shell git rev-parse --short HEAD)
IMG_NAME := navikt/helse-oppslag

.PHONY: all build test docker docker-push release

all: build test docker
release: docker-push
	git clone https://github.com/navikt/helse-iac.git

	cd helse-iac; \
	./set-image.sh preprod/helse-oppslag/naiserator.yaml $(IMG_NAME):$(VERSION); \
	git add preprod/helse-oppslag/naiserator.yaml; \
	./set-image.sh prod/helse-oppslag/naiserator.yaml $(IMG_NAME):$(VERSION); \
	git add prod/helse-oppslag/naiserator.yaml; \
	git commit -m "Bump version\nTriggered by $(TRAVIS_BUILD_WEB_URL)"; \
	git push origin master;

build:
	$(GRADLE) installDist

test:
	$(GRADLE) check

docker:
	$(DOCKER) build --pull -t $(IMG_NAME) -t $(IMG_NAME):$(VERSION) .

docker-push:
	$(DOCKER) push $(IMG_NAME):$(VERSION)
