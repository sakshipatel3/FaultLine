.PHONY: build run init-demo

build:
	mvn -B -DskipTests package

run: build
	java -jar target/repo-risk-analyzer-backend.jar

init-demo:
	cd sample-repos/demo-repo && bash init-repo.sh
