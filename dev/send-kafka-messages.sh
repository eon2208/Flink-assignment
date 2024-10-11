#!/bin/bash

kafka-topics --bootstrap-server kafka:9092 --create --if-not-exists --topic input-topic

echo "Continuously sending messages to input-topic..."

counter=1
while true; do
  message="{ \"value\": $counter }"
  echo "$message" | kafka-console-producer --bootstrap-server kafka:9092 --topic input-topic > /dev/null 2>&1
  echo "Sent: $message"
  ((counter++))
  sleep 1
done