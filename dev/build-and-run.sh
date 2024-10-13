#!/bin/bash
set -eu

IMAGE=enrichment-flink:1.20.0

build_flink_jar() {
  ./gradlew clean shadowJar
}

build_docker_image() {
  echo "Building docker image for $IMAGE"
  docker build -t "$IMAGE" --no-cache .
}

make_script_executable() {
  chmod +x ./dev/kafka-init/send-kafka-messages.sh
}

clean_up() {
  echo "Stopping and removing any existing containers..."
  docker-compose -f dev/docker-compose.yml down -v --remove-orphans
}

start_hcpe_locally() {
  docker-compose -f dev/docker-compose.yml up --force-recreate --remove-orphans
}

clean_up
build_flink_jar
build_docker_image
make_script_executable
start_hcpe_locally