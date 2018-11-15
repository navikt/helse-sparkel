DOCKER  := docker
GRADLE  := ./gradlew -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000
VERSION := $(shell cat ./VERSION)

.PHONY: all build test docker docker-push bump-version release

all: build test docker
release: tag docker-push

build:
	$(GRADLE) installDist

test:
	$(GRADLE) check

docker:
	$(DOCKER) build --pull -t navikt/helse-oppslag -t navikt/helse-oppslag:$(VERSION) .

docker-push:
	$(DOCKER) push navikt/helse-oppslag:$(VERSION)

bump-version:
	sed 's/navikt\/helse-oppslag:.*/navikt\/helse-oppslag:'$$(($$(cat ./VERSION) + 1))'/' naiserator.yaml > naiserator.yaml.new && mv naiserator.yaml.new naiserator.yaml
	@echo $$(($$(cat ./VERSION) + 1)) > ./VERSION

tag:
	git add VERSION naiserator.yaml
	git commit -m "Bump version to $(VERSION) [skip ci]"
	git tag -a $(VERSION) -m "auto-tag from Makefile"
