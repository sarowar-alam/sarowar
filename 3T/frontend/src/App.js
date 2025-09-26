import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost/api';

function App() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [newUser, setNewUser] = useState('');

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const response = await axios.get(`${API_URL}/users`);
      setUsers(response.data);
    } catch (error) {
      console.error('Error fetching users:', error);
    }
    setLoading(false);
  };

  const addUser = async () => {
    if (!newUser.trim()) return;
    
    try {
      await axios.post(`${API_URL}/users`, { name: newUser });
      setNewUser('');
      fetchUsers(); // Refresh the list
    } catch (error) {
      console.error('Error adding user:', error);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  return (
    <div style={{ padding: '20px' }}>
      <h1>3-Tier Application Demo</h1>
      
      <div style={{ marginBottom: '20px' }}>
        <input
          type="text"
          value={newUser}
          onChange={(e) => setNewUser(e.target.value)}
          placeholder="Enter user name"
          style={{ marginRight: '10px', padding: '5px' }}
        />
        <button onClick={addUser} style={{ padding: '5px 10px' }}>
          Add User
        </button>
      </div>

      <button onClick={fetchUsers} disabled={loading} style={{ marginBottom: '20px' }}>
        {loading ? 'Loading...' : 'Refresh Users'}
      </button>

      <h2>Users from Database (via Backend + Cache):</h2>
      <ul>
        {users.map((user) => (
          <li key={user.id}>
            {user.name} (ID: {user.id})
          </li>
        ))}
      </ul>

      <div style={{ marginTop: '20px', padding: '10px', background: '#f5f5f5' }}>
        <strong>Architecture:</strong> React Frontend → Nginx → Node.js Backend → Redis Cache → PostgreSQL Database
      </div>
    </div>
  );
}

export default App;