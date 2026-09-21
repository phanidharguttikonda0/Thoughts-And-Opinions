# Timeline Service

## Overview
The Timeline Service is a core component of the Thoughts and Opinions application. It is responsible for generating, managing, and delivering live user feeds in chronological order. To guarantee millisecond read latencies when users request their feed, it utilizes a **Fan-Out on Write** architecture backed by Redis.

---

## Finalized Architecture

### 1. Write Path: Fan-Out on Write (Kafka)
When a user publishes a new thought, we need to immediately distribute it to all of their followers. 
1. The **Thought Service** creates the thought in Postgres and immediately publishes a `ThoughtCreatedEvent` to Kafka.
2. The **Timeline Service** acts as a Kafka Consumer and listens to this event.
3. Upon receiving the event, it makes a synchronous **gRPC call to the Identity Service** (`getFollowersList`) to retrieve all the `follower_ids` of the author.
4. It iterates through the follower list and pushes the new `thought_id` into each follower's dedicated timeline in Redis.

### 2. Storage & LRU Strategy (Redis Sorted Sets)
The feed data is stored entirely in memory using Redis.
*   **Data Structure:** We use a Redis Sorted Set (`ZSET`) for each user (Key: `feed:{user_id}`).
*   **Chronological Order:** The "score" for each entry in the ZSET is the `createdAt` timestamp of the thought. This automatically sorts the feed strictly by time.
*   **LRU / Feed Capping:** To optimize RAM usage, we maintain a hard limit of **120 latest posts** per user. 
    *   This is achieved using Redis's built-in trimming. Every time a new thought is added via `ZADD`, we immediately run `ZREMRANGEBYRANK feed:{user_id} 0 -121`. 
    *   This instantly drops the oldest items, keeping only the 120 newest posts. Read items are NOT actively deleted, preventing the "empty feed" problem if a user refreshes the app.

### 3. Read Path & Hydration (gRPC Server)
The Timeline Service does not expose HTTP REST APIs; instead, it operates as a **gRPC Server** (`TimelineGatewayService`).
1. The **API Gateway** receives a client's HTTP request for their feed.
2. The API Gateway acts as a gRPC client and makes a call to the Timeline Service, passing the `user_id`, `limit` (10 to 20), and an optional `cursor` (the timestamp of the last seen post).
3. The Timeline Service queries the Redis `ZSET` using `ZREVRANGEBYSCORE` to fetch the paginated list of `thought_id`s.
4. **Hydration:** Because Redis only stores the IDs, the Timeline Service makes a synchronous **gRPC call to the Thoughts Service** (`getThought`) to fetch the full content, author details, likes count, etc., for those specific IDs.
5. Finally, it returns the fully hydrated list of `GetThoughtResponse` objects back to the API Gateway.

---

## Service Dependencies
*   **Identity Service (gRPC Client):** Used to fetch the list of followers during the Fan-Out phase.
*   **Thoughts Service (gRPC Client):** Used to fetch full thought details during the Hydration phase.
*   **Kafka:** Consumes `thought-created` topics.
*   **Redis:** Primary datastore for the chronological feeds.
