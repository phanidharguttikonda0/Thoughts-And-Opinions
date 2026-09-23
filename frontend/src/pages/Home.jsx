import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import ThoughtCard from '../components/ThoughtCard';
import { Loader, Image as ImageIcon, Smile } from 'lucide-react';

const Home = () => {
  const { user } = useAuth();
  const [thoughts, setThoughts] = useState([]);
  const [loading, setLoading] = useState(true);
  
  const [newThought, setNewThought] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showSuccessModal, setShowSuccessModal] = useState(false);

  useEffect(() => {
    fetchFeed();
  }, []);

  const fetchFeed = async () => {
    try {
      setLoading(true);
      const res = await api.get('/feed');
      if (res.data.success) {
        setThoughts(res.data.data.thoughts || []);
      }
    } catch (error) {
      console.error("Failed to fetch feed", error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateThought = async (e) => {
    e.preventDefault();
    if (!newThought.trim()) return;

    setIsSubmitting(true);
    try {
      const res = await api.post('/thoughts/', { content: newThought });
      if (res.data.success) {
        setShowSuccessModal(true);
        setNewThought('');
        // Option 1: fetch feed again. Option 2: let user refresh manually.
        fetchFeed();
        
        // Hide modal after 3 seconds
        setTimeout(() => {
          setShowSuccessModal(false);
        }, 3000);
      }
    } catch (error) {
      console.error("Failed to create thought", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="w-full max-w-2xl">
      <header className="sticky top-0 z-10 bg-(--bg-primary)/80 backdrop-blur-md border-b border-(--border-color) px-4 py-3 flex items-center justify-between">
        <h1 className="text-xl font-bold">Home</h1>
      </header>

      {/* Create Thought Form */}
      <div className="p-4 border-b border-(--border-color) flex gap-3">
        <div className="w-12 h-12 rounded-full bg-(--color-primary) flex items-center justify-center text-white font-bold shrink-0">
          {user?.sub ? user.sub.charAt(0).toUpperCase() : 'U'}
        </div>
        <div className="flex-1">
          <textarea
            value={newThought}
            onChange={(e) => setNewThought(e.target.value)}
            placeholder="What is happening?!"
            className="w-full bg-transparent text-xl resize-none outline-none text-(--text-primary) placeholder-(--text-secondary) min-h-[100px] py-2"
          />
          
          <div className="flex items-center justify-between mt-2 pt-2 border-t border-(--border-color)">
            <div className="flex gap-2 text-(--color-primary)">
              <button className="p-2 rounded-full hover:bg-blue-500/10 transition-colors">
                <ImageIcon size={20} />
              </button>
              <button className="p-2 rounded-full hover:bg-blue-500/10 transition-colors">
                <Smile size={20} />
              </button>
            </div>
            <button
              onClick={handleCreateThought}
              disabled={isSubmitting || !newThought.trim()}
              className="bg-(--color-primary) hover:bg-(--color-primary-hover) text-white font-bold py-2 px-5 rounded-full transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmitting ? <Loader className="animate-spin" size={18} /> : 'Post'}
            </button>
          </div>
        </div>
      </div>

      {/* Feed */}
      <div>
        {loading ? (
          <div className="p-8 flex justify-center">
            <Loader className="animate-spin text-(--color-primary)" size={32} />
          </div>
        ) : thoughts.length > 0 ? (
          thoughts.map(thought => (
            <ThoughtCard key={thought.id} thought={thought} showParent={true} />
          ))
        ) : (
          <div className="p-8 text-center text-(--text-secondary)">
            No thoughts yet. Be the first to post!
          </div>
        )}
      </div>
      
      {showSuccessModal && (
        <div className="fixed bottom-4 right-4 bg-(--color-repost) text-white px-6 py-3 rounded-lg shadow-lg z-50 flex items-center gap-2 animate-bounce">
          <span>✨</span> Post successfully posted!
        </div>
      )}
    </div>
  );
};

export default Home;
