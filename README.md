# Social Network Microservices Architecture

This repository contains the backend microservices architecture for a social platform. The system is built using an API Gateway pattern, where external HTTP/REST traffic is ingested, validated, and routed to internal gRPC-based microservices.

## 🏗️ System Architecture

*   **External Communication:** REST/HTTP endpoints exposed to clients (Web/Mobile).
*   **Internal Communication:** High-performance gRPC over HTTP/2 with Protocol Buffers.
*   **Authentication:** JWT-based stateless authentication validated at the API Gateway.

## 📦 Microservices Overview

| Service | Protocol     | Description | Database |
| :--- |:-------------| :--- | :--- |
| **API Gateway** | REST -> gRPC | Entry point. Handles routing, rate limiting, JWT validation, and translates HTTP requests to gRPC calls. | Redis (Rate Limiting) |
| **Identity Service** | gRPC         | Manages user registration, JWT generation, profiles, and the follower/following social graph. | PostgreSQL |
| **Thoughts Service** | gRPC         | Core content service. Handles creation, retrieval, and interaction (likes/replies) with user posts ("Thoughts"). | PostgreSQL / MongoDB |
| **Timeline Service** | Java/gRPC         | Aggregates Thoughts from followed users to generate the home feed. Fan-out architecture for fast reads. | Redis / Cassandra |
| **Notification Service**| Go/gRPC   | Consumes `notification.events` from Kafka and serves the notifications feed via gRPC. | Redis (Pointer Pattern) |

## 🚀 Getting Started

### Prerequisites
*   Java 21
*   Maven 3.8+
*   Docker & Docker Compose

### Local Development Setup

To run the entire ecosystem locally with their attached databases:

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd <repository-name>
   ```

## 🛡️ Exception Handling Workflow (gRPC to HTTP)

In this architecture, custom business exceptions thrown by backend microservices (e.g., `IdentityService`, `ThoughtsService`) must be translated into standard HTTP responses at the API Gateway. 

### How It Works:
1. **Service-Level Handling:** Each microservice implements a `GlobalGrpcExceptionHandler` (which implements `org.springframework.grpc.server.exception.GrpcExceptionHandler`).
2. **Translation to gRPC Status:** When a custom exception (like `UserNotFoundException` or `AlreadyFollowingException`) is thrown, this handler catches it *before* it leaves the microservice. It maps the custom exception to a standard gRPC `Status` (e.g., `Status.NOT_FOUND` or `Status.ALREADY_EXISTS`) and returns it as a `StatusException`.
3. **Gateway Handling:** The API Gateway receives a `StatusRuntimeException`. The `@RestControllerAdvice` (`GlobalExceptionHandler`) in the Gateway catches this exception, inspects the gRPC status code, and translates it into the appropriate HTTP status code (e.g., 404 Not Found, 409 Conflict, 400 Bad Request).
4. **Final Response:** The user receives a clean, standardized JSON `ResponseDTO` containing the success flag, the error message, and the correct HTTP status code.

## 🧪 Testing & Simulation

To thoroughly test the ecosystem and simulate real-world traffic, you have three primary methods available. All tests assume that your core microservices (API Gateway, Identity Service, and Thoughts Service) are actively running.

### 1. Automated Social Traffic Simulation (`simulate_traffic.sh`)
This script executes a full end-to-end integration test by mimicking a burst of user activity. It requires no external HTTP clients, just a standard bash terminal.

**What it does:**
1. Registers 10 unique users.
2. Logs them in to generate and extract JWT Auth Tokens.
3. User 1 creates a viral "Thought".
4. Users 2 through 10 like the thought.
5. Users 2 through 5 write opinion replies to the thought.
6. Users 6 through 10 follow User 1.
7. Fetches and displays the resulting Likes Feed, Opinions Feed, and Followers Feed.

**How to run it:**
```bash
chmod +x simulate_traffic.sh
./simulate_traffic.sh
```

### 2. Full Edge-Case API Verification (`test_remaining_apis.sh`)
This script tests the remaining "destructive" and edge-case REST endpoints that aren't covered by the main traffic simulation.

**What it covers:**
* Fetching customized User Feeds and specific Profile Data.
* Unliking a thought.
* Listing Reposts.
* Deleting a thought completely.
* Unfollowing a user.
* Fetching the customized Following list.

**How to run it:**
```bash
chmod +x test_remaining_apis.sh
./test_remaining_apis.sh
```

### 3. Manual IDE Testing (`api-tests.http`)
For granular, manual testing, an `api-tests.http` file is provided in the root directory. This is standard format for IDE-based HTTP Clients (like IntelliJ IDEA Premium, or VSCode with the "REST Client" extension).

**How to use:**
1. Open `api-tests.http` in your supported IDE.
2. Ensure you execute the `POST /auth/signin` blocks first. The IDE will automatically capture the returned JWT Token into an environment variable (e.g. `{{token1}}`).
3. Click "Run" next to any subsequent request to test specific behaviors independently.
4. Note: Multipart Form requests (like Profile Picture uploads via `PATCH /profile/`) are commented out and require valid local file path configurations in your IDE to test successfully.