import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

export const authAPI = {
  login: (email, password) => {
    return axios.post(`${API_BASE_URL}/auth/login`, { email, password });
  },

  register: (email, password, firstName, lastName) => {
    return axios.post(`${API_BASE_URL}/auth/register`, {
      email,
      password,
      firstName,
      lastName,
    });
  },

  health: () => {
    return axios.get(`${API_BASE_URL}/auth/health`);
  },
};

export const setAuthToken = (token) => {
  if (token) {
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    localStorage.setItem('authToken', token);
  } else {
    delete axios.defaults.headers.common['Authorization'];
    localStorage.removeItem('authToken');
  }
};

export const getAuthToken = () => {
  return localStorage.getItem('authToken');
};
