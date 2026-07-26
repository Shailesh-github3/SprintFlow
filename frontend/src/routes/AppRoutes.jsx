import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import AuthLayout from '../layouts/AuthLayout';
import MainLayout from '../layouts/MainLayout';
import ProtectedRoute from '../components/ProtectedRoute';
import Login from '../pages/Login';
import Register from '../pages/Register';
import Dashboard from '../pages/Dashboard';
import ProjectList from '../pages/ProjectList';
import ProjectDetails from '../pages/ProjectDetails';
import Subscription from '../pages/Subscription';
import Settings from '../pages/Settings';
import UpgradeSuccess from '../pages/UpgradeSuccess';

const AppRoutes = () => {
  return (
    <Routes>
      {/* Public/Auth Routes */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
      </Route>

      {/* Protected Routes - wrapped with ProtectedRoute for auth guard */}
      <Route element={<ProtectedRoute />}>
        <Route element={<MainLayout />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/projects" element={<ProjectList />} />
          <Route path="/projects/:projectId" element={<ProjectDetails />} />
          <Route path="/subscription" element={<Subscription />} />
          <Route path="/upgrade-plan/success" element={<UpgradeSuccess />} />
          <Route path="/settings" element={<Settings />} />
        </Route>
      </Route>
      
      {/* Fallback */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
};

export default AppRoutes;
