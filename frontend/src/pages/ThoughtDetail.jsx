import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import ThoughtCard from '../components/ThoughtCard';
import { Loader, ArrowLeft } from 'lucide-react';

const ThoughtDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [thought, setThought] = useState(null);
  const [opinions, setOpinions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const [newOpinion, setNewOpinion] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showSuccessModal, setShowSuccessModal] = useState(false);

  useEffect(() => {
    fetchThoughtDetails();
    // Scroll to top when thought ID changes
    window.scrollTo(0, 0);
  }, [id]);

  const fetchThoughtDetails = async () => {
    setLoading(true);
    setError(null);
    try {
      // Fetch the main thought
      const thoughtRes = await api.get(`/thoughts/${id}`);
      if (thoughtRes.data.success) {
        setThought(thoughtRes.data.data);
      } else {
        throw new Error(thoughtRes.data.message || 'Thought not found');
      }

      // Fetch opinions (replies) for this thought
      const opinionsRes = await api.get(`/thoughts/opinions/${id}`);
      if (opinionsRes.data.success) {
        // We might need to handle pagination in the future, for now just use the array
        setOpinions(opinionsRes.data.data.thoughts || []);
      }
    } catch (err) {
      console.error('Failed to load thought details', err);
      setError('Failed to load thought.');
    } finally {
      setLoading(false);
    }
  };

  const handlePostOpinion = async (e) => {
    e.preventDefault();
    if (!newOpinion.trim()) return;

    setIsSubmitting(true);
    try {
      const res = await api.post('/thoughts/', {
        content: newOpinion,
        parentThoughtId: id
      });
      if (res.data.success) {
        setShowSuccessModal(true);
        setNewOpinion('');
        // Option 1: fetch feed again
        fetchThought();
        // Hide modal after 3 seconds
        setTimeout(() => {
          setShowSuccessModal(false);
        }, 3000);
      }
    } catch (err) {
      console.error('Failed to post opinion', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="p-8 flex justify-center w-full max-w-2xl">
        <Loader className="animate-spin text-(--color-primary)" size={32} />
      </div>
    );
  }

  if (error || !thought) {
    return (
      <div className="w-full max-w-2xl">
        <header className="sticky top-0 z-10 bg-(--bg-primary)/80 backdrop-blur-md border-b border-(--border-color) px-4 py-2 flex items-center gap-6">
          <button onClick={() => navigate(-1)} className="p-2 rounded-full hover:bg-(--bg-secondary) transition-colors">
            <ArrowLeft size={20} />
          </button>
          <h1 className="text-xl font-bold">Thought</h1>
        </header>
        <div className="p-8 text-center text-red-500">
          {error || "Thought not found"}
        </div>
      </div>
    );
  }

  return (
    <div className="w-full max-w-2xl">
      <header className="sticky top-0 z-10 bg-(--bg-primary)/80 backdrop-blur-md border-b border-(--border-color) px-4 py-2 flex items-center gap-6">
        <button onClick={() => navigate(-1)} className="p-2 rounded-full hover:bg-(--bg-secondary) transition-colors">
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-xl font-bold">Thought</h1>
      </header>

      {/* Main Thought - we don't show its parent here by default, or we could if we wanted threaded view. Let's show its parent if it has one. */}
      <div>
        <ThoughtCard thought={thought} showParent={true} />
      </div>

      {/* Reply Input Box */}
      <div className="p-4 border-b border-(--border-color) flex gap-3">
        <div className="w-12 h-12 rounded-full bg-(--color-primary) flex items-center justify-center text-white font-bold shrink-0">
          {user?.sub ? user.sub.charAt(0).toUpperCase() : 'U'}
        </div>
        <div className="flex-1">
          <textarea
            value={newOpinion}
            onChange={(e) => setNewOpinion(e.target.value)}
            placeholder="Post your reply"
            className="w-full bg-transparent text-xl resize-none outline-none text-(--text-primary) placeholder-(--text-secondary) min-h-[60px] py-2"
          />
          <div className="flex justify-end mt-2 pt-2">
            <button
              onClick={handlePostOpinion}
              disabled={isSubmitting || !newOpinion.trim()}
              className="bg-(--color-primary) hover:bg-(--color-primary-hover) text-white font-bold py-1.5 px-4 rounded-full transition-colors disabled:opacity-50 disabled:cursor-not-allowed text-sm"
            >
              {isSubmitting ? <Loader className="animate-spin" size={16} /> : 'Reply'}
            </button>
          </div>
        </div>
      </div>

      {/* Opinions Feed */}
      <div className="flex flex-col">
        {opinions.length > 0 ? (
          opinions.map(opinion => (
            <div key={opinion.id} className="flex w-full">
              {/* A subtle thread indicator on the left */}
              <div className="w-4 sm:w-12 shrink-0 border-r-2 border-(--border-color) opacity-30 mt-4 mb-4"></div>
              <div className="flex-1 overflow-hidden">
                <ThoughtCard thought={opinion} showParent={false} />
              </div>
            </div>
          ))
        ) : (
          <div className="p-8 text-center text-(--text-secondary)">
            No replies yet.
          </div>
        )}
      </div>
      
      {showSuccessModal && (
        <div className="fixed bottom-4 right-4 bg-(--color-repost) text-white px-6 py-3 rounded-lg shadow-lg z-50 flex items-center gap-2 animate-bounce">
          <span>✨</span> Reply successfully posted!
        </div>
      )}
    </div>
  );
};

export default ThoughtDetail;
