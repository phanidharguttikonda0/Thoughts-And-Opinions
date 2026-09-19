package com.thoughtsandopinions.apigateway.controller;

import com.thoughtsandopinions.apigateway.dto.api.ResponseDTO;
import com.thoughtsandopinions.apigateway.dto.api.UpdateProfileDTO;
import com.thoughtsandopinions.apigateway.dto.service.UpdateProfileServiceDTO;
import com.thoughtsandopinions.apigateway.gRPC.IdentityServiceGrpcHandler;
import com.thoughtsandopinions.apigateway.service.MinioService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final IdentityServiceGrpcHandler identityServiceGrpcHandler;
    private final MinioService minioService;

    public ProfileController(IdentityServiceGrpcHandler identityServiceGrpcHandler, MinioService minioService) {
        this.identityServiceGrpcHandler = identityServiceGrpcHandler;
        this.minioService = minioService;
    }

    /*
     * WHY WE CHOSE WEBFLUX:
     * We chose WebFlux because our I/O operations can be long. In a normal Spring Boot application 
     * (which has around 200 threads), if 200 simultaneous requests hit an endpoint and wait for I/O, 
     * all 200 threads get blocked and go to sleep. When that happens, the server cannot accept new 
     * requests and effectively goes down. 
     * 
     * WebFlux uses an Event Loop architecture (similar to Node.js). When a non-blocking I/O operation 
     * hits, the Event Loop switches tasks instead of putting the thread to sleep. This allows WebFlux 
     * to handle massive concurrency using very few threads (typically 1 thread per CPU core).
     * 
     * THE PROBLEM WITH OLD I/O IN WEBFLUX:
     * WebFlux only works if threads NEVER go to sleep. However, if we use old, blocking Java code:
     * -> The Event Loop *wants* to switch tasks, but calling standard blocking I/O physically freezes 
     *    the Java thread at the OS level. The Event Loop can't switch tasks if it's completely frozen!
     * -> We are using a BlockingStub for gRPC. Calling it blocks the current thread, preventing 
     *    task switching. 
     * 
     * THE SOLUTION:
     * -> We could use gRPC FutureStubs (which do NOT block the thread and allow task switching), 
     *    but configuring and chaining them can be complex.
     * -> Instead, we use a "disposable thread pool" (`Schedulers.boundedElastic()`) which is 
     *    specifically designed to handle old, blocking Java code. 
     * -> We "offload" the blocking task to these disposable worker threads. The worker thread is 
     *    the one that gets blocked and goes to sleep, leaving our precious Event Loop threads 
     *    awake, free to switch tasks, and ready to serve other users!
     * 
     * Our main motto is: Keep the Event Loop awake and switching tasks, NEVER sleeping!
     */

    @PatchMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ResponseDTO<Void>>> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestPart(value = "data", required = false) UpdateProfileDTO profileData,
            @RequestPart(value = "file", required = false) FilePart filePart) {

        /*
         * PROFILE UPDATE EXECUTION FLOW:
         * First, the WebFlux Event Loop receives the incoming multipart file chunks and passes
         * them non-blockingly to the uploadProfilePicture function, which streams the chunks
         * to a temporary file on the local disk. Once the file is complete, the Event Loop
         * offloads the heavy lifting to a `Schedulers.boundedElastic()` worker thread (a separate,
         * disposable OS-level platform thread from a dedicated pool). This worker thread uses
         * the old, blocking MinIO SDK to upload the file. While this worker thread goes to sleep
         * waiting for MinIO, the main WebFlux Event Loop remains completely unblocked and free
         * to serve other users. Once the upload succeeds, the worker thread wakes up and returns
         * the public URL. We then use this URL to build our Service DTO. Finally, the Event Loop
         * offloads the blocking gRPC call to another boundedElastic worker thread, ensuring the
         * Identity Service is updated without ever freezing our core WebFlux threads.
         */

        Mono<String> imageUrlMono = filePart != null 
                ? minioService.uploadProfilePicture(filePart) 
                : Mono.just("");

        /*
        * Here we are
        * */
        return imageUrlMono.flatMap(imageUrl -> {
            String profileUrl = imageUrl.isEmpty() ? null : imageUrl;

            UpdateProfileServiceDTO serviceDTO = new UpdateProfileServiceDTO(
                    profileData != null ? profileData.name() : null,
                    profileData != null ? profileData.username() : null,
                    userId,
                    profileData != null ? profileData.bio() : null,
                    profileUrl
            );

            // Offload blocking gRPC call to boundedElastic thread pool
            return Mono.fromCallable(() -> identityServiceGrpcHandler.updateProfile(serviceDTO))
                    .subscribeOn(Schedulers.boundedElastic())
                    .map(grpcResponse -> {
                        ResponseDTO<Void> response = ResponseDTO.<Void>builder()
                                .success(true)
                                .message("Profile updated successfully")
                                .build();
                        return ResponseEntity.ok(response);
                    });
        });
    }
}

/*
 * PROFILE UPDATE EXECUTION FLOW:
 * First, the WebFlux Event Loop receives the incoming multipart file chunks and passes 
 * them non-blockingly to the uploadProfilePicture function, which streams the chunks 
 * to a temporary file on the local disk. Once the file is complete, the Event Loop 
 * offloads the heavy lifting to a `Schedulers.boundedElastic()` worker thread to upload 
 * the temp file to the MinIO server via the MinIO SDK. 
 * 
 * While this worker thread goes to sleep waiting for MinIO, the main WebFlux Event Loop 
 * remains completely unblocked and free to shift tasks and serve other users. Once the 
 * upload succeeds, the worker thread wakes up and returns the public URL. We then use 
 * this URL to build our Service DTO. Finally, the Event Loop offloads the blocking gRPC 
 * call to a boundedElastic worker thread to send back the response without blocking the 
 * core threads.
 * 
 * THE WORKER THREAD BOTTLENECK & VIRTUAL THREADS (JAVA 21):
 * By default, `boundedElastic()` uses OS-level threads. It caps these worker threads 
 * at 10 * [Number of CPU Cores]. Because these worker threads go to sleep when executing 
 * blocking SDK calls, a massive spike in traffic (e.g., 1000 users) will quickly max out 
 * the worker pool. The core WebFlux threads remain unfrozen, but the excess user requests 
 * are forced to wait in a queue until a sleeping worker thread wakes up and becomes available.
 * 
 * To solve this, we utilize Java 21 and enable Virtual Threads (`spring.threads.virtual.enabled=true`).
 * With Virtual Threads enabled, the boundedElastic pool ditches heavy OS threads and instead 
 * spawns incredibly lightweight Virtual Threads. Because Virtual Threads unmount from the OS 
 * when they sleep, they consume almost zero memory. We can spawn millions of them instantly, 
 * eliminating the worker pool bottleneck entirely and allowing WebFlux to scale infinitely!
 */
