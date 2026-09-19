# A to Z Guide: File Uploads in Spring WebFlux (API Gateway)

Handling file uploads in a reactive system like Spring WebFlux is fundamentally different from standard Spring Boot. Because you mentioned you haven't done backend file handling before, we will break this down from the ground up.

## 1. How `multipart/form-data` Works
Normally, when you send JSON, the entire body of the HTTP request is just text. But what if you want to send a user's name (text) AND a profile picture (binary file) at the same time? 

You use `multipart/form-data`. The HTTP request is literally split into "parts" or "chunks", separated by a boundary string.
- **Part 1:** Could be a JSON string containing `{"name": "Phani", "bio": "Hello"}`.
- **Part 2:** Contains the binary 1s and 0s of the image file.

## 2. Standard Spring Boot (MVC) vs. WebFlux
### Standard Spring Boot (Spring Web / Tomcat)
In normal Spring Boot, a dedicated thread is assigned to every single user request. When a file is being uploaded, that thread **halts and waits (blocks)** until the entire file is received and saved to a temporary folder on the server's hard drive. Then, it gives you a `MultipartFile` object. It's simple, but a slow internet connection uploading a 10MB file will freeze that server thread the entire time.

### Spring WebFlux (Reactive / Netty)
WebFlux works exactly like Node.js. It only has a few threads (event loop) handling thousands of requests. **You cannot block these threads.** 
When a file uploads, WebFlux doesn't wait for the whole file. Instead, it fires an event every time a small chunk of data arrives. It gives you a `FilePart` object, which is essentially a reactive stream (a pipeline) of data chunks.

## 3. The Architecture for Profile Updates

Here is how we will architect the Profile Update endpoint in the API Gateway.

> [!NOTE]
> Since we use WebFlux, we must ensure the MinIO upload does not block our event loop. We will use Java's `Schedulers.boundedElastic()` which safely offloads blocking tasks to a separate, dedicated worker thread pool.

### The Endpoint Signature
Our controller will look something like this:
```java
@PatchMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public Mono<ResponseEntity<ResponseDTO<Void>>> updateProfile(
    @RequestPart("data") String updateDataJson, 
    @RequestPart(value = "file", required = false) FilePart profilePicture // required = false handles the case where user doesn't upload an image
)
```

### The Step-by-Step Flow

#### Case A: User Updates Bio/Name, but NO Profile Picture
1. The request arrives. `profilePicture` is `null`.
2. We parse the `updateDataJson` into a Java object.
3. We immediately construct the gRPC `UpdateProfileRequest`.
4. We call the Identity Service via gRPC.
5. We return success to the user.

#### Case B: User Updates Bio AND Uploads a Profile Picture
1. The request arrives. `profilePicture` is present.
2. **Transfer to Temp File:** Because the official MinIO Java SDK expects a standard `InputStream` (which is blocking), we reactively stream the `FilePart` chunks to a temporary file on the Gateway's disk. *This is non-blocking.*
3. **Upload to MinIO:** Once the temporary file is completely saved on disk, we offload the MinIO upload process to a background worker thread (`Schedulers.boundedElastic()`). 
   - We upload the file to your MinIO bucket (e.g., `profiles`).
   - We generate the public URL for the image (e.g., `http://minio:9000/profiles/user-123-pic.jpg`).
4. **Clean up:** We delete the temporary file from the Gateway's disk to save space.
5. **Call gRPC:** We construct the gRPC `UpdateProfileRequest`, combining the parsed JSON data and the new MinIO Image URL.
6. **Return Success:** We send the `ResponseDTO` back to the frontend.

## 4. Why This Approach is the Best
- **`required = false`** handles the optional nature of the file beautifully without throwing errors.
- **Non-blocking chunks:** By streaming chunks to a temp file first, your API Gateway can handle thousands of concurrent file uploads without crashing or running out of memory. If we stored all chunks in RAM, 100 users uploading 5MB files simultaneously would instantly consume 500MB of server RAM.
- **Separation of Concerns:** The API Gateway handles the messy HTTP Multipart parsing, the MinIO file storage, and the URL generation. The internal Identity Service (gRPC) only ever sees clean data: strings, names, and image URLs. This keeps your internal microservices lightweight and completely unaware of HTTP or File Systems.

## 5. Next Steps
To implement this, we will need:
1. To add the `minio` Java dependency to the API Gateway.
2. To configure MinIO credentials (URL, Access Key, Secret Key) in `application.yaml`.
3. To write a `MinioService` in the Gateway that handles uploading the file and returning the URL.
