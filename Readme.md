# Flink Enrichment Application
## Design Overview
The Flink enrichment application performs the following key operations:

* **Data Ingestion**: Consumes data from Kafka.
* **ETL Process**: Executes extract, transform, and load (ETL) operations.
* **External Service Fetching**: Retrieves a value from an external service (with caching for the latest response).
  * The caching part has been moved to the message enrichment operator (this approach allows AsyncIO to be used in the fetch operator).
* **Message Enrichment**: Enriches the transformed message with the value fetched from the external service.
* **Data Output**: Sends the enriched message back to Kafka.

## Local Setup
To start the local environment, follow these steps:

* Execute the following command from the root source to build and run the application:
```bash  
chmod +x ./dev/build-and-run.sh && ./dev/build-and-run.sh
```
* The Job Manager UI is accessible at the following URL:
  http://localhost:8081

* The Redpanda (Kafka) UI can be accessed via the following URL:
  http://localhost:8089

## Requirements
* Min Java 17 version
* Docker 
* Docker Compose