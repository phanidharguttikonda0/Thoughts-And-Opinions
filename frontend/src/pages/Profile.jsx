import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import ThoughtCard from '../components/ThoughtCard';
import EditProfileModal from '../components/EditProfileModal';
import { Loader, ArrowLeft, Calendar, MapPin, Link as LinkIcon, X } from 'lucide-react';
import { format } from 'date-fns';

const Profile = () => {
  const { id } = useParams();
  const { user: currentUser, setUser } = useAuth();
  const navigate = useNavigate();

  const [profileUser, setProfileUser] = useState(null);
  const [thoughts, setThoughts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  
  // Follower/Following Modal state
  const [showUsersModal, setShowUsersModal] = useState(false);
  const [modalUsers, setModalUsers] = useState([]);
  const [modalType, setModalType] = useState(''); // 'followers' or 'following'
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [usersCursor, setUsersCursor] = useState(null);

  const targetUserId = id || (currentUser?.user_id || currentUser?.userId);

  useEffect(() => {
    if (targetUserId) {
      fetchProfileData(targetUserId);
    }
  }, [id, currentUser, targetUserId]);

  const fetchProfileData = async (uid) => {
    setLoading(true);
    setError(null);
    try {
      // 1. Fetch Profile Details
      const profileRes = await api.get(`/profile/${targetUserId}`);
      const userData = profileRes.data.data;
      setProfileUser(userData);

      // 2. Fetch Profile Feed
      const feedRes = await api.get(`/profile/${targetUserId}/feed`);
      
      // Backend doesn't attach user details to profile feed thoughts, so we manually attach them here
      const mappedThoughts = (feedRes.data.data.thoughts || []).map(thought => ({
        ...thought,
        user: {
          userId: userData.userId,
          name: userData.name,
          username: userData.username,
        }
      }));
      setThoughts(mappedThoughts);

    } catch (err) {
      console.error("Failed to fetch profile", err);
      setError("Failed to load profile.");
    } finally {
      setLoading(false);
    }
  };

  const handleProfileUpdated = (updatedUser) => {
    // 1. Update the local profileUser state immediately
    setProfileUser(updatedUser);
    
    // 2. If editing our own profile, update AuthContext user
    if (String(currentUser?.user_id) === String(updatedUser.userId) && setUser) {
      setUser(prev => ({
        ...prev,
        name: updatedUser.name,
        sub: updatedUser.username,
        profilePicUrl: updatedUser.profilePicUrl
      }));
    }
  };

  const fetchFollowUsers = async (type, cursorStr = null) => {
    try {
      setLoadingUsers(true);
      if (!cursorStr) {
        setShowUsersModal(true);
        setModalType(type);
        setModalUsers([]);
      }
      
      const endpoint = type === 'followers' ? 'followerslist' : 'followinglist';
      const url = cursorStr 
        ? `/user/${targetUserId}/${endpoint}?limit=20&cursor=${cursorStr}`
        : `/user/${targetUserId}/${endpoint}?limit=20`;
        
      const res = await api.get(url);
      if (res.data.success) {
        if (cursorStr) {
          setModalUsers(prev => [...prev, ...(res.data.data.users || [])]);
        } else {
          setModalUsers(res.data.data.users || []);
        }
        setUsersCursor(res.data.data.nextCursor);
      }
    } catch (err) {
      console.error(`Failed to fetch ${type}`, err);
    } finally {
      setLoadingUsers(false);
    }
  };

  if (loading) {
    return (
      <div className="p-8 flex justify-center w-full max-w-2xl">
        <Loader className="animate-spin text-(--color-primary)" size={32} />
      </div>
    );
  }

  if (error || !profileUser) {
    return (
      <div className="p-8 text-center text-red-500 w-full max-w-2xl">
        {error || "User not found"}
      </div>
    );
  }

  const isOwnProfile = String(currentUser?.user_id) === String(profileUser.userId);

  return (
    <div className="w-full max-w-2xl">
      {/* Header */}
      <header className="sticky top-0 z-10 bg-(--bg-primary)/80 backdrop-blur-md border-b border-(--border-color) px-4 py-2 flex items-center gap-6">
        <button 
          onClick={() => navigate(-1)}
          className="p-2 rounded-full hover:bg-(--bg-secondary) transition-colors"
        >
          <ArrowLeft size={20} />
        </button>
        <div>
          <h1 className="text-xl font-bold">{profileUser.name}</h1>
          <p className="text-sm text-(--text-secondary)">{thoughts.length} thoughts</p>
        </div>
      </header>

      {/* Banner & Avatar */}
      <div className="relative">
        <div className="h-48 bg-linear-to-r from-pink-500/20 to-blue-500/20"></div>
        <div className="absolute -bottom-16 left-4 border-4 border-(--bg-primary) rounded-full w-32 h-32 bg-(--color-primary) flex items-center justify-center text-white text-5xl font-bold overflow-hidden">
          {profileUser.profilePicUrl ? (
            <img 
              src={profileUser.profilePicUrl.startsWith('http') ? profileUser.profilePicUrl : `http://localhost:8080${profileUser.profilePicUrl}`} 
              alt={profileUser.name} 
              className="w-full h-full object-cover"
            />
          ) : (
            profileUser.name ? profileUser.name.charAt(0).toUpperCase() : 'U'
          )}
        </div>
      </div>

      {/* Profile Info */}
      <div className="pt-20 px-4 pb-4 border-b border-(--border-color)">
        <div className="flex justify-between items-start mb-4">
          <div>
            <h2 className="text-2xl font-bold">{profileUser.name}</h2>
            <p className="text-(--text-secondary)">@{profileUser.username}</p>
          </div>
          {isOwnProfile ? (
            <button 
              onClick={() => setIsEditModalOpen(true)}
              className="px-4 py-1.5 rounded-full border border-(--border-color) font-bold hover:bg-(--bg-secondary) transition-colors"
            >
              Edit Profile
            </button>
          ) : (
            <button className="px-4 py-1.5 rounded-full bg-(--text-primary) text-(--bg-primary) font-bold hover:opacity-90 transition-opacity">
              Follow
            </button>
          )}
        </div>
        
        {/* Dynamic extra fields if any are returned, except userId */}
        {profileUser.bio && (
          <p className="mt-4 text-(--text-primary)">{profileUser.bio}</p>
        )}
        
        <div className="flex flex-wrap gap-4 mt-4 text-(--text-secondary) text-sm">
          {profileUser.location && (
            <div className="flex items-center gap-1">
              <MapPin size={16} />
              <span>{profileUser.location}</span>
            </div>
          )}
          {profileUser.website && (
            <div className="flex items-center gap-1">
              <LinkIcon size={16} />
              <a href={profileUser.website} className="text-(--color-primary) hover:underline" target="_blank" rel="noopener noreferrer">
                {profileUser.website.replace(/^https?:\/\//, '')}
              </a>
            </div>
          )}
          {profileUser.createdAt && (
            <div className="flex items-center gap-1">
              <Calendar size={16} />
              <span>Joined {format(new Date(profileUser.createdAt), 'MMMM yyyy')}</span>
            </div>
          )}
        </div>
        
        <div className="flex gap-4 mt-4 text-sm">
          <div className="cursor-pointer hover:underline" onClick={() => fetchFollowUsers('following')}>
            <span className="font-bold text-(--text-primary)">{profileUser.followingCount || 0}</span> <span className="text-(--text-secondary)">Following</span>
          </div>
          <div className="cursor-pointer hover:underline" onClick={() => fetchFollowUsers('followers')}>
            <span className="font-bold text-(--text-primary)">{profileUser.followersCount || 0}</span> <span className="text-(--text-secondary)">Followers</span>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-(--border-color)">
        <div className="flex-1 py-4 text-center font-bold relative cursor-pointer hover:bg-(--bg-secondary) transition-colors text-(--text-primary)">
          Posts
          <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-16 h-1 bg-(--color-primary) rounded-full"></div>
        </div>
      </div>

      {/* Feed */}
      <div>
        {thoughts.length > 0 ? (
          thoughts.map(thought => (
            <ThoughtCard key={thought.thoughtId || thought.id} thought={thought} showParent={true} />
          ))
        ) : (
          <div className="p-8 text-center text-(--text-secondary)">
            No thoughts yet.
          </div>
        )}
      </div>

      {/* Edit Profile Modal */}
      <EditProfileModal 
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        user={profileUser}
        onProfileUpdated={handleProfileUpdated}
      />

      {/* Users Modal for Followers/Following */}
      {showUsersModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm" onClick={() => setShowUsersModal(false)}>
          <div 
            className="bg-(--bg-primary) border border-(--border-color) rounded-2xl w-full max-w-sm max-h-[80vh] flex flex-col"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center gap-4 p-4 border-b border-(--border-color)">
              <button onClick={() => setShowUsersModal(false)} className="p-1 rounded-full hover:bg-(--bg-secondary) transition-colors">
                <X size={20} />
              </button>
              <h2 className="text-xl font-bold capitalize">{modalType}</h2>
            </div>
            
            <div className="flex-1 overflow-y-auto">
              {modalUsers.map(u => (
                <div 
                  key={u.userId} 
                  className="flex items-center gap-3 p-4 border-b border-(--border-color) hover:bg-(--bg-secondary) transition-colors cursor-pointer"
                  onClick={() => {
                    setShowUsersModal(false);
                    navigate(`/profile/${u.userId}`);
                  }}
                >
                  <div className="w-10 h-10 rounded-full bg-(--color-primary) flex items-center justify-center text-white font-bold shrink-0 overflow-hidden">
                    {u.profilePicUrl ? (
                      <img src={u.profilePicUrl.startsWith('http') ? u.profilePicUrl : `http://localhost:8080${u.profilePicUrl}`} alt={u.name} className="w-full h-full object-cover" />
                    ) : (
                      u.name?.charAt(0).toUpperCase()
                    )}
                  </div>
                  <div className="min-w-0">
                    <div className="font-bold text-(--text-primary) truncate">{u.name}</div>
                    <div className="text-sm text-(--text-secondary) truncate">@{u.username}</div>
                  </div>
                </div>
              ))}
              {loadingUsers && (
                <div className="p-4 text-center text-(--text-secondary)">Loading...</div>
              )}
              {!loadingUsers && modalUsers.length === 0 && (
                <div className="p-8 text-center text-(--text-secondary)">
                  No {modalType} yet.
                </div>
              )}
              {usersCursor && !loadingUsers && (
                <button 
                  onClick={() => fetchFollowUsers(modalType, usersCursor)}
                  className="w-full p-4 text-(--color-primary) hover:bg-(--bg-secondary) transition-colors font-medium"
                >
                  Load more
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Profile;
