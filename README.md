# RideLink — Java Spring Boot Microservices

Backend implementation for the IT3130 Application Development group assignment.

## Assignment conditions

1. All four core microservices are developed in **Java with Spring Boot**.
2. **MERN is not used or permitted** for this assignment.
3. This implementation uses MongoDB Atlas.
4. Each microservice owns its **own independent MongoDB database**. The services connect to the same Atlas cluster but use separate databases; they do not share collections or query one another's data.
5. **No frontend is required**. Demonstrate APIs using each service's Swagger UI and the shared Postman collection.

## Services and ownership

| Microservice | Owner (fill in) | Port | Independent data store |
|---|---|---:|---|
| Account Service | IT24100927 | 8001 | MongoDB `ridelink_accounts` |
| Driver & Vehicle Service | IT24101181 | 8002 | MongoDB `ridelink_drivers` |
| Ride Management Service | IT24100836 | 8003 | MongoDB `ridelink_rides` |
| Fare & Payment Service | IT24100651 | 8004 | MongoDB `ridelink_payments` |

## Requirements

- JDK 21 or newer (Spring Boot 3.5.6)
- Maven 3.9+

## Build and run

From the project root, build all modules:

```powershell
mvn clean package
```

Set the same signing secret in each service process. In each local PowerShell terminal:

```powershell
$env:RIDE_LINK_JWT_SECRET = "local-demo-only-change-me"
$env:MONGODB_URI = "mongodb+srv://<username>:<new-password>@<cluster-host>/?appName=<app-name>"
```

To create the administrator account, set these two variables in the **account-service terminal only**, before starting that service. Use a strong password of at least 12 characters:

```powershell
$env:ADMIN_EMAIL = "admin@ridelink.local"
$env:ADMIN_PASSWORD = "<choose-a-strong-password>"
```

At startup, Account Service creates this admin account if the email is unused, or resets the password of the existing admin account to the configured `ADMIN_PASSWORD`. If that email already belongs to a non-admin account, startup fails; choose a different admin email. Admin accounts cannot be created through public registration. Log in through `POST http://localhost:8001/accounts/login` using the configured email and password, then authorize Swagger with the returned `access_token` to call admin-only operations such as `PATCH /accounts/status`. Keep the bootstrap password private and out of Git.

Start each service in a separate terminal from the project root:

```powershell
java -jar account-service/target/account-service-1.0.0.jar
java -jar driver-service/target/driver-service-1.0.0.jar
java -jar ride-service/target/ride-service-1.0.0.jar
java -jar fare-service/target/fare-service-1.0.0.jar
```

Set `MONGODB_URI` in each terminal to your Atlas connection URI. Do not paste credentials into source code or commit them. If a password contains reserved URI characters, percent-encode them. Each service's `spring.data.mongodb.database` setting selects its own database name; change it only if you intentionally want a different independent database. Atlas network access must permit the machine running the services.

## Swagger UI and OpenAPI

- Account: http://localhost:8001/swagger-ui/index.html
- Driver & Vehicle: http://localhost:8002/swagger-ui/index.html
- Ride Management: http://localhost:8003/swagger-ui/index.html
- Fare & Payment: http://localhost:8004/swagger-ui/index.html

OpenAPI JSON is available at `/v3/api-docs` on each service. Swagger UI is the primary interface for exploring and exercising endpoints; Postman demonstrates the integrated workflow.

## Postman demo

Import `postman/RideLink.postman_collection.json`; the included environment is `postman/RideLink.local.postman_environment.json`. Run requests in order: register passenger and driver; log in; create driver profile and set availability; request a fare estimate; request and assign a ride; accept, start, and complete it; record the simulated payment; retrieve its receipt. The collection includes negative cases for no available driver and an invalid lifecycle transition. Use a fresh local database if replaying registrations.

For a local-only demonstration, the collection uses fictional accounts with password `RideLink123!`. Do not reuse these credentials.

## Tests, documentation, and contribution

Run tests for all modules with `mvn test`. The GitHub Actions workflow builds and tests the multi-module project on pushes and pull requests to `main`.

Architecture and sequence diagrams are in `docs/`. `docs/technical-report.md` is an editable report draft; complete group-specific evidence and export the final report to PDF. Replace TODO owner names, add the group's real repository and contribution evidence, and capture actual CI and demonstration results. Use feature branches, pull requests, meaningful commits, and peer review as agreed by the group. Do not invent contribution records or test evidence.

## Scope and limitations

Locations, drivers, rides, fares, and payments are fictional/simulated. The fare is LKR 3.00 base plus LKR 2.00 per simulated kilometre. Driver assignment selects the first available driver in the requested service area. Interservice calls use synchronous REST and bearer tokens. This educational backend does not integrate real maps or payments.
