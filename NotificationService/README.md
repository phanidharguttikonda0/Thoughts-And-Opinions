# Notification Service

The Notification Service is a Go-based microservice that processes and serves user notifications.

## Architecture

1. **Kafka Consumer**: Listens to the `notification.events` topic for `LIKE`, `REPOST`, `OPINION`, and `FOLLOW` events published by the API Gateway.
2. **Redis Store**: Uses a pointer pattern for efficient storage and retrieval.
    *   **Index (`ZSET`)**: `notifs:index:{user_id}` stores `notification_id` sorted by timestamp.
    *   **Payload (`String`)**: `notif:payload:{notification_id}` stores the JSON payload with a 30-day TTL.
    *   Stale members in the `ZSET` are lazily cleaned up when their payload expires.
3. **gRPC Server**: Exposes a `GetNotifications` RPC endpoint (port `9095`) that the API Gateway queries to serve the frontend feed.

## Running Locally

Requirements:
- Go 1.20+
- Redis (localhost:6379)
- Kafka (localhost:9092)

```bash
# Install dependencies
go mod tidy

# Run the service
go run cmd/server/main.go
```

## Configuration

| Environment Variable | Default | Description |
| :--- | :--- | :--- |
| `REDIS_ADDR` | `localhost:6379` | Redis connection address |
| `KAFKA_BROKER` | `localhost:9092` | Kafka broker address |
| `KAFKA_TOPIC` | `notification.events` | Kafka topic to consume |
| `KAFKA_GROUP_ID` | `notification-service-group` | Kafka consumer group ID |
| `GRPC_PORT` | `9095` | Port for the gRPC server |
