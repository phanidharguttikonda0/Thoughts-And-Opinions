package store

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"time"

	"notificationservice/internal/model"

	"github.com/redis/go-redis/v9"
)

const (
	// PayloadTTL is 30 days in seconds.
	PayloadTTL = 30 * 24 * 60 * 60 * time.Second
)

// RedisStore manages notification storage in Redis.
type RedisStore struct {
	client *redis.Client
}

// NewRedisStore creates a new Redis store.
func NewRedisStore(addr string) *RedisStore {
	client := redis.NewClient(&redis.Options{
		Addr: addr,
	})

	// Test connection
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := client.Ping(ctx).Err(); err != nil {
		log.Fatalf("Failed to connect to Redis at %s: %v", addr, err)
	}
	log.Printf("Connected to Redis at %s", addr)

	return &RedisStore{client: client}
}

// indexKey returns the ZSET key for a user's notification index.
func indexKey(userID int64) string {
	return fmt.Sprintf("notifs:index:%d", userID)
}

// payloadKey returns the String key for a notification payload.
func payloadKey(notificationID string) string {
	return fmt.Sprintf("notif:payload:%s", notificationID)
}

// StoreNotification writes a notification to Redis.
// 1. Stores the payload as a JSON string with TTL.
// 2. Adds the notification_id to the user's ZSET index with the timestamp as score.
func (s *RedisStore) StoreNotification(ctx context.Context, userID int64, notif model.NotificationPayload) error {
	// Marshal the payload to JSON
	payloadJSON, err := json.Marshal(notif)
	if err != nil {
		return fmt.Errorf("failed to marshal notification payload: %w", err)
	}

	// Store payload with TTL
	pKey := payloadKey(notif.NotificationID)
	if err := s.client.Set(ctx, pKey, string(payloadJSON), PayloadTTL).Err(); err != nil {
		return fmt.Errorf("failed to store notification payload: %w", err)
	}

	// Add to the user's ZSET index
	iKey := indexKey(userID)
	score := float64(notif.Timestamp)
	if err := s.client.ZAdd(ctx, iKey, redis.Z{
		Score:  score,
		Member: notif.NotificationID,
	}).Err(); err != nil {
		return fmt.Errorf("failed to add to notification index: %w", err)
	}

	log.Printf("Stored notification %s for user %d", notif.NotificationID, userID)
	return nil
}

// GetNotifications fetches notifications for a user in descending order.
// cursor is a timestamp string; items older than cursor are fetched.
// limit controls how many items to return.
func (s *RedisStore) GetNotifications(ctx context.Context, userID int64, limit int32, cursor string) ([]model.NotificationPayload, string, error) {
	iKey := indexKey(userID)

	var maxScore string
	if cursor != "" {
		maxScore = cursor
	} else {
		maxScore = "+inf"
	}

	// Fetch notification IDs from ZSET in descending order
	results, err := s.client.ZRevRangeByScoreWithScores(ctx, iKey, &redis.ZRangeBy{
		Min:    "-inf",
		Max:    maxScore,
		Offset: 0,
		Count:  int64(limit),
	}).Result()
	if err != nil {
		return nil, "", fmt.Errorf("failed to fetch notification index: %w", err)
	}

	var notifications []model.NotificationPayload
	var nextCursor string
	var staleMembers []string

	for _, z := range results {
		notifID, ok := z.Member.(string)
		if !ok {
			continue
		}

		// Fetch the payload
		pKey := payloadKey(notifID)
		payloadJSON, err := s.client.Get(ctx, pKey).Result()
		if err == redis.Nil {
			// Payload expired, clean up stale ZSET member
			staleMembers = append(staleMembers, notifID)
			continue
		}
		if err != nil {
			log.Printf("Failed to fetch payload for %s: %v", notifID, err)
			continue
		}

		var payload model.NotificationPayload
		if err := json.Unmarshal([]byte(payloadJSON), &payload); err != nil {
			log.Printf("Failed to unmarshal payload for %s: %v", notifID, err)
			continue
		}

		notifications = append(notifications, payload)
		// The score of the last item becomes the next cursor
		nextCursor = fmt.Sprintf("%.0f", z.Score-1)
	}

	// Clean up stale ZSET members asynchronously
	if len(staleMembers) > 0 {
		go func() {
			cleanCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
			defer cancel()
			for _, member := range staleMembers {
				s.client.ZRem(cleanCtx, iKey, member)
			}
			log.Printf("Cleaned up %d stale notification members for user %d", len(staleMembers), userID)
		}()
	}

	// If we got fewer results than requested, there are no more items
	if int32(len(results)) < limit {
		nextCursor = ""
	}

	return notifications, nextCursor, nil
}
