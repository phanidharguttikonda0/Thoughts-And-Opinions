package consumer

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"os/signal"
	"sync"
	"syscall"

	"notificationservice/internal/model"
	"notificationservice/internal/store"

	"github.com/IBM/sarama"
)

// KafkaConsumer consumes notification events from Kafka and stores them in Redis.
type KafkaConsumer struct {
	store   *store.RedisStore
	brokers []string
	topic   string
	groupID string
	ready   chan bool
}

// NewKafkaConsumer creates a new consumer.
func NewKafkaConsumer(redisStore *store.RedisStore, brokers []string, topic, groupID string) *KafkaConsumer {
	return &KafkaConsumer{
		store:   redisStore,
		brokers: brokers,
		topic:   topic,
		groupID: groupID,
		ready:   make(chan bool),
	}
}

// Start begins consuming from Kafka in a blocking goroutine.
func (c *KafkaConsumer) Start(ctx context.Context, wg *sync.WaitGroup) {
	defer wg.Done()

	config := sarama.NewConfig()
	config.Consumer.Group.Rebalance.GroupStrategies = []sarama.BalanceStrategy{sarama.NewBalanceStrategyRange()}
	config.Consumer.Offsets.Initial = sarama.OffsetNewest
	config.Version = sarama.V3_6_0_0

	client, err := sarama.NewConsumerGroup(c.brokers, c.groupID, config)
	if err != nil {
		log.Fatalf("Error creating Kafka consumer group: %v", err)
	}
	defer func() {
		if err := client.Close(); err != nil {
			log.Printf("Error closing Kafka consumer group: %v", err)
		}
	}()

	handler := &consumerGroupHandler{
		store: c.store,
		ready: c.ready,
	}

	// Trap signals for graceful shutdown
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		for {
			if err := client.Consume(ctx, []string{c.topic}, handler); err != nil {
				log.Printf("Error from Kafka consumer: %v", err)
			}
			if ctx.Err() != nil {
				return
			}
			handler.ready = make(chan bool)
		}
	}()

	<-handler.ready
	log.Printf("Kafka consumer started, listening on topic: %s", c.topic)

	select {
	case <-ctx.Done():
		log.Println("Kafka consumer context cancelled, shutting down...")
	case sig := <-sigChan:
		log.Printf("Caught signal %v, shutting down Kafka consumer...", sig)
	}
}

// consumerGroupHandler implements sarama.ConsumerGroupHandler.
type consumerGroupHandler struct {
	store *store.RedisStore
	ready chan bool
}

func (h *consumerGroupHandler) Setup(sarama.ConsumerGroupSession) error {
	close(h.ready)
	return nil
}

func (h *consumerGroupHandler) Cleanup(sarama.ConsumerGroupSession) error {
	return nil
}

func (h *consumerGroupHandler) ConsumeClaim(session sarama.ConsumerGroupSession, claim sarama.ConsumerGroupClaim) error {
	for msg := range claim.Messages() {
		log.Printf("Received Kafka message: topic=%s partition=%d offset=%d", msg.Topic, msg.Partition, msg.Offset)

		var event model.NotificationEvent
		if err := json.Unmarshal(msg.Value, &event); err != nil {
			log.Printf("Failed to unmarshal Kafka message: %v", err)
			session.MarkMessage(msg, "")
			continue
		}

		// Generate deterministic notification ID
		notificationID := fmt.Sprintf("%s:%d:%d:%d", event.Type, event.ActorID, event.ThoughtID, event.Timestamp)

		payload := model.NotificationPayload{
			NotificationID: notificationID,
			Type:           event.Type,
			ActorID:        event.ActorID,
			ActorUsername:   event.ActorUsername,
			ThoughtID:      event.ThoughtID,
			Timestamp:      event.Timestamp,
		}

		ctx := context.Background()
		if err := h.store.StoreNotification(ctx, event.TargetUserID, payload); err != nil {
			log.Printf("Failed to store notification: %v", err)
		}

		session.MarkMessage(msg, "")
	}
	return nil
}
