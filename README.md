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
| **Timeline Service** | gRPC         | Aggregates Thoughts from followed users to generate the home feed. Fan-out architecture for fast reads. | Redis / Cassandra |
| **Notification Service**| gRPC / SSE   | Asynchronous service that pushes real-time alerts (new followers, likes, mentions) to users. | Kafka / RabbitMQ |

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