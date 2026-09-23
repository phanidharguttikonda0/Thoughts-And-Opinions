import React, { useState } from 'react';
import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom';
import { Home, User, LogOut, Search, Moon, Sun, Feather } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import api from '../services/api';

const Sidebar = () => {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const location = useLocation();

  const navItems = [
    { name: 'Home', path: '/', icon: Home },
    { name: 'Profile', path: `/profile/${user?.user_id}`, icon: User },
  ];

  return (
    <div className="w-64 h-screen fixed left-0 border-r border-(--border-color) bg-(--bg-primary) flex flex-col p-4">
      <div className="flex items-center gap-3 mb-8 px-2">
        <div className="w-10 h-10 rounded-full bg-linear-to-tr from-pink-500 to-blue-500 flex items-center justify-center">
          <Feather className="text-white" size={24} />
        </div>
        <span className="text-xl font-bold bg-clip-text text-transparent bg-linear-to-r from-pink-500 to-blue-500">
          Thoughts
        </span>
      </div>

      <nav className="flex-1 flex flex-col gap-2">
        {navItems.map((item) => (
          <Link
            key={item.name}
            to={item.path}
            className={`flex items-center gap-4 px-4 py-3 rounded-full transition-all duration-200 ${
              location.pathname === item.path
                ? 'bg-(--color-primary) text-white'
                : 'hover:bg-(--bg-secondary) text-(--text-primary)'
            }`}
          >
            <item.icon size={24} />
            <span className="text-xl font-medium">{item.name}</span>
          </Link>
        ))}
      </nav>

      <div className="flex flex-col gap-2 mt-auto">
        <button
          onClick={toggleTheme}
          className="flex items-center gap-4 px-4 py-3 rounded-full hover:bg-(--bg-secondary) transition-colors text-left"
        >
          {theme === 'dark' ? <Sun size={24} /> : <Moon size={24} />}
          <span className="text-xl font-medium">{theme === 'dark' ? 'Light Mode' : 'Dark Mode'}</span>
        </button>
        <button
          onClick={logout}
          className="flex items-center gap-4 px-4 py-3 rounded-full hover:bg-red-500/10 text-red-500 transition-colors text-left"
        >
          <LogOut size={24} />
          <span className="text-xl font-medium">Log out</span>
        </button>
        
        {user && (
          <div className="flex items-center gap-3 mt-4 p-3 rounded-full hover:bg-(--bg-secondary) transition-colors cursor-pointer" onClick={() => navigate(`/profile/${user.user_id}`)}>
            <div className="w-10 h-10 rounded-full bg-(--color-primary) flex items-center justify-center text-white font-bold shrink-0 overflow-hidden">
              {user.profilePicUrl ? (
                <img 
                  src={user.profilePicUrl.startsWith('http') ? user.profilePicUrl : `http://localhost:8080${user.profilePicUrl}`} 
                  alt={user.sub} 
                  className="w-full h-full object-cover" 
                />
              ) : (
                user.sub ? user.sub.charAt(0).toUpperCase() : 'U'
              )}
            </div>
            <div className="flex flex-col overflow-hidden">
              <span className="font-bold truncate text-(--text-primary)">{user.sub}</span>
              <span className="text-(--text-secondary) truncate text-sm">@{user.sub}</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

const RightSidebar = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const navigate = useNavigate();

  const handleSearch = async (e) => {
    const query = e.target.value;
    setSearchQuery(query);
    
    if (query.trim().length === 0) {
      setSearchResults([]);
      return;
    }

    try {
      setIsSearching(true);
      const response = await api.get(`/profile/search/${query}`);
      if (response.data.success) {
        // Only show top 5
        setSearchResults((response.data.data.users || []).slice(0, 5));
      } else {
        setSearchResults([]);
      }
    } catch (error) {
      console.error('Search failed', error);
      setSearchResults([]);
    } finally {
      setIsSearching(false);
    }
  };

  const handleUserClick = (userId) => {
    setSearchQuery('');
    setSearchResults([]);
    navigate(`/profile/${userId}`);
  };

  return (
    <div className="w-80 h-screen fixed right-0 border-l border-(--border-color) bg-(--bg-primary) p-4 hidden lg:block">
      <div className="relative group">
        <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
          <Search className="text-(--text-secondary) group-focus-within:text-(--color-primary)" size={20} />
        </div>
        <input
          type="text"
          placeholder="Search users..."
          value={searchQuery}
          onChange={handleSearch}
          className="w-full bg-(--bg-secondary) border border-transparent rounded-full py-3 pl-12 pr-4 focus:outline-none focus:border-(--color-primary) focus:bg-(--bg-primary) transition-all text-(--text-primary)"
        />
        
        {/* Search Results Dropdown */}
        {searchQuery.trim().length > 0 && (
          <div className="absolute top-full left-0 right-0 mt-2 bg-(--bg-primary) border border-(--border-color) rounded-2xl shadow-xl overflow-hidden z-50">
            {isSearching ? (
              <div className="p-4 text-center text-(--text-secondary)">Searching...</div>
            ) : searchResults.length > 0 ? (
              searchResults.map(user => (
                <div 
                  key={user.userId} 
                  onClick={() => handleUserClick(user.userId)}
                  className="p-3 hover:bg-(--bg-secondary) cursor-pointer flex items-center gap-3 transition-colors"
                >
                  <div className="w-10 h-10 rounded-full bg-(--color-primary) flex items-center justify-center text-white font-bold shrink-0">
                    {user.name ? user.name.charAt(0).toUpperCase() : 'U'}
                  </div>
                  <div className="flex flex-col overflow-hidden">
                    <span className="font-bold truncate text-(--text-primary)">{user.name}</span>
                    <span className="text-(--text-secondary) truncate text-sm">@{user.username}</span>
                  </div>
                </div>
              ))
            ) : (
              <div className="p-4 text-center text-(--text-secondary)">No users found</div>
            )}
          </div>
        )}
      </div>
      
      <div className="mt-6 bg-(--bg-secondary) rounded-2xl p-4">
        <h2 className="text-xl font-bold mb-4">What's happening</h2>
        <div className="text-(--text-secondary) text-sm text-center">
          Trends will appear here
        </div>
      </div>
    </div>
  );
};

const Layout = () => {
  return (
    <div className="min-h-screen bg-(--bg-primary) text-(--text-primary) flex justify-center max-w-7xl mx-auto">
      <Sidebar />
      <main className="flex-1 ml-64 lg:mr-80 min-h-screen border-r border-(--border-color) border-l-transparent">
        <Outlet />
      </main>
      <RightSidebar />
    </div>
  );
};

export default Layout;
