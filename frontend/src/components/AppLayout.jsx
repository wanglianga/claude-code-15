import React from 'react'
import { Layout, Menu, Dropdown, Tag } from 'antd'
import {
  DashboardOutlined, TeamOutlined, AlertOutlined, FundOutlined,
  ScheduleOutlined, LogoutOutlined, UserOutlined
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth'

const { Sider, Header, Content } = Layout

const ROLE_LABEL = {
  ADMIN: '系统管理员', THERAPIST: '康复治疗师', NURSE: '康复护士',
  DOCTOR: '医生', FAMILY: '患者/家属'
}

const ROLE_COLOR = { ADMIN: 'purple', THERAPIST: 'blue', NURSE: 'gold', DOCTOR: 'magenta', FAMILY: 'green' }

export default function AppLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const menus = []
  if (['THERAPIST', 'ADMIN'].includes(user.role)) {
    menus.push({ key: '/', icon: <DashboardOutlined />, label: '工作台' })
  }
  if (['THERAPIST', 'ADMIN', 'NURSE', 'DOCTOR'].includes(user.role)) {
    menus.push({ key: '/patients', icon: <TeamOutlined />, label: '患者管理' })
    menus.push({ key: '/alerts', icon: <AlertOutlined />, label: user.role === 'DOCTOR' ? '介入处理' : '预警随访' })
    menus.push({ key: '/risk-board', icon: <FundOutlined />, label: '风险看板' })
  }
  if (user.role === 'FAMILY') {
    menus.push({ key: '/today', icon: <ScheduleOutlined />, label: '今日训练' })
  }

  const selected = '/' + (location.pathname.split('/')[1] || '')

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="dark" width={210}>
        <div className="app-logo">
          县城康复中心
          <small>居家训练处方与复诊评估平台</small>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selected === '/' && location.pathname !== '/' ? location.pathname : selected]}
          items={menus}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 20px', display: 'flex', justifyContent: 'flex-end', alignItems: 'center', boxShadow: '0 1px 4px rgba(0,21,41,.08)', zIndex: 1 }}>
          <Dropdown menu={{
            items: [{ key: 'logout', icon: <LogoutOutlined />, label: '退出登录', onClick: logout }]
          }}>
            <span style={{ cursor: 'pointer' }}>
              <UserOutlined style={{ marginRight: 8 }} />
              {user.name}
              <Tag color={ROLE_COLOR[user.role]} style={{ marginLeft: 8 }}>{ROLE_LABEL[user.role]}</Tag>
              {user.title && <span style={{ color: '#999', fontSize: 12 }}>{user.title}</span>}
            </span>
          </Dropdown>
        </Header>
        <Content className="page-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
