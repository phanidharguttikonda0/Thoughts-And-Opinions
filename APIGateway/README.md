# API Gateway

The API Gateway is the central entry point for the Thoughts & Opinions Social Media Application. It acts as a reverse proxy, routing incoming REST HTTP requests from frontend clients (web, mobile) to the appropriate backend gRPC microservices (such as `IdentityService` and `ThoughtsService`).

## Features
- **REST to gRPC Translation:** Exposes RESTful endpoints for clients, and communicates with internal microservices using high-performance gRPC.
- **Authentication & Authorization:** Verifies JSON Web Tokens (JWT) using a global filter to secure endpoints and extract user context before forwarding requests.
- **Centralized Error Handling:** Uses a Global Exception Handler (`@RestControllerAdvice`) to elegantly translate gRPC `StatusRuntimeException` errors into standard HTTP status codes and strict JSON `ResponseDTO` payloads.
- **Standardized DTOs:** Enforces a strict `ResponseDTO<T>` format across all APIs, ensuring frontend clients receive a predictable JSON structure for both successes and failures.

## Tech Stack
- **Java 21**
- **Spring Boot 3.x (WebFlux / Cloud Gateway)**
- **gRPC & Protocol Buffers**
- **Maven**

## Configuration
The service is configured via `src/main/resources/application.yaml`.

Key configurations:
- `identity.grpc.host`: Host for the Identity Service (default: `localhost`)
- `identity.grpc.port`: Port for the Identity Service (default: `9090`)
- `jwt.secret`: Base64 encoded secret for JWT validation

## Running the Service
1. **Ensure gRPC dependencies are compiled**:
   ```bash
   ./mvnw clean compile
   ```
2. **Start the application**:
   ```bash
   ./mvnw spring-boot:run
   ```
