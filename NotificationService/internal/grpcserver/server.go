package grpcserver

import (
	"context"
	"log"

	"notificationservice/internal/store"
	pb "notificationservice/proto"
)

// NotificationServer implements the gRPC NotificationService.
type NotificationServer struct {
	pb.UnimplementedNotificationServiceServer
	store *store.RedisStore
}

// NewNotificationServer creates a new gRPC server.
func NewNotificationServer(redisStore *store.RedisStore) *NotificationServer {
	return &NotificationServer{store: redisStore}
}

// GetNotifications returns a paginated list of notifications for a user in descending order.
func (s *NotificationServer) GetNotifications(ctx context.Context, req *pb.GetNotificationsRequest) (*pb.GetNotificationsResponse, error) {
	userID := req.GetUserId()
	limit := req.GetLimit()
	if limit <= 0 {
		limit = 20
	}

	cursor := ""
	if req.Cursor != nil {
		cursor = *req.Cursor
	}

	log.Printf("GetNotifications: userID=%d, limit=%d, cursor=%s", userID, limit, cursor)

	notifications, nextCursor, err := s.store.GetNotifications(ctx, userID, limit, cursor)
	if err != nil {
		log.Printf("Error fetching notifications: %v", err)
		return nil, err
	}

	var items []*pb.NotificationItem
	for _, n := range notifications {
		items = append(items, &pb.NotificationItem{
			NotificationId: n.NotificationID,
			Type:           n.Type,
			ActorId:        n.ActorID,
			ActorUsername:   n.ActorUsername,
			ThoughtId:      n.ThoughtID,
			Timestamp:      n.Timestamp,
		})
	}

	resp := &pb.GetNotificationsResponse{
		Notifications: items,
		NextCursor:    nextCursor,
	}

	log.Printf("Returning %d notifications for user %d", len(items), userID)
	return resp, nil
}
