import React, { useState } from 'react';
import 'bootstrap/dist/css/bootstrap.min.css';

function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  
  return (
    <div className="container-fluid min-vh-100 bg-light p-0">
      <nav className="navbar navbar-expand-lg navbar-dark bg-dark px-4 py-3">
        <a className="navbar-brand fw-bold text-white d-flex align-items-center" href="#/">
          <span className="badge bg-primary me-2">AI</span> SmartLB-AI Control Panel
        </a>
      </nav>
      
      <div className="row g-0">
        <div className="col-md-2 bg-white border-end min-vh-100 p-3">
          <ul className="nav flex-column gap-2">
            <li className="nav-item">
              <button 
                onClick={() => setActiveTab('dashboard')} 
                className={`btn w-100 text-start px-3 py-2 fw-semibold ${activeTab === 'dashboard' ? 'btn-primary' : 'btn-light'}`}
              >
                Dashboard
              </button>
            </li>
            <li className="nav-item">
              <button 
                onClick={() => setActiveTab('tenants')} 
                className={`btn w-100 text-start px-3 py-2 fw-semibold ${activeTab === 'tenants' ? 'btn-primary' : 'btn-light'}`}
              >
                Tenant Configs
              </button>
            </li>
            <li className="nav-item">
              <button 
                onClick={() => setActiveTab('health')} 
                className={`btn w-100 text-start px-3 py-2 fw-semibold ${activeTab === 'health' ? 'btn-primary' : 'btn-light'}`}
              >
                Backend Health
              </button>
            </li>
          </ul>
        </div>
        
        <div className="col-md-10 p-5">
          <div className="card shadow-sm p-4 border-0">
            <h1 className="h3 mb-3 fw-bold">System Status Overview</h1>
            <p className="text-secondary mb-4">Welcome to your SmartLB-AI routing control dashboard. This UI acts as the client control console (placeholder).</p>
            <div className="alert alert-info py-3 border-0 rounded-4">
              <strong>Initialization State:</strong> Project files are successfully outlined. Connect backend microservices to replace this placeholder.
            </div>
            
            <div className="row g-4 mt-2">
              <div className="col-md-4">
                <div className="card bg-primary text-white border-0 p-4 shadow-sm">
                  <h6>ACTIVE TENANTS</h6>
                  <h2 className="display-6 fw-bold">12</h2>
                </div>
              </div>
              <div className="col-md-4">
                <div className="card bg-success text-white border-0 p-4 shadow-sm">
                  <h6>INTELLIGENT ROUTING DECISIONS</h6>
                  <h2 className="display-6 fw-bold">99.8%</h2>
                </div>
              </div>
              <div className="col-md-4">
                <div className="card bg-warning text-dark border-0 p-4 shadow-sm">
                  <h6>BACKEND SERVER COUNT</h6>
                  <h2 className="display-6 fw-bold">5 / 5</h2>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;