package main

import (
	"context"
	"log"
	"net"
	"os"
	"sync"

	"notificationservice/internal/consumer"
	"notificationservice/internal/grpcserver"
	"notificationservice/internal/store"
	pb "notificationservice/proto"

	"google.golang.org/grpc"
)

func getEnv(key, defaultVal string) string {
	if val := os.Getenv(key); val != "" {
		return val
	}
	return defaultVal
}

func main() {
	log.SetFlags(log.LstdFlags | log.Lshortfile)
	log.Println("Starting Notification Service...")

	// Configuration
	redisAddr := getEnv("REDIS_ADDR", "localhost:6379")
	kafkaBroker := getEnv("KAFKA_BROKER", "localhost:9092")
	grpcPort := getEnv("GRPC_PORT", "9095")
	kafkaTopic := getEnv("KAFKA_TOPIC", "notification.events")
	kafkaGroupID := getEnv("KAFKA_GROUP_ID", "notification-service-group")

	// Initialize Redis store
	redisStore := store.NewRedisStore(redisAddr)

	// Create context with cancellation
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	var wg sync.WaitGroup

	// Start Kafka consumer in a goroutine
	kafkaConsumer := consumer.NewKafkaConsumer(redisStore, []string{kafkaBroker}, kafkaTopic, kafkaGroupID)
	wg.Add(1)
	go kafkaConsumer.Start(ctx, &wg)

	// Start gRPC server
	lis, err := net.Listen("tcp", ":"+grpcPort)
	if err != nil {
		log.Fatalf("Failed to listen on port %s: %v", grpcPort, err)
	}

	grpcServer := grpc.NewServer()
	notifServer := grpcserver.NewNotificationServer(redisStore)
	pb.RegisterNotificationServiceServer(grpcServer, notifServer)

	log.Printf("gRPC server listening on :%s", grpcPort)

	// Run gRPC server (this blocks)
	if err := grpcServer.Serve(lis); err != nil {
		log.Fatalf("Failed to serve gRPC: %v", err)
	}

	// Wait for Kafka consumer to finish
	cancel()
	wg.Wait()
}
