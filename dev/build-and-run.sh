#!/bin/bash
set -eu

IMAGE=enrichment-flink:1.20.0

cd "$(dirname "$0")"

build_flink_jar() {
  (cd .. && ./gradlew :clean :shadowJar)
  cp -r ../build/libs ./build
}

build_docker_image() {
  echo "Building docker image for $IMAGE"
  docker build -t "$IMAGE" -f ../Dockerfile .
}

setup_volumes() {
  if [ ! -d "kafka-data" ]; then
    mkdir -p kafka-data
  else
    echo "kafka-data directory already exists."
  fi
  chmod -R 777 kafka-data
}

make_script_executable() {
  if [ -f "send-kafka-messages.sh" ]; then
    chmod +x send-kafka-messages.sh
  else
    exit 1
  fi
}

clean_up() {
  echo "Stopping and removing any existing containers..."
  docker-compose down -v --remove-orphans
}

start_hcpe_locally() {
  setup_volumes
  docker-compose up --force-recreate --remove-orphans
}

clean_up
build_flink_jar
build_docker_image
make_script_executable
start_hcpe_locally