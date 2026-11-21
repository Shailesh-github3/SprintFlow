import axios from 'axios';

// Create a centralized Axios instance
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Response interceptor for centralized error handling
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    const requestUrl = error.config?.url || '';
    const isAuthEndpoint = requestUrl.includes('/api/auth/login') || requestUrl.includes('/api/auth/register');

    if (error.response && error.response.status === 401 && !isAuthEndpoint) {
      localStorage.removeItem('user');
      sessionStorage.setItem('session-expired', '1');
      window.dispatchEvent(new Event('auth:logout'));
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
