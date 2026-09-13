# Identity Service

Welcome to the Identity Service! This is a core gRPC microservice responsible for handling everything related to users in our Social Media Application. It acts as the single source of truth for user authentication, profile management, and the social graph (who is following whom). 

This README is designed to explain not just *what* the service does, but *how* and *why* we built it this way. I've broken down our technical decisions so that any developer jumping into this codebase can easily understand our architecture.

---

## 🔌 gRPC API Contract (Functionalities)

Unlike a traditional REST API, this service uses **gRPC** for high-performance, strongly-typed communication. We define our API contract in the `identity.proto` file. 

Here is the complete list of functionalities this service provides, along with exactly what we need to send in the Request Body and what we get back in the Response:

### 1. User Authentication
*   **SignUp (`SignUpRequest` ➔ `AuthResponse`)**
    *   **Request:** `email` (string), `username` (string), `password` (string), `name` (string)
    *   **Response:** `user_id` (int64), `jwt_token` (string)
    *   *Behavior:* Creates a new user, hashes their password securely, stores them in the database, and returns a JSON Web Token (JWT) for future requests.
*   **Login (`LoginRequest` ➔ `AuthResponse`)**
    *   **Request:** `username` (string), `password` (string)
    *   **Response:** `user_id` (int64), `jwt_token` (string)
    *   *Behavior:* Validates the credentials against the database. If correct, returns a JWT.

### 2. Profile Management
*   **UpdateProfile (`UpdateProfileRequest` ➔ `Empty`)**
    *   **Request:** `user_id` (int64), plus optional fields: `name`, `bio`, `avatar_url`, `username`
    *   **Response:** `Empty` (Just a success acknowledgment)
    *   *Behavior:* Performs a partial update (PATCH). Only the fields that are provided will be updated in the database.
*   **GetUserProfile (`GetUserRequest` ➔ `ProfileData`)**
    *   **Request:** `user_id` (int64)
    *   **Response:** `user_id`, `username`, `name`, `bio`, `profile_pic`, `followers_count`, `following_count`, `joined_at`
    *   *Behavior:* Retrieves all the public statistics and information needed to render a user's profile page.

### 3. Social Graph (Followers)
*   **FollowUser (`FollowRequest` ➔ `Empty`)**
    *   **Request:** `user_id` (The person following), `target_user_id` (The person being followed)
    *   **Response:** `Empty`
*   **UnfollowUser (`FollowRequest` ➔ `Empty`)**
    *   **Request:** `user_id`, `target_user_id`
    *   **Response:** `Empty`
*   **GetFollowersList (`UsersListRequest` ➔ `UsersListResponse`)**
    *   **Request:** `user_id` (int64), `limit` (int32 - max users to return), `cursor` (string - for pagination)
    *   **Response:** A list of `UserDetails` (id, username, name, profile pic), and a `next_cursor` (string).
*   **GetFollowingList (`UsersListRequest` ➔ `UsersListResponse`)**
    *   **Request:** `user_id` (int64), `limit` (int32), `cursor` (string)
    *   **Response:** A list of `UserDetails`, and a `next_cursor`.

### 4. Search
*   **SearchUsers (`SearchRequest` ➔ `SearchResponse`)**
    *   **Request:** `query` (string - e.g., typing a prefix of a username)
    *   **Response:** A list of `UserDetails` matching the prefix.

---

## 🧠 Key Technical Features & How They Work

### 1. Password Hashing & The "Salting" Technique
**Why we need it:** If our database is ever compromised, we do not want attackers to see our users' passwords in plain text. 

**How it works:** We use Spring Security's `BCryptPasswordEncoder` to perform a one-way mathematical hash on passwords before saving them. However, simple hashing isn't enough because hackers use "Rainbow Tables" (massive pre-computed lists of common passwords and their hashes) to crack databases instantly.

To defeat this, we use **Salting**. 
* When a user signs up with the password `password123`, BCrypt automatically generates a completely random string of characters (a "Salt" — for example, `Xr8T`). 
* It combines the salt and the password (`password123Xr8T`) and hashes *that* combination. 
* The salt is then stored securely right alongside the hash in the database. 
* Because the salt is random every time, if two different users both use the password `password123`, they will have **completely different hashes** in our database!
* When logging in, the `passwordEncoder.matches()` function extracts the unique salt from the database row, applies it to the login attempt, hashes it, and checks if it matches.

### 2. Distributed Snowflake ID Generator
**Why we need it:** Traditionally, databases use auto-incrementing numbers (1, 2, 3...) for user IDs. But in a massive distributed system with multiple server instances running in parallel, relying on a single database to generate IDs creates a massive bottleneck. Alternatively, using UUIDs (long random strings) severely hurts database indexing performance because they aren't sequential.

**How it works:** We implemented a custom **Snowflake ID Generator** (originally designed by Twitter). It generates 64-bit numbers mathematically without ever talking to the database. The 64 bits are broken down into:
*   **Timestamp (41 bits):** The current time in milliseconds. This ensures our IDs are always increasing sequentially, which keeps database inserts blazingly fast.
*   **Datacenter ID (5 bits) & Worker ID (5 bits):** We inject unique numbers into each server instance via environment variables. This identifies *which specific server* generated the ID.
*   **Sequence (12 bits):** A counter that increments if the same server generates multiple IDs in the exact same millisecond.

**The result?** Every single server in our backend can generate millions of unique, sequentially sortable IDs per second, completely independently, with **zero risk of collisions** and zero database locking.

### 3. Cursor-Based Pagination
For fetching Followers and Following lists, we do not use traditional page numbers (Offset pagination). In a fast-moving social network, offset pagination causes bugs. If you are on "Page 1" and someone follows you, the list shifts down. When you click "Page 2", you will see duplicate users or skip users entirely.
Instead, we implemented **Cursor-Based Pagination**. We use the exact `created_at` timestamp as a pointer (cursor). When you ask for the next page, you pass the cursor of the last user you saw, and the database simply grabs the next batch of users *older* than that timestamp. It is incredibly fast and completely immune to list-shifting bugs.

---

## 📦 Dependencies & Their Use Cases

We manage our dependencies using Maven (`pom.xml`). Here is a clear breakdown of every major dependency we use and *why* it is in the project:

| Dependency | Purpose in Identity Service |
| :--- | :--- |
| **`spring-boot-starter-actuator`** | Provides production-ready health endpoints. It allows our orchestration tools (like Kubernetes) to ping the service and know if it is healthy, out of memory, or needs to be restarted. |
| **`spring-boot-starter-data-jpa`** | This gives us Hibernate ORM. It allows us to build our database schema entirely using Java classes (like `UserEntity.java`) and handles all the complex SQL queries for us behind the scenes. |
| **`spring-boot-starter-security`** | We don't use this for web routing (since this is gRPC), but we specifically import it to use its industry-standard cryptographic tools, primarily the `BCryptPasswordEncoder` for hashing passwords. |
| **`postgresql`** | The official JDBC driver required for our Java code to communicate natively over the network with our Postgres database using the proprietary Postgres protocol. |
| **`lombok`** | A developer productivity tool. It automatically generates repetitive boilerplate code like Getters, Setters, and Constructors during compilation, keeping our entity classes clean and readable. |
| **`spring-grpc-server-spring-boot-starter`** | The official modern Spring package for hosting gRPC servers. It automatically binds our gRPC service implementations to a port, manages threading, and handles gRPC exception routing out of the box. |
| **`jjwt-api`, `jjwt-impl`, `jjwt-jackson`** | The JSON Web Token libraries. When a user successfully signs up or logs in, we use these to cryptographically sign a payload (containing their User ID) to generate a secure JWT token for API Gateway authentication. |
| **`minio`** | An Amazon S3-compatible object storage client. If users upload profile pictures, this client allows the service to generate pre-signed URLs or communicate with our MinIO bucket to store those images. |
| **`protobuf-maven-plugin`** | This is a build-time plugin. Every time we run `mvn compile`, this tool reads our `identity.proto` file and automatically writes hundreds of lines of complex Java networking code for us, allowing us to just implement the business logic. |
| **`jakarta.annotation-api`** | A necessary library required by modern Java (Java 9+) to successfully compile the classes generated by the protobuf compiler. |

---

## ⚙️ Environment Variables

Create an `application-local.yml` or set the following environment variables to run the service:

| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `SERVER_PORT` | The port for Spring Boot Actuator health checks | `8080` |
| `GRPC_SERVER_PORT` | The port for gRPC traffic | `9090` |
| `DB_URL` | PostgreSQL connection string | `jdbc:postgresql://localhost:5432/identity_db` |
| `DB_USERNAME` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | `secret` |
| `JWT_SECRET` | Secret key for signing tokens | `your-256-bit-secret` |
| `JWT_EXPIRATION` | Token lifespan in milliseconds | `86400000` (24 hours) |

## 🏃‍♂️ Building and Running

1. **Compile Protobufs & Build the Project:**
   ```bash
   ./mvnw clean install
   ```
2. **Run the Server:**
   ```bash
   ./mvnw spring-boot:run
   ```