import React, { useEffect, useState } from 'react'
import { Table, Tag, Button, Space, Modal, Form, Input, Select, message, Drawer, Timeline, Descriptions } from 'antd'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../auth'
import { ALERT_TYPE, ALERT_STATUS, ALERT_LEVEL, DISEASE_TYPE, STAGE } from '../utils'

/** 预警随访中心：护士接单随访 / 医生处理 / 治疗师查看 */
export default function Alerts() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [list, setList] = useState([])
  const [statusFilter, setStatusFilter] = useState()
  const [followupTarget, setFollowupTarget] = useState(null)
  const [opinionTarget, setOpinionTarget] = useState(null)
  const [drawerAlert, setDrawerAlert] = useState(null)
  const [followups, setFollowups] = useState([])
  const [form] = Form.useForm()
  const [opinionForm] = Form.useForm()

  const load = () => {
    const qs = statusFilter ? `?status=${statusFilter}` : ''
    api.get(`/alerts${qs}`).then((res) => setList(res.data))
  }
  useEffect(() => { load() }, [statusFilter])

  const openDrawer = (alert) => {
    setDrawerAlert(alert)
    api.get(`/alerts/${alert.id}/followups`).then((res) => setFollowups(res.data))
  }

  const assign = async (alert) => {
    await api.post(`/alerts/${alert.id}/assign`)
    message.success('已接单，开始随访')
    load()
  }

  const submitFollowup = async () => {
    const values = await form.validateFields()
    await api.post(`/alerts/${followupTarget.id}/followups`, values)
    message.success('随访记录已保存并写入患者时间线')
    setFollowupTarget(null)
    form.resetFields()
    load()
  }

  const escalate = async (alert) => {
    await api.post(`/alerts/${alert.id}/escalate`)
    message.success('已转医生处理')
    load()
  }

  const submitOpinion = async () => {
    const values = await opinionForm.validateFields()
    await api.post(`/alerts/${opinionTarget.id}/doctor-opinion`, values)
    message.success('医生意见已记录')
    setOpinionTarget(null)
    opinionForm.resetFields()
    load()
  }

  const resolve = async (alert) => {
    await api.post(`/alerts/${alert.id}/resolve`)
    message.success('预警已解除')
    load()
  }

  const isNurse = ['NURSE', 'ADMIN'].includes(user.role)
  const isDoctor = ['DOCTOR', 'ADMIN'].includes(user.role)
  const isStaff = isNurse || isDoctor || user.role === 'THERAPIST'

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Select
          placeholder="按状态筛选" allowClear style={{ width: 160 }} onChange={setStatusFilter}
          options={Object.entries(ALERT_STATUS).map(([k, v]) => ({ value: k, label: v.label }))}
        />
        <Button onClick={() => api.post('/alerts/scan-missed').then(() => { message.success('漏练扫描完成'); load() })}>
          手动扫描漏练
        </Button>
      </Space>
      <Table
        rowKey="id"
        dataSource={list}
        pagination={{ pageSize: 12 }}
        columns={[
          { title: '时间', dataIndex: 'createdAt', width: 150 },
          {
            title: '患者', width: 160,
            render: (_, r) => (
              <a onClick={() => navigate(`/patients/${r.patient.id}`)}>
                {r.patient.name}（{DISEASE_TYPE[r.patient.diseaseType]}）
              </a>
            )
          },
          { title: '阶段', width: 100, render: (_, r) => <Tag color={STAGE[r.patient.diseaseStage]?.color}>{STAGE[r.patient.diseaseStage]?.label}</Tag> },
          { title: '预警类型', dataIndex: 'type', width: 120, render: (v) => <Tag color="volcano">{ALERT_TYPE[v]}</Tag> },
          { title: '级别', dataIndex: 'level', width: 70, render: (v) => <Tag color={ALERT_LEVEL[v]?.color}>{ALERT_LEVEL[v]?.label}</Tag> },
          { title: '状态', dataIndex: 'status', width: 90, render: (v) => <Tag color={ALERT_STATUS[v]?.color}>{ALERT_STATUS[v]?.label}</Tag> },
          { title: '内容', dataIndex: 'message', ellipsis: true },
          { title: '护士', width: 80, render: (_, r) => r.nurse?.name || '-' },
          {
            title: '操作', width: 260, fixed: 'right',
            render: (_, r) => (
              <Space size={4} wrap>
                <Button size="small" onClick={() => openDrawer(r)}>详情</Button>
                {isNurse && r.status === 'PENDING' && <Button size="small" type="primary" onClick={() => assign(r)}>接单</Button>}
                {isNurse && ['PENDING', 'FOLLOWING'].includes(r.status) && (
                  <Button size="small" onClick={() => setFollowupTarget(r)}>电话随访</Button>
                )}
                {isStaff && ['PENDING', 'FOLLOWING'].includes(r.status) && (
                  <Button size="small" danger onClick={() => escalate(r)}>转医生</Button>
                )}
                {isDoctor && r.status === 'ESCALATED' && (
                  <Button size="small" type="primary" onClick={() => setOpinionTarget(r)}>填写意见</Button>
                )}
                {isStaff && r.status !== 'RESOLVED' && (
                  <Button size="small" onClick={() => resolve(r)}>解除</Button>
                )}
              </Space>
            )
          }
        ]}
      />

      {/* 电话随访 */}
      <Modal title={`电话随访（${followupTarget?.patient?.name} · ${ALERT_TYPE[followupTarget?.type]}）`}
        open={!!followupTarget} onOk={submitFollowup} onCancel={() => setFollowupTarget(null)} okText="保存随访记录">
        <Form form={form} layout="vertical" initialValues={{ method: '电话' }}>
          <Form.Item name="method" label="随访方式">
            <Select options={['电话', '视频', '上门'].map((v) => ({ value: v, label: v }))} />
          </Form.Item>
          <Form.Item name="content" label="随访内容" rules={[{ required: true, message: '请填写随访内容' }]}>
            <Input.TextArea rows={3} placeholder="询问了什么情况、患者/家属如何反馈" />
          </Form.Item>
          <Form.Item name="outcome" label="随访结果" rules={[{ required: true, message: '请选择随访结果' }]}>
            <Select options={['已恢复训练', '建议降低强度', '建议线下复诊', '需医生介入', '无法接通'].map((v) => ({ value: v, label: v }))} />
          </Form.Item>
          <Form.Item name="nextAction" label="后续安排"><Input.TextArea rows={2} /></Form.Item>
        </Form>
      </Modal>

      {/* 医生意见 */}
      <Modal title={`医生处理意见（${opinionTarget?.patient?.name}）`}
        open={!!opinionTarget} onOk={submitOpinion} onCancel={() => setOpinionTarget(null)} okText="保存意见">
        <Form form={opinionForm} layout="vertical">
          <Form.Item name="opinion" label="处理意见" rules={[{ required: true, message: '请填写处理意见' }]}>
            <Input.TextArea rows={4} placeholder="诊断意见、处理建议、是否调整训练/用药、复诊安排" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 预警详情抽屉 */}
      <Drawer title="预警详情与随访记录" open={!!drawerAlert} onClose={() => setDrawerAlert(null)} width={480}>
        {drawerAlert && (
          <div>
            <Descriptions size="small" column={1} bordered>
              <Descriptions.Item label="患者">{drawerAlert.patient.name}</Descriptions.Item>
              <Descriptions.Item label="类型">{ALERT_TYPE[drawerAlert.type]}</Descriptions.Item>
              <Descriptions.Item label="级别">{ALERT_LEVEL[drawerAlert.level]?.label}</Descriptions.Item>
              <Descriptions.Item label="状态">{ALERT_STATUS[drawerAlert.status]?.label}</Descriptions.Item>
              <Descriptions.Item label="内容">{drawerAlert.message}</Descriptions.Item>
              <Descriptions.Item label="触发时间">{drawerAlert.createdAt}</Descriptions.Item>
              <Descriptions.Item label="负责护士">{drawerAlert.nurse?.name || '未接单'}</Descriptions.Item>
            </Descriptions>
            <h4 style={{ marginTop: 16 }}>随访记录</h4>
            {followups.length === 0 ? <div style={{ color: '#999' }}>暂无随访记录</div> : (
              <Timeline
                items={followups.map((f) => ({
                  children: (
                    <div>
                      <div><Tag color="gold">{f.method}</Tag><b>{f.outcome}</b></div>
                      <div style={{ fontSize: 13, color: '#666', marginTop: 4 }}>{f.content}</div>
                      {f.nextAction && <div style={{ fontSize: 13, color: '#1677ff', marginTop: 2 }}>后续：{f.nextAction}</div>}
                      <div style={{ fontSize: 12, color: '#999', marginTop: 2 }}>{f.followTime} ｜ {f.nurse?.name}</div>
                    </div>
                  )
                }))}
              />
            )}
          </div>
        )}
      </Drawer>
    </div>
  )
}
