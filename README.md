# RideLink Fare & Payment Service

Spring Boot service for fare estimates, simulated ride payments, and payment
receipts. It uses the shared RideLink JWT helpers and an independent MongoDB
database.

## Build and test

Requires JDK 21 and Maven 3.9 or newer.

```powershell
mvn -pl fare-service -am test
mvn -pl fare-service -am package
```

## Run

Set `MONGODB_URI`, `RIDE_LINK_JWT_SECRET`, and `RIDE_URL` in the environment.
`RIDE_URL` should point at Ride Management Service (port 8003 by default).
Keep connection strings and secrets out of source control.

```powershell
$env:MONGODB_URI = "mongodb://localhost:27017"
$env:RIDE_LINK_JWT_SECRET = "<set-a-strong-shared-secret>"
$env:RIDE_URL = "http://127.0.0.1:8003"
mvn -pl fare-service -am spring-boot:run
```

The service listens on port 8004 by default. Swagger UI is available at
`/swagger-ui/index.html`.

## Endpoints

- `POST /fares/estimate` calculates an estimate at LKR 3.00 base plus LKR 2.00
  per simulated kilometre.
- `POST /payments` verifies that the authenticated passenger owns a completed
  ride, then records a simulated payment.
- `POST /payments/receipt` returns a payment receipt to its owner or an
  administrator.

Payments are simulated; no external payment processor is contacted.