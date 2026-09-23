import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import logoUrl from '../assets/logo.png';
import { Feather, Loader } from 'lucide-react';

const Login = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await api.post('/auth/signin', { username, password });
      if (res.data.success) {
        login(res.data.data.token, res.data.data.userId);
        navigate('/');
      } else {
        setError(res.data.message || 'Login failed');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'An error occurred during login');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-(--bg-primary) flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-(--bg-secondary) rounded-3xl p-8 border border-(--border-color) shadow-2xl relative overflow-hidden glass">
        
        {/* Decorative elements */}
        <div className="absolute -top-20 -right-20 w-40 h-40 bg-pink-500 rounded-full mix-blend-multiply filter blur-3xl opacity-20"></div>
        <div className="absolute -bottom-20 -left-20 w-40 h-40 bg-blue-500 rounded-full mix-blend-multiply filter blur-3xl opacity-20"></div>
        
        <div className="relative z-10 flex flex-col items-center">
          <img src={logoUrl} alt="Thoughts & Opinions" className="w-24 h-24 mb-4 rounded-2xl shadow-lg object-cover" />
          <h1 className="text-3xl font-bold mb-2 bg-clip-text text-transparent bg-linear-to-r from-pink-500 to-blue-500">
            Welcome Back
          </h1>
          <p className="text-(--text-secondary) mb-8">Sign in to your account</p>

          {error && (
            <div className="w-full bg-red-500/10 border border-red-500/20 text-red-500 p-3 rounded-xl mb-6 text-sm text-center">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="w-full flex flex-col gap-4">
            <div>
              <label className="block text-sm font-medium mb-1 text-(--text-secondary)">Username</label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full bg-(--bg-primary) border border-(--border-color) rounded-xl p-3 focus:outline-none focus:border-(--color-primary) focus:ring-1 focus:ring-(--color-primary) transition-all text-(--text-primary)"
                placeholder="Enter your username"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1 text-(--text-secondary)">Password</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-(--bg-primary) border border-(--border-color) rounded-xl p-3 focus:outline-none focus:border-(--color-primary) focus:ring-1 focus:ring-(--color-primary) transition-all text-(--text-primary)"
                placeholder="Enter your password"
                required
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full mt-4 bg-linear-to-r from-pink-500 to-blue-500 hover:from-pink-600 hover:to-blue-600 text-white font-bold py-3 px-4 rounded-xl transition-all shadow-lg hover:shadow-xl flex items-center justify-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed"
            >
              {loading ? <Loader className="animate-spin" size={20} /> : 'Sign In'}
            </button>
          </form>

          <div className="mt-6 text-sm text-(--text-secondary)">
            Don't have an account?{' '}
            <Link to="/signup" className="text-(--color-primary) hover:underline font-medium">
              Sign up
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;
