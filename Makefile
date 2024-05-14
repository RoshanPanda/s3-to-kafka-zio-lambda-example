
.PHONY: s3-msk-build
s3-msk-build:
	sbt "clean; assembly"
