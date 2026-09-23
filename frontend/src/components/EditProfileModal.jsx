import React, { useState } from 'react';
import { X, Camera } from 'lucide-react';
import api from '../services/api';

const EditProfileModal = ({ isOpen, onClose, user, onProfileUpdated }) => {
  const [name, setName] = useState(user?.name || '');
  const [username, setUsername] = useState(user?.username || '');
  const [bio, setBio] = useState(user?.bio || '');
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(user?.profilePicUrl || '');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handleFileChange = (e) => {
    const selected = e.target.files[0];
    if (selected) {
      setFile(selected);
      setPreview(URL.createObjectURL(selected));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      const formData = new FormData();
      if (name.trim() !== user?.name || bio.trim() !== (user?.bio || '') || username.trim() !== (user?.username || '')) {
        formData.append('data', new Blob([JSON.stringify({ 
          name: name.trim(), 
          bio: bio.trim(),
          username: username.trim()
        })], { type: 'application/json' }));
      }
      
      if (file) {
        formData.append('file', file);
      }

      const res = await api.patch('/profile/', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });

      if (res.data.success) {
        const updatedUser = {
          ...user,
          name: name.trim() || user?.name,
          username: username.trim() || user?.username,
          bio: bio.trim() || user?.bio,
          // Since backend doesn't return the new minio URL immediately in the empty response,
          // we optimistically use the preview URL if a file was uploaded.
          profilePicUrl: file ? preview : user?.profilePicUrl 
        };
        onProfileUpdated(updatedUser);
        onClose();
      }
    } catch (err) {
      console.error('Failed to update profile', err);
      setError(err.response?.data?.message || 'Failed to update profile');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm" onClick={onClose}>
      <div 
        className="bg-(--bg-primary) border border-(--border-color) rounded-2xl w-full max-w-md overflow-hidden flex flex-col"
        onClick={e => e.stopPropagation()}
      >
        <div className="flex items-center justify-between p-4 border-b border-(--border-color)">
          <div className="flex items-center gap-4">
            <button onClick={onClose} className="p-1 rounded-full hover:bg-(--bg-secondary) transition-colors">
              <X size={20} />
            </button>
            <h2 className="text-xl font-bold">Edit profile</h2>
          </div>
          <button 
            onClick={handleSubmit}
            disabled={isSubmitting || (!name.trim())}
            className="px-4 py-1.5 bg-(--text-primary) text-(--bg-primary) font-bold rounded-full disabled:opacity-50 hover:opacity-90 transition-opacity"
          >
            {isSubmitting ? 'Saving...' : 'Save'}
          </button>
        </div>

        <div className="p-4 overflow-y-auto max-h-[70vh]">
          {error && <div className="text-red-500 mb-4 text-sm bg-red-500/10 p-2 rounded">{error}</div>}
          
          <div className="relative mb-8 flex justify-center">
            <div className="relative w-24 h-24 rounded-full overflow-hidden bg-(--bg-secondary) flex items-center justify-center border-4 border-(--bg-primary)">
              {preview ? (
                <img src={preview.startsWith('http') ? preview : `http://localhost:8080${preview}`} alt="Profile" className="w-full h-full object-cover" />
              ) : (
                <span className="text-3xl font-bold">{user?.name?.charAt(0).toUpperCase()}</span>
              )}
              <div className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 hover:opacity-100 transition-opacity cursor-pointer">
                <Camera size={24} className="text-white" />
                <input 
                  type="file" 
                  accept="image/jpeg,image/png,image/jpg" 
                  className="absolute inset-0 opacity-0 cursor-pointer"
                  onChange={handleFileChange}
                />
              </div>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm text-(--text-secondary) mb-1 px-1">Name</label>
              <input 
                type="text" 
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full bg-(--bg-primary) border border-(--border-color) rounded-lg px-3 py-2 text-(--text-primary) focus:outline-none focus:border-(--color-primary)"
                maxLength={50}
              />
            </div>

            <div>
              <label className="block text-sm text-(--text-secondary) mb-1 px-1">Username</label>
              <input 
                type="text" 
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full bg-(--bg-primary) border border-(--border-color) rounded-lg px-3 py-2 text-(--text-primary) focus:outline-none focus:border-(--color-primary)"
                maxLength={50}
              />
            </div>
            
            <div>
              <label className="block text-sm text-(--text-secondary) mb-1 px-1">Bio</label>
              <textarea 
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                className="w-full bg-(--bg-primary) border border-(--border-color) rounded-lg px-3 py-2 text-(--text-primary) focus:outline-none focus:border-(--color-primary) resize-none h-24"
                maxLength={160}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EditProfileModal;
