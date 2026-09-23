import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import logoUrl from '../assets/logo.png';
import { Loader } from 'lucide-react';

const Signup = () => {
  const [formData, setFormData] = useState({
    username: '',
    name: '',
    email: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      // 1. Register User
      const res = await api.post('/auth/signup', formData);
      if (res.data.success) {
        // 2. Auto Login after Registration
        const loginRes = await api.post('/auth/signin', { 
          username: formData.username, 
          password: formData.password 
        });
        if (loginRes.data.success) {
          login(loginRes.data.data.token, loginRes.data.data.userId);
          navigate('/');
        } else {
          // Fallback to manual login
          navigate('/login');
        }
      } else {
        setError(res.data.message || 'Registration failed');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'An error occurred during registration');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-(--bg-primary) flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-(--bg-secondary) rounded-3xl p-8 border border-(--border-color) shadow-2xl relative overflow-hidden glass">
        
        <div className="absolute -top-20 -right-20 w-40 h-40 bg-pink-500 rounded-full mix-blend-multiply filter blur-3xl opacity-20"></div>
        <div className="absolute -bottom-20 -left-20 w-40 h-40 bg-blue-500 rounded-full mix-blend-multiply filter blur-3xl opacity-20"></div>
        
        <div className="relative z-10 flex flex-col items-center">
          <img src={logoUrl} alt="Thoughts & Opinions" className="w-16 h-16 mb-4 rounded-xl shadow-lg object-cover" />
          <h1 className="text-3xl font-bold mb-2 bg-clip-text text-transparent bg-linear-to-r from-pink-500 to-blue-500">
            Join Now
          </h1>
          <p className="text-(--text-secondary) mb-6">Create your account</p>

          {error && (
            <div className="w-full bg-red-500/10 border border-red-500/20 text-red-500 p-3 rounded-xl mb-6 text-sm text-center">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="w-full flex flex-col gap-4">
            <div>
              <label className="block text-sm font-medium mb-1 text-(--text-secondary)">Name</label>
              <input
                type="text"
                name="name"
                value={formData.name}
                onChange={handleChange}
                className="w-full bg-(--bg-primary) border border-(--border-color) rounded-xl p-3 focus:outline-none focus:border-(--color-primary) focus:ring-1 focus:ring-(--color-primary) transition-all text-(--text-primary)"
                placeholder="Full Name"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1 text-(--text-secondary)">Username</label>
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleChange}
                className="w-full bg-(--bg-primary) border border-(--border-color) rounded-xl p-3 focus:outline-none focus:border-(--color-primary) focus:ring-1 focus:ring-(--color-primary) transition-all text-(--text-primary)"
                placeholder="@username"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1 text-(--text-secondary)">Email</label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                className="w-full bg-(--bg-primary) border border-(--border-color) rounded-xl p-3 focus:outline-none focus:border-(--color-primary) focus:ring-1 focus:ring-(--color-primary) transition-all text-(--text-primary)"
                placeholder="Email Address"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1 text-(--text-secondary)">Password</label>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                className="w-full bg-(--bg-primary) border border-(--border-color) rounded-xl p-3 focus:outline-none focus:border-(--color-primary) focus:ring-1 focus:ring-(--color-primary) transition-all text-(--text-primary)"
                placeholder="Password"
                required
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full mt-2 bg-linear-to-r from-pink-500 to-blue-500 hover:from-pink-600 hover:to-blue-600 text-white font-bold py-3 px-4 rounded-xl transition-all shadow-lg hover:shadow-xl flex items-center justify-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed"
            >
              {loading ? <Loader className="animate-spin" size={20} /> : 'Sign Up'}
            </button>
          </form>

          <div className="mt-6 text-sm text-(--text-secondary)">
            Already have an account?{' '}
            <Link to="/login" className="text-(--color-primary) hover:underline font-medium">
              Log in
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Signup;
