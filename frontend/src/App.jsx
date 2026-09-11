import React from 'react'
import { HashRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Spin } from 'antd'
import { AuthProvider, useAuth } from './auth'
import AppLayout from './components/AppLayout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Patients from './pages/Patients'
import PatientDetail from './pages/PatientDetail'
import Alerts from './pages/Alerts'
import Escalations from './pages/Escalations'
import RiskBoard from './pages/RiskBoard'
import ReviewWorkspace from './pages/ReviewWorkspace'
import FamilyToday from './pages/FamilyToday'

function Guard({ children, roles }) {
  const { user, loading } = useAuth()
  if (loading) return <div style={{ textAlign: 'center', padding: 100 }}><Spin size="large" /></div>
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />
  return children
}

function Home() {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (user.role === 'FAMILY') return <Navigate to="/today" replace />
  if (user.role === 'NURSE' || user.role === 'DOCTOR') return <Navigate to="/alerts" replace />
  return <Dashboard />
}

export default function App() {
  return (
    <AuthProvider>
      <HashRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/" element={<Guard><AppLayout /></Guard>}>
            <Route index element={<Home />} />
            <Route path="patients" element={<Guard roles={['THERAPIST', 'ADMIN', 'NURSE', 'DOCTOR']}><Patients /></Guard>} />
            <Route path="patients/:id" element={<Guard roles={['THERAPIST', 'ADMIN', 'NURSE', 'DOCTOR', 'FAMILY']}><PatientDetail /></Guard>} />
            <Route path="patients/:id/review" element={<Guard roles={['THERAPIST', 'ADMIN']}><ReviewWorkspace /></Guard>} />
            <Route path="alerts" element={<Guard roles={['NURSE', 'DOCTOR', 'THERAPIST', 'ADMIN']}><Alerts /></Guard>} />
            <Route path="escalations" element={<Guard roles={['NURSE', 'DOCTOR', 'THERAPIST', 'ADMIN']}><Escalations /></Guard>} />
            <Route path="risk-board" element={<Guard roles={['THERAPIST', 'NURSE', 'DOCTOR', 'ADMIN']}><RiskBoard /></Guard>} />
            <Route path="today" element={<Guard roles={['FAMILY']}><FamilyToday /></Guard>} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </HashRouter>
    </AuthProvider>
  )
}
