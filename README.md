# RideLink - Backend Microservices

## Backend Microservices for a Ride-Sharing Platform

RideLink is a backend-only ride-sharing platform developed using a microservices architecture. The system consists of four independently executable services that handle account management, driver and vehicle management, ride management, and fare and payment processing.

The project uses RESTful APIs for communication and Swagger UI/OpenAPI and Postman for API documentation, testing, and demonstration.

---

## Project Information

| Item                  | Details                          |
| --------------------- | -------------------------------- |
| Project Name          | RideLink - Backend Microservices |
| Module                | IT3130 - Application Development |
| Architecture          | Microservices                    |
| Project Type          | Backend                          |
| Programming Framework | Spring Boot                      |
| Database              | MongoDB                          |
| API Style             | RESTful JSON APIs                |
| API Documentation     | Swagger UI / OpenAPI             |
| API Testing           | Postman                          |
| Version Control       | Git / GitHub                     |

---

## Team Members

| Member Name         | Student ID | Responsibility             |
| ------------------- | ---------- | -------------------------- |
| Dissanayake M.A.T.D | IT24100927 | Account Service            |
| H.A.G.H Dilshara    | IT24100651 | Fare and Payment Service   |
| Abeywardana Y.C.Y   | IT24100836 | Ride Management Service    |
| W.G.J Yasith        | IT24101181 | Driver and Vehicle Service |

Each team member is responsible for the development and maintenance of the assigned microservice. The team collaborates on system integration, architecture, testing, and overall project development.

---

## System Architecture

RideLink consists of four core business microservices:

1. **Account Service** - Manages user accounts, authentication, and authorization.
2. **Driver and Vehicle Service** - Manages driver information and vehicle details.
3. **Ride Management Service** - Handles ride requests, driver assignment, and ride status updates.
4. **Fare and Payment Service** - Handles fare calculations and payment processing.

### Architecture Overview

```text
                       RideLink
                          |
        +-----------------+-----------------+
        |                 |                 |
        v                 v                 v
+---------------+ +---------------+ +---------------+
| Account       | | Driver and    | | Ride          |
| Service       | | Vehicle       | | Management    |
| Port: 8081    | | Service       | | Service       |
|               | | Port: 8082    | | Port: 8083    |
+-------+-------+ +-------+-------+ +-------+-------+
        |                 |                 |
        v                 v                 v
   Account DB        Driver/Vehicle DB    Ride DB

                          |
                          v
                 +------------------+
                 | Fare and Payment |
                 | Service          |
                 | Port: 8084        |
                 +--------+---------+
                          |
                          v
                      Payment DB
```

---

## Technologies Used

* Java and Spring Boot for backend service development.
* MongoDB for service-specific data storage.
* REST APIs for communication between services.
* Swagger UI / OpenAPI for API documentation.
* Postman for API testing.
* Git and GitHub for version control and collaboration.

---

## Service Responsibilities

### 1. Account Service

Manages user accounts and account-related operations, including authentication and authorization.

### 2. Driver and Vehicle Service

Maintains driver profiles and vehicle information required for ride-sharing operations.

### 3. Ride Management Service

Manages ride requests, driver assignment, ride acceptance, ride progress, and completion or cancellation.

### 4. Fare and Payment Service

Calculates ride fares and manages payment-related operations associated with rides.

---

## API Documentation and Testing

The microservices expose RESTful endpoints that can be documented and tested using Swagger UI/OpenAPI and Postman.

Each service can be run independently using its configured application settings.

---

## Project Scope

RideLink focuses on backend microservices and API-based functionality for a ride-sharing platform. The system separates business responsibilities across independent services to support modular development, maintainability, and service-level data ownership.

---

## Getting Started

1. Clone the project repository.
2. Install the required Java, Maven, and MongoDB tools.
3. Configure the database connection settings for each microservice.
4. Run each service using its configured port.
5. Access the available Swagger UI endpoints to explore and test the APIs.

**Note:** Update the service ports, database configuration, and startup commands if they differ from your local project setup.
