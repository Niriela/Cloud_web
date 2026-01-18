import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAuthToken } from '../api/authAPI';
import './Dashboard.css';

function Dashboard() {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);

  useEffect(() => {
    const userData = localStorage.getItem('user');
    if (userData) {
      setUser(JSON.parse(userData));
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('authToken');
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <div className="dashboard-container">
      <div className="dashboard-content">
        <header className="dashboard-header">
          <h1>Bienvenue sur Cloud Web</h1>
          <button onClick={handleLogout} className="logout-button">
            Déconnexion
          </button>
        </header>

        {user && (
          <div className="user-info">
            <h2>Informations utilisateur</h2>
            <div className="info-card">
              <p>
                <strong>Nom:</strong> {user.firstName} {user.lastName}
              </p>
              <p>
                <strong>Email:</strong> {user.email}
              </p>
            </div>
          </div>
        )}

        <div className="dashboard-main">
          <p>Vous êtes maintenant connecté à l'application Cloud Web!</p>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
