import React, { useState } from 'react'
import { Card, Form, Input, Button, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)

  const onFinish = async (values) => {
    setLoading(true)
    try {
      const user = await login(values.username, values.password)
      message.success(`欢迎，${user.name}`)
      navigate('/', { replace: true })
    } catch {
      // 拦截器已提示
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-wrap">
      <Card className="login-card">
        <h2 className="login-title">县城康复中心</h2>
        <div className="login-sub">居家训练处方与复诊评估平台</div>
        <Form onFinish={onFinish} size="large">
          <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>登 录</Button>
        </Form>
        <div className="demo-accounts" style={{ marginTop: 16 }}>
          <div><b>演示账号</b>（密码均为 123456）</div>
          <div>治疗师：therapist / therapist2　护士：nurse</div>
          <div>医生：doctor　家属：family ~ family5　管理员：admin</div>
        </div>
      </Card>
    </div>
  )
}
