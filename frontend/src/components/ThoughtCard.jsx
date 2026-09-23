import React, { useState, useEffect } from 'react';
import { formatDistanceToNow } from 'date-fns';
import { Heart, Repeat, MessageCircle, Share, Trash2 } from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

const ThoughtCard = ({ thought, showParent = false }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user: currentUser } = useAuth();

  const [isLiked, setIsLiked] = useState(thought.isLiked || false);
  const [likesCount, setLikesCount] = useState(thought.likesCount || 0);
  const [isReposted, setIsReposted] = useState(thought.isReposted || false);
  const [repostsCount, setRepostsCount] = useState(thought.repostsCount || 0);
  
  const [parentThought, setParentThought] = useState(null);
  const [loadingParent, setLoadingParent] = useState(false);
  
  // Modals state
  const [showInteractModal, setShowInteractModal] = useState(false);
  const [interactModalTitle, setInteractModalTitle] = useState('');
  const [interactType, setInteractType] = useState('');
  const [interactUsers, setInteractUsers] = useState([]);
  const [loadingInteract, setLoadingInteract] = useState(false);
  const [interactNextCursor, setInteractNextCursor] = useState(null);

  const [showMenu, setShowMenu] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteInput, setDeleteInput] = useState('');
  const [isDeleted, setIsDeleted] = useState(false);

  useEffect(() => {
    // If showParent is true and there is a parentThoughtId, fetch parent details
    if (showParent && thought.parentThoughtId) {
      const fetchParent = async () => {
        setLoadingParent(true);
        try {
          const res = await api.get(`/thoughts/${thought.parentThoughtId}`);
          if (res.data.success) {
            setParentThought(res.data.data);
          }
        } catch (error) {
          console.error("Failed to fetch parent thought", error);
        } finally {
          setLoadingParent(false);
        }
      };
      fetchParent();
    }
  }, [showParent, thought.parentThoughtId]);

  const handleLike = async (e) => {
    e.stopPropagation();
    
    // Optimistic Update
    const wasLiked = isLiked;
    setIsLiked(!wasLiked);
    setLikesCount(prev => wasLiked ? prev - 1 : prev + 1);

    try {
      if (wasLiked) {
        await api.delete(`/thoughts/interact/${thought.thoughtId || thought.id}`);
      } else {
        await api.get(`/thoughts/interact/${thought.thoughtId || thought.id}`);
      }
    } catch (error) {
      // Revert on failure
      console.error("Failed to toggle like", error);
      setIsLiked(wasLiked);
      setLikesCount(prev => wasLiked ? prev + 1 : prev - 1);
    }
  };

  const handleRepost = async (e) => {
    e.stopPropagation();
    
    const wasReposted = isReposted;
    setIsReposted(!wasReposted);
    setRepostsCount(prev => wasReposted ? prev - 1 : prev + 1);
    
    try {
       if (wasReposted) {
         // Backend doesn't have an unrepost endpoint that takes parentThoughtId. 
         console.warn("Un-repost not supported by backend.");
         setIsReposted(true);
         setRepostsCount(prev => prev + 1);
       } else {
         const currentId = thought.thoughtId || thought.id;
         await api.post(`/thoughts/`, { parentThoughtId: currentId, content: null });
       }
    } catch (error) {
       console.error("Failed to toggle repost", error);
       setIsReposted(wasReposted);
       setRepostsCount(prev => wasReposted ? prev + 1 : prev - 1);
    }
  };

  const navigateToProfile = (e, userId) => {
    e.stopPropagation();
    navigate(`/profile/${userId}`);
  };

  const navigateToThought = (e) => {
    e.stopPropagation();
    const currentId = thought.thoughtId || thought.id;
    // Don't navigate if we are already on this thought's detail page
    if (location.pathname !== `/thought/${currentId}`) {
      navigate(`/thought/${currentId}`);
    }
  };

  const fetchInteractUsers = async (type, cursor = null) => {
    if (!cursor) {
      setInteractModalTitle(type === 'likes' ? 'Liked by' : 'Reposted by');
      setInteractType(type);
      setShowInteractModal(true);
      setInteractUsers([]);
    }
    setLoadingInteract(true);
    
    try {
      const currentId = thought.thoughtId || thought.id;
      const url = cursor 
        ? `/thoughts/${type}/${currentId}?limit=20&cursor=${cursor}`
        : `/thoughts/${type}/${currentId}?limit=20`;
      
      const res = await api.get(url);
      if (res.data.success) {
        if (cursor) {
          setInteractUsers(prev => [...prev, ...(res.data.data.users || [])]);
        } else {
          setInteractUsers(res.data.data.users || []);
        }
        setInteractNextCursor(res.data.data.nextCursor);
      }
    } catch (error) {
      console.error(`Failed to fetch ${type}`, error);
    } finally {
      setLoadingInteract(false);
    }
  };

  const handleInteractScroll = (e) => {
    const { scrollTop, scrollHeight, clientHeight } = e.target;
    if (scrollHeight - scrollTop <= clientHeight * 1.5 && !loadingInteract && interactNextCursor) {
      fetchInteractUsers(interactType, interactNextCursor);
    }
  };

  const handleLikesClick = (e) => {
    e.stopPropagation();
    if (likesCount > 0) fetchInteractUsers('likes');
  };

  const handleRepostsClick = (e) => {
    e.stopPropagation();
    if (repostsCount > 0) fetchInteractUsers('reposts');
  };

  const handleDeleteThought = async () => {
    try {
      const res = await api.delete(`/thoughts/${thought.thoughtId || thought.id}`);
      if (res.data.success) {
        setIsDeleted(true);
        setShowDeleteConfirm(false);
      }
    } catch (error) {
      console.error('Failed to delete thought', error);
    }
  };

  if (isDeleted) return null;

  const renderCardContent = (data, isParent = false) => {
    // user detail from thought (or attached manually by feed)
    const user = data.user || data; 
    
    return (
      <div className={`p-4 border-b border-(--border-color) hover:bg-(--bg-secondary) transition-colors cursor-pointer ${isParent ? 'pb-2 border-b-0' : ''}`} onClick={navigateToThought}>
        {isParent && (
          <div className="flex items-center gap-2 text-(--text-secondary) text-sm font-medium mb-1 ml-10">
             Replying to this thought
          </div>
        )}
        <div className="flex gap-3">
          {/* Avatar */}
          <div 
            className="w-12 h-12 rounded-full bg-(--color-primary) flex items-center justify-center text-white font-bold shrink-0 cursor-pointer hover:opacity-80 transition-opacity"
            onClick={(e) => navigateToProfile(e, user.userId || user.id)}
          >
            {user.name ? user.name.charAt(0).toUpperCase() : 'U'}
          </div>
          
          <div className="flex-1 min-w-0">
            {/* Header */}
            <div className="flex items-center justify-between">
              <div 
                className="flex items-center gap-1 overflow-hidden shrink cursor-pointer group"
                onClick={(e) => navigateToProfile(e, user.userId || user.id)}
              >
                <span className="font-bold text-(--text-primary) truncate group-hover:underline">
                  {user.name || 'Unknown User'}
                </span>
                <span className="text-(--text-secondary) truncate text-sm">
                  @{user.username || 'unknown'}
                </span>
                <span className="text-(--text-secondary) shrink-0 text-sm">
                  · {data.createdAt ? formatDistanceToNow(new Date(data.createdAt), { addSuffix: true }) : ''}
                </span>
              </div>
              
              <div className="relative">
                {/* Only show delete if the current user owns it */}
                {(currentUser?.user_id || currentUser?.userId) == (user.userId || user.id) && (
                  <button 
                    onClick={(e) => { e.stopPropagation(); setShowDeleteConfirm(true); setDeleteInput(''); }}
                    className="text-(--text-secondary) hover:text-red-500 transition-colors p-1 rounded-full hover:bg-red-500/10"
                    title="Delete Thought"
                  >
                    <Trash2 size={18} />
                  </button>
                )}
              </div>
            </div>

            {/* Content */}
            <div className="mt-1 text-(--text-primary) whitespace-pre-wrap break-words">
              {data.content}
            </div>

            {/* Interactions (only for main thought, not parent preview unless desired) */}
            {!isParent && (
              <div className="flex items-center justify-between mt-3 text-(--text-secondary) max-w-md">
                <button className="flex items-center gap-2 hover:text-(--color-primary) group transition-colors">
                  <div className="p-2 rounded-full group-hover:bg-blue-500/10">
                    <MessageCircle size={18} />
                  </div>
                  <span className="text-sm">{data.opinionsCount || 0}</span>
                </button>
                
                <div className={`flex items-center gap-2 group transition-colors ${isReposted ? 'text-(--color-repost)' : 'hover:text-(--color-repost)'}`} onClick={(e) => e.stopPropagation()}>
                  <button 
                    onClick={handleRepost}
                    className={`p-2 rounded-full ${isReposted ? 'bg-green-500/10' : 'group-hover:bg-green-500/10'}`}
                  >
                    <Repeat size={18} />
                  </button>
                  <span onClick={handleRepostsClick} className={`text-sm ${repostsCount > 0 ? 'cursor-pointer hover:underline' : ''} p-1`}>{repostsCount}</span>
                </div>

                <div className={`flex items-center gap-2 group transition-colors ${isLiked ? 'text-(--color-like)' : 'hover:text-(--color-like)'}`} onClick={(e) => e.stopPropagation()}>
                  <button 
                    onClick={handleLike}
                    className={`p-2 rounded-full ${isLiked ? 'bg-pink-500/10' : 'group-hover:bg-pink-500/10'}`}
                  >
                    <Heart size={18} className={isLiked ? "fill-current" : ""} />
                  </button>
                  <span onClick={handleLikesClick} className={`text-sm ${likesCount > 0 ? 'cursor-pointer hover:underline' : ''} p-1`}>{likesCount}</span>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="flex flex-col w-full border-b border-(--border-color) hover:bg-(--bg-secondary)/30 transition-colors cursor-pointer" onClick={navigateToThought}>
      {showParent && thought.parentThoughtId && parentThought && (
        <div className="relative">
          {renderCardContent(parentThought, true)}
          <div className="absolute left-10 top-16 bottom-0 w-0.5 bg-(--border-color) -z-10"></div>
        </div>
      )}
      {renderCardContent(thought, false)}
      
      {/* Interact Users Modal */}
      {showInteractModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm" onClick={(e) => { e.stopPropagation(); setShowInteractModal(false); }}>
          <div className="bg-(--bg-primary) w-full max-w-sm rounded-2xl shadow-xl overflow-hidden" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between p-4 border-b border-(--border-color)">
              <h2 className="text-xl font-bold">{interactModalTitle}</h2>
              <button onClick={() => setShowInteractModal(false)} className="text-(--text-secondary) hover:text-(--text-primary)">
                ✕
              </button>
            </div>
            <div className="p-4 max-h-80 overflow-y-auto" onScroll={handleInteractScroll}>
              {interactUsers.length > 0 ? (
                <>
                  {interactUsers.map((u, i) => (
                    <div key={i} className="flex items-center gap-3 mb-4 last:mb-0 cursor-pointer hover:bg-(--bg-secondary)/50 p-2 rounded-xl transition-colors" onClick={(e) => { setShowInteractModal(false); navigateToProfile(e, u.userId); }}>
                      <div className="w-10 h-10 rounded-full bg-(--color-primary) flex items-center justify-center text-white font-bold shrink-0">
                        {(u.name || u.username || 'U').charAt(0).toUpperCase()}
                      </div>
                      <div className="flex flex-col">
                        <span className="font-bold text-(--text-primary)">{u.name || u.username}</span>
                        <span className="text-sm text-(--text-secondary)">@{u.username}</span>
                      </div>
                    </div>
                  ))}
                  {loadingInteract && <div className="text-center text-(--text-secondary) py-4">Loading more...</div>}
                </>
              ) : loadingInteract ? (
                <div className="text-center text-(--text-secondary) py-4">Loading...</div>
              ) : (
                <div className="text-center text-(--text-secondary) py-4">No users found.</div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm" onClick={(e) => { e.stopPropagation(); setShowDeleteConfirm(false); }}>
          <div className="bg-(--bg-primary) w-full max-w-sm rounded-2xl shadow-xl overflow-hidden p-6" onClick={e => e.stopPropagation()}>
            <h2 className="text-xl font-bold mb-2">Delete Thought?</h2>
            <p className="text-(--text-secondary) mb-6">
              This can't be undone and it will be removed from your profile, the timeline of any accounts that follow you, and from search results.
            </p>
            <div className="flex flex-col gap-3">
              <input 
                type="text" 
                placeholder="Type 'delete' to confirm"
                value={deleteInput}
                onChange={(e) => setDeleteInput(e.target.value)}
                className="w-full bg-(--bg-secondary) border border-(--border-color) rounded-xl px-4 py-3 mb-2 text-(--text-primary) placeholder-(--text-secondary) focus:outline-none focus:border-(--color-primary) focus:ring-1 focus:ring-(--color-primary) transition-all"
              />
              <button 
                onClick={handleDeleteThought}
                disabled={deleteInput.toLowerCase() !== 'delete'}
                className="w-full bg-red-500 hover:bg-red-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-bold py-3 rounded-full transition-colors"
              >
                Delete
              </button>
              <button 
                onClick={() => setShowDeleteConfirm(false)}
                className="w-full border border-(--border-color) hover:bg-(--bg-secondary) text-(--text-primary) font-bold py-3 rounded-full transition-colors"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ThoughtCard;
