# Event Ledger System

This repository contains two Spring Boot microservices:

- `event-gateway`: public-facing event ingestion gateway
- `account-service`: internal account transaction processor

## Architecture

- `event-gateway` receives transaction events, validates them, stores them locally, and forwards transaction application requests to `account-service`.
- `account-service` manages account transactions, balances, and account-level queries.

## Docker Setup

The project includes Dockerfiles for both services and a `docker-compose.yml` at the repository root.

To build and run both services with Docker Compose:

```bash
cd /workspaces/event-ledger-system
docker compose up --build
```

Services will be available at:

- Gateway: `http://localhost:8080`
- Account Service: `http://localhost:8081`

## Manual Build

Each service can also be built with its Maven wrapper:

```bash
cd account-service
./mvnw package

cd ../event-gateway
./mvnw package
```

Then run each service from its directory:

```bash
java -jar target/account-service-0.0.1-SNAPSHOT.jar
java -jar target/event-gateway-0.0.1-SNAPSHOT.jar
```

## Notes

- The Gateway forwards account transaction requests to `http://account-service:8081`.
- Docker Compose waits for the account service to become healthy before starting the gateway.
