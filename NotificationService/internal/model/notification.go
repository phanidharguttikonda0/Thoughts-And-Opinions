package model

// NotificationEvent represents the Kafka event from the API Gateway.
type NotificationEvent struct {
	Type          string `json:"type"`          // LIKE, REPOST, OPINION, FOLLOW
	ActorID       int64  `json:"actorId"`       // User who performed the action
	ActorUsername string `json:"actorUsername"`  // Username of the actor
	TargetUserID  int64  `json:"targetUserId"`  // User who receives the notification
	ThoughtID     int64  `json:"thoughtId"`     // Relevant thought ID (0 for FOLLOW)
	Timestamp     int64  `json:"timestamp"`     // Unix millis
}

// NotificationPayload is stored in Redis as JSON string.
type NotificationPayload struct {
	NotificationID string `json:"notificationId"`
	Type           string `json:"type"`
	ActorID        int64  `json:"actorId"`
	ActorUsername   string `json:"actorUsername"`
	ThoughtID      int64  `json:"thoughtId"`
	Timestamp      int64  `json:"timestamp"`
}
