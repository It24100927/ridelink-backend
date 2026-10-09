# RideLink Account Service

Spring Boot account service for RideLink. It provides account registration,
login, profile management, and administrator-controlled account status.

## Build and test

Requires JDK 21 and Maven 3.9 or newer.

```powershell
mvn -pl account-service -am test
mvn -pl account-service -am package
```

## Run

Set `MONGODB_URI` and `RIDE_LINK_JWT_SECRET` in the environment. To bootstrap an
administrator, also set `ADMIN_EMAIL` and `ADMIN_PASSWORD`; the password must
contain at least 12 characters. Keep these values out of source control.

```powershell
$env:MONGODB_URI = "mongodb://localhost:27017"
$env:RIDE_LINK_JWT_SECRET = "<set-a-strong-shared-secret>"
$env:ADMIN_EMAIL = "admin@example.test"
$env:ADMIN_PASSWORD = "<set-a-private-password>"
mvn -pl account-service -am spring-boot:run
```

The service listens on port 8001 by default. Swagger UI is available at
`/swagger-ui/index.html`.

## Endpoints

- `POST /accounts/register` creates a passenger or driver account.
- `POST /accounts/login` returns a bearer token.
- `GET /accounts/me` and `PATCH /accounts/me` read and update the authenticated
  user's profile.
- `PATCH /accounts/status` lets an administrator activate or suspend an account.

Passwords are salted and hashed; login responses and profile responses never
include password hashes. Email uniqueness is enforced by MongoDB.