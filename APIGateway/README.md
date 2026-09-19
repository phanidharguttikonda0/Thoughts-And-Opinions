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

## Architecture Deep Dive: WebFlux, Threads, and Blocking I/O

To achieve maximum concurrency, this API Gateway uses **Spring WebFlux** (built on Netty) instead of traditional Spring Boot (built on Tomcat). It's crucial to understand the threading model to prevent the gateway from freezing under heavy load.

### Normal Spring Boot vs WebFlux
- **Normal Spring Boot (Tomcat):** Maintains a large thread pool (e.g., 200 threads). If a thread makes a long I/O call (like uploading a file or calling a database), that specific thread goes to sleep (blocks) until the operation completes. The server stays alive because it has 199 other threads available to handle incoming requests.
- **WebFlux (Netty):** Operates on an Event Loop model with a strictly limited number of core threads (typically 1 thread per CPU core). **These core threads must NEVER go to sleep.** When a non-blocking I/O operation occurs, the thread "switches tasks," handing off the wait and immediately picking up another user's request.

### The Problem: Blocking Code in WebFlux
If you execute old, blocking Java code (like standard file writing, the MinIO SDK, or a gRPC `BlockingStub`) directly on the WebFlux Event Loop, the OS physically freezes the Java thread. Because WebFlux only has a handful of threads, a spike of just a few simultaneous requests running blocking code will freeze the entire API Gateway, locking out all users.

### The Solution: Offloading to `boundedElastic`
We cannot allow the core WebFlux threads to sleep. Therefore, when we must execute blocking code, we "offload" the task to a disposable worker thread pool called `Schedulers.boundedElastic()`. 

**The Execution Flow (e.g., Profile Upload):**
1. The WebFlux Event Loop accepts the request and streams the chunks non-blockingly to a temp file.
2. We wrap the blocking MinIO upload and gRPC call in `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`.
3. The Event Loop hands the heavy lifting to a disposable `boundedElastic` worker thread.
4. The worker thread executes the blocking code, goes to sleep waiting for the result, and wakes up. 
5. Crucially, the Event Loop remained completely awake and free to switch tasks and serve other users during this time!

### The Worker Pool Bottleneck & Java 21 Virtual Threads
By default, `boundedElastic()` uses heavy OS-level threads, capped at a maximum of `10 * Number of CPU Cores`. If a massive traffic spike occurs (e.g., 1,000 users upload files simultaneously), the OS worker threads will quickly hit their limit, and all remaining requests will be forced into a waiting queue until a sleeping worker thread wakes up.

To eliminate this bottleneck, we utilize **Java 21 Virtual Threads** by enabling `spring.threads.virtual.enabled=true`. 
With Virtual Threads, the `boundedElastic` pool abandons heavy OS threads. Instead, the JVM instantly spawns incredibly lightweight Virtual Threads that unmount from the OS when they sleep, consuming near-zero memory. This allows the API Gateway to spawn millions of sleeping worker threads instantly, ensuring the system can scale infinitely without ever causing a traffic jam.
