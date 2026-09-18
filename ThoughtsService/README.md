# Thoughts Service

Welcome to the Thoughts Service! This is a core gRPC microservice responsible for handling the creation, retrieval, and interaction with "Thoughts" (posts), Opinions (comments), Reposts, and Likes in our Social Media Application.

This README breaks down our technical decisions so that any developer jumping into this codebase can easily understand our architecture.

---

## 🔌 gRPC API Contract (Functionalities)

This service uses **gRPC** for high-performance, strongly-typed communication. We define our API contract in the `thoughts.proto` file.

Here is the complete list of functionalities this service provides, along with exactly what we need to send in the Request Body and what we get back in the Response:

### 1. Thoughts (Posts)
*   **CreateThought (`CreateRequest` ➔ `CreateResponse`)**
    *   **Request:** `user_id` (int64), `content` (string, optional), `parent_thought_id` (int64, optional)
    *   **Response:** `thought_id` (int64), `created_at` (timestamp)
    *   *Behavior:* Creates a new thought, opinion, or repost. If `parent_thought_id` is provided without `content`, it acts as a repost. If both are provided, it acts as an opinion (comment).
*   **DeleteThought (`DeleteRequest` ➔ `Empty`)**
    *   **Request:** `user_id` (int64), `thought_id` (int64)
    *   **Response:** `Empty`
    *   *Behavior:* Deletes a specific thought and cascades to remove all associated media, mentions, opinions, and likes.
*   **GetThought (`GetThoughtRequest` ➔ `GetThoughtResponse`)**
    *   **Request:** `thought_id` (int64)
    *   **Response:** The `thought_id`, `content`, interaction counts (likes, opinions, reposts), the `created_at` timestamp, and the author's `User` details.
    *   *Behavior:* Fetches all details for a single thought.
*   **GetUserHistory (`GetUserHistoryRequest` ➔ `GetHistoryResponse`)**
    *   **Request:** `user_id` (int64), `limit` (int32), `cursor` (string)
    *   **Response:** A list of `ThoughtDetails` the user created, and a `next_cursor` for pagination.
    *   *Behavior:* Fetches the chronological history of thoughts posted by a specific user.

### 2. Opinions (Comments) & Reposts
*   **GetOpinions (`GetOpinionsRequest` ➔ `GetOpinionsResponse`)**
    *   **Request:** `thought_id` (int64), `limit` (int32), `cursor` (string)
    *   **Response:** A list of `GetThoughtResponse` objects representing comments on the parent thought, and a `next_cursor`.
    *   *Behavior:* Fetches paginated opinions for a specific thought.
*   **GetReposts (`GetRepostsRequest` ➔ `GetRepostsResponse`)**
    *   **Request:** `thought_id` (int64), `limit` (int32), `cursor` (string)
    *   **Response:** A list of `Users` who reposted the thought, and a `next_cursor`.
    *   *Behavior:* Fetches the users who reposted a specific thought without adding their own content.

### 3. Likes
*   **LikeThought (`LikeRequest` ➔ `Empty`)**
    *   **Request:** `user_id` (int64), `thought_id` (int64)
    *   **Response:** `Empty`
    *   *Behavior:* Adds a like from the user to the thought.
*   **UnlikeThought (`LikeRequest` ➔ `Empty`)**
    *   **Request:** `user_id` (int64), `thought_id` (int64)
    *   **Response:** `Empty`
    *   *Behavior:* Removes a user's like from a thought.
*   **GetThoughtLikes (`GetLikesRequest` ➔ `GetLikesResponse`)**
    *   **Request:** `thought_id` (int64), `limit` (int32), `cursor` (string)
    *   **Response:** A list of `Users` who liked the thought, and a `next_cursor`.
    *   *Behavior:* Fetches the paginated list of users who liked a specific thought.

### 4. Cache Sync
*   **StoreUser (`Users` ➔ `Empty`)**
    *   **Request:** `user_id` (int64), `username` (string), `name` (string), `profile_pic_url` (string)
    *   **Response:** `Empty`
    *   *Behavior:* Keeps a local read-optimized copy of user data. When IdentityService registers or updates a user, it sends an event here to store the denormalized user data.

---

## 🧠 Key Technical Features & How They Work

### 1. Snowflake ID Generator
**Why we need it:** Just like in Identity Service, using auto-incrementing numbers for global entities like Thoughts and Likes is a major bottleneck in distributed systems.

**How it works:** We use a custom **Snowflake ID Generator** using a Hibernate Interceptor. Before a `ThoughtsEntity` or `LikesEntity` is persisted, the interceptor mathematically generates a time-sortable 64-bit ID based on the current millisecond, a datacenter ID, and a worker ID. This enables massive concurrent scaling without database locks.

### 2. Cursor-Based Pagination
**Why we need it:** In a social media feed, data changes by the second. If we used traditional page numbers (Offset pagination), users fetching "Page 2" while new thoughts are posted would see duplicate or skipped items.

**How it works:** We use `created_at` as the pointer (cursor). By requesting the next batch of thoughts strictly *older* than the `next_cursor` provided in the previous response (`t.createdAt < :cursor`), the feed remains stable regardless of how many new thoughts are inserted in real-time. This is also significantly faster than offset pagination on large datasets.

### 3. User Caching (CQRS Pattern)
**Why we need it:** To return a thought, we must include the author's username, name, and profile picture. Querying the IdentityService over the network for every single thought fetch would cripple our application's performance and cause N+1 network problems.

**How it works:** We maintain a local `users_cache` table (via `UsersEntity`). We store a denormalized, read-only copy of user profiles. This allows us to use blazing-fast SQL `JOIN`s to attach user data to thoughts instantly without leaving the Thoughts database. We keep this synchronized via the `StoreUser` gRPC method (and eventually Kafka events).

---

## 📦 Dependencies & Their Use Cases

| Dependency | Purpose in Thoughts Service |
| :--- | :--- |
| **`spring-boot-starter-actuator`** | Exposes health endpoints for container orchestration tools (like Kubernetes). |
| **`spring-boot-starter-data-jpa`** | Provides Hibernate ORM for interacting with our PostgreSQL database via Java Entities (`ThoughtsEntity`, `LikesEntity`, etc.). |
| **`postgresql`** | The official JDBC driver required to communicate with PostgreSQL. |
| **`lombok`** | Generates repetitive boilerplate code (Getters, Setters, Constructors) at compile time. |
| **`spring-boot-starter-grpc-server`** | The framework used to host our high-performance gRPC server and route exceptions. |
| **`protobuf-maven-plugin`** | Compiles our `.proto` contract files into Java classes automatically during the build process. |

---

## ⚙️ Environment Variables

Set the following environment variables (or use `application.yaml`) to run the service:

| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `SERVER_PORT` | The port for Spring Boot Actuator health checks | `8082` |
| `GRPC_SERVER_PORT` | The port for gRPC traffic | `9092` |
| `DB_URL` | PostgreSQL connection string | `jdbc:postgresql://localhost:5432/thoughts_db` |
| `DB_USERNAME` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | `secret` |

## 🧪 Testing Strategy

We maintain a robust suite of unit tests utilizing **JUnit 5** and **Mockito** to validate the core business logic (`service` layer) in complete isolation from the database and gRPC transport layer.

### 1. Test Coverage Scope
*   **`UsersServiceTests`**: Validates the creation of original thoughts, reposts, and opinions. Asserts the graceful handling of exceptions (e.g., `UserNotFoundException`, `ThoughtNotFoundException`, `DuplicateRepostException`). Confirms that liking and deleting thoughts appropriately cascades and behaves as expected. Ensures local caching of `StoreUser` logic accurately handles partial or empty fields (like missing names).
*   **`ThoughtsServiceTests`**: Evaluates thought retrievals (checking for missing/null parent IDs), and cursor pagination correctly fetches historical data without duplicates for `GetThoughtOpinions`, `GetThoughtReposts`, and `GetUserThoughtHistory`.
*   **`LikesServiceTests`**: Confirms that cursor pagination successfully retrieves users who liked a post accurately.

### 2. Edge Cases Verified
*   **Duplicate Reposts**: Tested that the system correctly identifies and rejects attempts by the same user to repost the exact same thought multiple times using `DuplicateRepostException`.
*   **Null Checks & Cursors**: Verified stability when dealing with empty strings instead of nulls in Protobuf (`entity.getName().isEmpty()`), as well as correct decoding logic for empty or missing pagination cursors.
*   **Foreign Key Safety**: Ensured `ThoughtDetails` strictly uses `Long` instead of primitive `long` for `parentThoughtId` to prevent null pointer exceptions when accessing root thoughts directly from the repository.

### 3. Validation Results
- **Total Test Cases**: 24
- **Success Rate**: 100% (24/24 passed, 0 failures, 0 errors)
- **Frameworks**: JUnit 5, Mockito

---

## 🏃‍♂️ Building and Running

1. **Compile Protobufs & Build the Project:**
   ```bash
   ./mvnw clean install
   ```
2. **Run the Server:**
   ```bash
   ./mvnw spring-boot:run
   ```
