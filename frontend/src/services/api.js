import axios from 'axios';


export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'https://140.245.199.122';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    const userId = localStorage.getItem('userId');
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      if (userId) {
        config.headers['X-User-Id'] = userId;
      }
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default api;
