
.PHONY: wmos-s3-msk-build
wmos-s3-msk-build:
	sbt "clean; assembly"
