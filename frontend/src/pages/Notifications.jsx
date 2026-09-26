import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Heart, Repeat, MessageCircle, UserPlus, Loader, Bell } from 'lucide-react';
import api from '../services/api';

const notifConfig = {
  LIKE: {
    icon: Heart,
    color: 'text-(--color-like)',
    bgColor: 'bg-pink-500/10',
    text: 'liked your thought',
  },
  REPOST: {
    icon: Repeat,
    color: 'text-(--color-repost)',
    bgColor: 'bg-green-500/10',
    text: 'reposted your thought',
  },
  OPINION: {
    icon: MessageCircle,
    color: 'text-(--color-primary)',
    bgColor: 'bg-blue-500/10',
    text: 'shared an opinion on your thought',
  },
  FOLLOW: {
    icon: UserPlus,
    color: 'text-purple-500',
    bgColor: 'bg-purple-500/10',
    text: 'followed you',
  },
};

const timeAgo = (timestamp) => {
  const seconds = Math.floor((Date.now() - timestamp) / 1000);
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}d`;
  return new Date(timestamp).toLocaleDateString();
};

const NotificationItem = ({ notification, onClick }) => {
  const config = notifConfig[notification.type] || notifConfig.LIKE;
  const Icon = config.icon;

  return (
    <div
      onClick={onClick}
      className="flex items-start gap-4 p-4 border-b border-(--border-color) hover:bg-(--bg-secondary) cursor-pointer transition-colors duration-200"
    >
      <div className={`w-10 h-10 rounded-full ${config.bgColor} flex items-center justify-center shrink-0`}>
        <Icon size={20} className={config.color} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-(--text-primary)">
          <span className="font-bold">@{notification.actorUsername || 'user'}</span>
          {' '}
          <span className="text-(--text-secondary)">{config.text}</span>
        </p>
        <span className="text-(--text-secondary) text-sm">{timeAgo(notification.timestamp)}</span>
      </div>
    </div>
  );
};

const Notifications = () => {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [nextCursor, setNextCursor] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetchNotifications();
  }, []);

  const fetchNotifications = async (cursor = null) => {
    try {
      if (cursor) {
        setLoadingMore(true);
      } else {
        setLoading(true);
      }

      const params = { limit: 20 };
      if (cursor) params.cursor = cursor;

      const res = await api.get('/notifications', { params });
      if (res.data.success && res.data.data) {
        const newNotifs = res.data.data.notifications || [];
        if (cursor) {
          setNotifications(prev => [...prev, ...newNotifs]);
        } else {
          setNotifications(newNotifs);
        }
        setNextCursor(res.data.data.nextCursor || null);
      }
    } catch (error) {
      console.error('Failed to fetch notifications', error);
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  };

  const handleNotificationClick = (notification) => {
    if (notification.type === 'FOLLOW') {
      navigate(`/profile/${notification.actorId}`);
    } else if (notification.thoughtId && notification.thoughtId !== 0) {
      navigate(`/thought/${notification.thoughtId}`);
    }
  };

  return (
    <div className="w-full max-w-2xl">
      <header className="sticky top-0 z-10 bg-(--bg-primary)/80 backdrop-blur-md border-b border-(--border-color) px-4 py-3">
        <h1 className="text-xl font-bold">Notifications</h1>
      </header>

      <div>
        {loading ? (
          <div className="p-8 flex justify-center">
            <Loader className="animate-spin text-(--color-primary)" size={32} />
          </div>
        ) : notifications.length > 0 ? (
          <>
            {notifications.map((notif, index) => (
              <NotificationItem
                key={notif.notificationId || index}
                notification={notif}
                onClick={() => handleNotificationClick(notif)}
              />
            ))}
            {nextCursor && (
              <div className="p-4 flex justify-center">
                <button
                  onClick={() => fetchNotifications(nextCursor)}
                  disabled={loadingMore}
                  className="bg-(--color-primary) hover:bg-(--color-primary-hover) text-white font-bold py-2 px-6 rounded-full transition-colors disabled:opacity-50"
                >
                  {loadingMore ? <Loader className="animate-spin" size={18} /> : 'Load More'}
                </button>
              </div>
            )}
          </>
        ) : (
          <div className="p-12 text-center">
            <div className="w-16 h-16 rounded-full bg-(--bg-secondary) flex items-center justify-center mx-auto mb-4">
              <Bell size={32} className="text-(--text-secondary)" />
            </div>
            <h2 className="text-xl font-bold mb-2">Nothing here yet</h2>
            <p className="text-(--text-secondary)">
              When someone likes, reposts, or shares an opinion on your thoughts, or follows you, it'll show up here.
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default Notifications;
