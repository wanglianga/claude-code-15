import React, { useEffect, useState } from 'react'
import { Table, Button, Tag, Input, Select, Space, Modal, Form, DatePicker, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import dayjs from 'dayjs'
import api from '../api'
import { DISEASE_TYPE, STAGE, age } from '../utils'
import { useAuth } from '../auth'

export default function Patients() {
  const [patients, setPatients] = useState([])
  const [loading, setLoading] = useState(true)
  const [keyword, setKeyword] = useState('')
  const [stageFilter, setStageFilter] = useState()
  const [typeFilter, setTypeFilter] = useState()
  const [modalOpen, setModalOpen] = useState(false)
  const [familyUsers, setFamilyUsers] = useState([])
  const [form] = Form.useForm()
  const navigate = useNavigate()
  const { user } = useAuth()
  const canEdit = ['THERAPIST', 'ADMIN'].includes(user.role)

  const load = () => {
    setLoading(true)
    api.get('/patients').then((res) => setPatients(res.data)).finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
    if (canEdit) api.get('/users?role=FAMILY').then((res) => setFamilyUsers(res.data)).catch(() => {})
  }, [])

  const filtered = patients.filter((p) => {
    if (keyword && !p.name.includes(keyword) && !p.patientNo.includes(keyword)) return false
    if (stageFilter && p.diseaseStage !== stageFilter) return false
    if (typeFilter && p.diseaseType !== typeFilter) return false
    return true
  })

  const create = async () => {
    const values = await form.validateFields()
    const payload = {
      ...values,
      birthDate: values.birthDate?.format('YYYY-MM-DD'),
      dischargeDate: values.dischargeDate?.format('YYYY-MM-DD'),
      nextReviewDate: values.nextReviewDate?.format('YYYY-MM-DD')
    }
    await api.post('/patients', payload)
    message.success('患者档案已建立')
    setModalOpen(false)
    form.resetFields()
    load()
  }

  return (
    <div>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search placeholder="姓名 / 档案号" allowClear style={{ width: 200 }} onSearch={setKeyword} onChange={(e) => !e.target.value && setKeyword('')} />
        <Select placeholder="疾病阶段" allowClear style={{ width: 140 }} onChange={setStageFilter}
          options={Object.entries(STAGE).map(([k, v]) => ({ value: k, label: v.label }))} />
        <Select placeholder="疾病类型" allowClear style={{ width: 160 }} onChange={setTypeFilter}
          options={Object.entries(DISEASE_TYPE).map(([k, v]) => ({ value: k, label: v }))} />
        {canEdit && <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>新建患者档案</Button>}
      </Space>

      <Table
        rowKey="id"
        loading={loading}
        dataSource={filtered}
        pagination={{ pageSize: 10 }}
        onRow={(record) => ({ onClick: () => navigate(`/patients/${record.id}`), style: { cursor: 'pointer' } })}
        columns={[
          { title: '档案号', dataIndex: 'patientNo', width: 110 },
          { title: '姓名', dataIndex: 'name', width: 100, render: (v) => <b>{v}</b> },
          { title: '性别/年龄', width: 100, render: (_, r) => `${r.gender || '-'} / ${age(r.birthDate)}` },
          { title: '疾病类型', dataIndex: 'diseaseType', width: 120, render: (v) => DISEASE_TYPE[v] || v },
          {
            title: '疾病阶段', dataIndex: 'diseaseStage', width: 110,
            render: (v) => <Tag color={STAGE[v]?.color}>{STAGE[v]?.label || v}</Tag>
          },
          { title: '诊断', dataIndex: 'diagnosis', ellipsis: true },
          { title: '照护人', width: 140, render: (_, r) => r.caregiverName ? `${r.caregiverName}（${r.caregiverRelation || '-'}）` : '-' },
          { title: '负责治疗师', width: 100, render: (_, r) => r.therapist?.name || '-' },
          { title: '下次复诊', dataIndex: 'nextReviewDate', width: 110, render: (v) => v || '-' }
        ]}
      />

      <Modal title="新建患者档案" open={modalOpen} onOk={create} onCancel={() => setModalOpen(false)} width={720} okText="保存">
        <Form form={form} layout="vertical" initialValues={{ diseaseStage: 'NEWLY_DISCHARGED', insuranceType: '城乡居民医保' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0 16px' }}>
            <Form.Item name="name" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}><Input /></Form.Item>
            <Form.Item name="gender" label="性别"><Select options={[{ value: '男' }, { value: '女' }]} /></Form.Item>
            <Form.Item name="birthDate" label="出生日期"><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="diseaseType" label="疾病类型" rules={[{ required: true, message: '请选择疾病类型' }]}>
              <Select options={Object.entries(DISEASE_TYPE).map(([k, v]) => ({ value: k, label: v }))} />
            </Form.Item>
            <Form.Item name="diseaseStage" label="疾病阶段">
              <Select options={Object.entries(STAGE).map(([k, v]) => ({ value: k, label: v.label }))} />
            </Form.Item>
            <Form.Item name="phone" label="联系电话"><Input /></Form.Item>
            <Form.Item name="dischargeDate" label="出院日期"><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="nextReviewDate" label="计划复诊日期"><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="familyUserId" label="绑定家属账号">
              <Select allowClear options={familyUsers.map((u) => ({ value: u.id, label: `${u.name}（${u.username}）` }))} />
            </Form.Item>
          </div>
          <Form.Item name="diagnosis" label="诊断"><Input.TextArea rows={2} placeholder="如：左侧基底节区脑梗死，右侧肢体偏瘫" /></Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0 16px' }}>
            <Form.Item name="caregiverName" label="家庭照护人姓名"><Input /></Form.Item>
            <Form.Item name="caregiverRelation" label="与患者关系"><Input /></Form.Item>
            <Form.Item name="caregiverPhone" label="照护人电话"><Input /></Form.Item>
            <Form.Item name="insuranceType" label="医保类型">
              <Select options={['职工医保', '城乡居民医保', '新农合', '自费'].map((v) => ({ value: v, label: v }))} />
            </Form.Item>
            <Form.Item name="insuranceNo" label="医保号"><Input /></Form.Item>
            <Form.Item name="address" label="住址"><Input /></Form.Item>
          </div>
        </Form>
      </Modal>
    </div>
  )
}
