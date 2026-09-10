# Identity Service

The Identity Service is a core gRPC microservice responsible for user authentication, profile management, and maintaining the social graph (followers/following). 

It acts as the source of truth for user data and is designed for high read-throughput (profile queries) and secure credential handling.

## 🛠️ Tech Stack

*   **Framework:** Spring Boot 4.1.1
*   **Language:** Java 21
*   **RPC Framework:** gRPC (Official Spring Boot gRPC Starter)
*   **Database:** PostgreSQL
*   **ORM:** Spring Data JPA / Hibernate
*   **Object Storage:** MinIO / AWS S3 (for profile pictures)

## 🔌 API Contract (gRPC)

The service implements the `IdentityGatewayService` defined in `identity.proto`. It does not expose REST endpoints directly.

| RPC Method | Input Message | Output Message | Behavior |
| :--- | :--- | :--- | :--- |
| `SignUp` | `SignUpRequest` | `AuthResponse` | Creates user, hashes password, returns JWT. |
| `Login` | `LoginRequest` | `AuthResponse` | Validates credentials, returns JWT. |
| `UpdateProfile` | `UpdateProfileRequest`| `Empty` | Partial updates to bio, name, or avatar (PATCH). |
| `FollowUser` | `FollowRequest` | `Empty` | Creates a follower relationship. |
| `UnfollowUser` | `FollowRequest` | `Empty` | Removes a follower relationship. |
| `GetUserProfile`| `GetUserRequest` | `ProfileData` | Retrieves profile stats and info by username. |
| `SearchUsers` | `SearchRequest` | `SearchResponse`| Returns paginated user search results. |

## 🗄️ Database Schema

The service connects to the `identity_db` PostgreSQL database.

*   `users`: Stores core credentials, profile data, and timestamps.
*   `followers`: A join table with a composite primary key (`follower_id`, `following_id`) tracking the social graph.

## ⚙️ Environment Variables

Create an `application-local.yml` or set the following environment variables to run the service:

| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `SERVER_PORT` | The port for Spring Boot Actuator/Web | `8080` |
| `GRPC_SERVER_PORT` | The port for gRPC traffic | `9090` |
| `DB_URL` | PostgreSQL connection string | `jdbc:postgresql://localhost:5432/identity_db` |
| `DB_USERNAME` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | `secret` |
| `JWT_SECRET` | Secret key for signing tokens | `your-256-bit-secret` |
| `JWT_EXPIRATION` | Token lifespan in milliseconds | `86400000` (24 hours) |

## 🏃‍♂️ Building and Running

1. **Compile Protobufs & Build the Project:**
   Maven will automatically generate the Java classes from the `.proto` files during the compile phase.
   ```bash
   mvn clean install