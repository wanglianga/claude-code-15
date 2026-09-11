import React, { useEffect, useState } from 'react'
import { Table, Tag, Button, Space, Modal, Form, Input, Select, message, Drawer, Descriptions, Radio, Checkbox, DatePicker, Timeline } from 'antd'
import { useNavigate } from 'react-router-dom'
import dayjs from 'dayjs'
import api from '../api'
import { useAuth } from '../auth'
import { ESCALATION_STATUS, ESCALATION_TRIGGER, DISPOSITION, DISEASE_TYPE } from '../utils'

/** 疼痛升级处置中心：家属补充 → 护士电话评估 → 医生复核处置 → 风险解除 */
export default function Escalations() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [list, setList] = useState([])
  const [statusFilter, setStatusFilter] = useState()
  const [detail, setDetail] = useState(null)
  const [reportTarget, setReportTarget] = useState(null)
  const [assessTarget, setAssessTarget] = useState(null)
  const [disposeTarget, setDisposeTarget] = useState(null)
  const [clearTarget, setClearTarget] = useState(null)
  const [reportForm] = Form.useForm()
  const [assessForm] = Form.useForm()
  const [disposeForm] = Form.useForm()
  const [clearForm] = Form.useForm()

  const load = () => {
    const qs = statusFilter ? `?status=${statusFilter}` : ''
    api.get(`/escalations${qs}`).then((res) => setList(res.data))
  }
  useEffect(() => { load() }, [statusFilter])

  const isNurse = ['NURSE', 'ADMIN'].includes(user.role)
  const isDoctor = ['DOCTOR', 'ADMIN'].includes(user.role)

  const submitReport = async () => {
    const values = await reportForm.validateFields()
    await api.post(`/escalations/${reportTarget.id}/family-report`, values)
    message.success('家属症状信息已代登记，进入护士电话评估')
    setReportTarget(null)
    reportForm.resetFields()
    load()
  }

  const submitAssess = async () => {
    const values = await assessForm.validateFields()
    await api.post(`/escalations/${assessTarget.id}/nurse-assessment`, values)
    message.success(values.decision === 'ESCALATE_DOCTOR' ? '已转医生复核，动作保持暂停' : '评估完成：风险解除，动作已恢复')
    setAssessTarget(null)
    assessForm.resetFields()
    load()
  }

  const submitDispose = async () => {
    const values = await disposeForm.validateFields()
    await api.post(`/escalations/${disposeTarget.id}/doctor-disposition`, {
      dispositions: values.dispositions,
      conclusion: values.conclusion,
      reviewDate: values.reviewDate?.format('YYYY-MM-DD')
    })
    message.success('医生处置结论已记录，并同步治疗师与家属')
    setDisposeTarget(null)
    disposeForm.resetFields()
    load()
  }

  const submitClear = async () => {
    const values = await clearForm.validateFields()
    await api.post(`/escalations/${clearTarget.id}/clear`, values)
    message.success('风险已解除，相关动作恢复每日训练任务')
    setClearTarget(null)
    clearForm.resetFields()
    load()
  }

  const triggerTags = (r) => (r.triggers || '').split(',').filter(Boolean).map((t) => (
    <Tag key={t} color="red">{ESCALATION_TRIGGER[t] || t}</Tag>
  ))

  const columns = [
    { title: '触发时间', dataIndex: 'createdAt', width: 150, render: (v) => v?.slice(0, 16) },
    {
      title: '患者', width: 150,
      render: (_, r) => <a onClick={() => navigate(`/patients/${r.patient.id}`)}>{r.patient.name}（{DISEASE_TYPE[r.patient.diseaseType]}）</a>
    },
    { title: '触发原因', width: 190, render: (_, r) => <Space size={2} wrap>{triggerTags(r)}{r.painScore != null && <Tag color="volcano">疼痛 {r.painScore} 分</Tag>}</Space> },
    { title: '暂停动作', dataIndex: 'suspendedItemNames', width: 180, render: (v) => <b style={{ color: '#cf1322' }}>{v}</b> },
    { title: '状态', dataIndex: 'status', width: 140, render: (v) => <Tag color={ESCALATION_STATUS[v]?.color}>{ESCALATION_STATUS[v]?.label}</Tag> },
    {
      title: '家属补充', width: 110,
      render: (_, r) => r.familyReportedAt
        ? (r.familyFell ? <Tag color="red">有摔倒！</Tag> : <Tag color="green">已补充</Tag>)
        : <Tag color="orange">待补充</Tag>
    },
    { title: '医生处置', width: 130, render: (_, r) => r.doctorDispositions
        ? <Space size={2} wrap>{r.doctorDispositions.split(',').map((d) => <Tag key={d} color="purple">{DISPOSITION[d] || d}</Tag>)}</Space>
        : '-' },
    {
      title: '操作', width: 250, fixed: 'right',
      render: (_, r) => (
        <Space size={4} wrap>
          <Button size="small" onClick={() => setDetail(r)}>详情</Button>
          {isNurse && r.status === 'PENDING_FAMILY_INFO' && (
            <Button size="small" onClick={() => setReportTarget(r)}>代登记症状</Button>
          )}
          {isNurse && r.status === 'NURSE_ASSESSING' && (
            <Button size="small" type="primary" onClick={() => setAssessTarget(r)}>电话评估</Button>
          )}
          {isDoctor && r.status === 'DOCTOR_REVIEW' && (
            <Button size="small" type="primary" onClick={() => setDisposeTarget(r)}>医生处置</Button>
          )}
          {isDoctor && r.status === 'DISPOSITION_ACTIVE' && (
            <Button size="small" type="primary" ghost onClick={() => setClearTarget(r)}>解除风险</Button>
          )}
        </Space>
      )
    }
  ]

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Select
          placeholder="按状态筛选" allowClear style={{ width: 190 }} onChange={setStatusFilter}
          options={Object.entries(ESCALATION_STATUS).map(([k, v]) => ({ value: k, label: v.label }))}
        />
        <span style={{ color: '#888', fontSize: 13 }}>流程：平台暂停动作 → 家属补充 → 护士电话评估 → 医生处置（休息/冰敷/影像/复诊）→ 解除风险恢复训练</span>
      </Space>
      <Table rowKey="id" dataSource={list} columns={columns} pagination={{ pageSize: 12 }} />

      {/* 护士代登记家属症状 */}
      <Modal title={`代登记家属症状（${reportTarget?.patient?.name}）`}
        open={!!reportTarget} onOk={submitReport} onCancel={() => setReportTarget(null)} okText="保存并转护士评估">
        <Form form={reportForm} layout="vertical" initialValues={{ fell: false }}>
          <Form.Item name="symptoms" label="症状表现" rules={[{ required: true, message: '请填写症状' }]}>
            <Input.TextArea rows={2} placeholder="肿胀/麻木部位、疼痛规律、是否影响睡眠" />
          </Form.Item>
          <Form.Item name="medication" label="用药情况" rules={[{ required: true, message: '请填写用药情况' }]}>
            <Input.TextArea rows={2} placeholder="止痛药/外用药名称与剂量；未用药请填“未用药”" />
          </Form.Item>
          <Form.Item noStyle shouldUpdate={(a, b) => a.fell !== b.fell}>
            {({ getFieldValue }) => (
              <>
                <Form.Item name="fell" label="是否摔倒" rules={[{ required: true, message: '请选择' }]}>
                  <Radio.Group>
                    <Radio value={false}>没有摔倒</Radio>
                    <Radio value={true}>有摔倒</Radio>
                  </Radio.Group>
                </Form.Item>
                {getFieldValue('fell') === true && (
                  <Form.Item name="fellDetail" label="摔倒经过" rules={[{ required: true, message: '请补充摔倒经过' }]}>
                    <Input.TextArea rows={2} placeholder="时间、部位、是否磕碰头部、能否自行站起" />
                  </Form.Item>
                )}
              </>
            )}
          </Form.Item>
        </Form>
      </Modal>

      {/* 护士电话评估 */}
      <Modal title={`护士电话评估（${assessTarget?.patient?.name} · 暂停「${assessTarget?.suspendedItemNames}」）`}
        open={!!assessTarget} onOk={submitAssess} onCancel={() => setAssessTarget(null)} okText="保存评估结论" width={560}>
        {assessTarget && (
          <Descriptions size="small" column={1} bordered style={{ marginBottom: 12 }}>
            <Descriptions.Item label="家属补充症状">{assessTarget.familySymptoms}</Descriptions.Item>
            <Descriptions.Item label="用药">{assessTarget.familyMedication}</Descriptions.Item>
            <Descriptions.Item label="是否摔倒">
              {assessTarget.familyFell ? <span style={{ color: '#cf1322' }}>是：{assessTarget.familyFellDetail}</span> : '否'}
            </Descriptions.Item>
          </Descriptions>
        )}
        <Form form={assessForm} layout="vertical">
          <Form.Item name="content" label="电话评估内容" rules={[{ required: true, message: '请填写评估内容' }]}>
            <Input.TextArea rows={3} placeholder="电话询问情况、症状变化、初步判断" />
          </Form.Item>
          <Form.Item name="decision" label="评估结论" rules={[{ required: true, message: '请选择结论' }]}>
            <Radio.Group>
              <Radio value="OBSERVE_RESUME">风险可控：继续观察，恢复训练（解除暂停）</Radio>
              <Radio value="ESCALATE_DOCTOR">需医生介入：转医生复核（保持暂停）</Radio>
            </Radio.Group>
          </Form.Item>
        </Form>
      </Modal>

      {/* 医生处置 */}
      <Modal title={`医生复核处置（${disposeTarget?.patient?.name} · 暂停「${disposeTarget?.suspendedItemNames}」）`}
        open={!!disposeTarget} onOk={submitDispose} onCancel={() => setDisposeTarget(null)} okText="确认处置并同步家属/治疗师" width={600}>
        {disposeTarget && (
          <Descriptions size="small" column={1} bordered style={{ marginBottom: 12 }}>
            <Descriptions.Item label="家属补充症状">{disposeTarget.familySymptoms}</Descriptions.Item>
            <Descriptions.Item label="用药">{disposeTarget.familyMedication}</Descriptions.Item>
            <Descriptions.Item label="是否摔倒">
              {disposeTarget.familyFell ? <span style={{ color: '#cf1322' }}>是：{disposeTarget.familyFellDetail}</span> : '否'}
            </Descriptions.Item>
            <Descriptions.Item label="护士评估">{disposeTarget.nurseAssessment}</Descriptions.Item>
          </Descriptions>
        )}
        <Form form={disposeForm} layout="vertical">
          <Form.Item name="dispositions" label="处置方式（处方随之调整）" rules={[{ required: true, message: '请至少选择一种处置方式' }]}>
            <Checkbox.Group
              style={{ display: 'flex', gap: 16 }}
              options={Object.entries(DISPOSITION).map(([k, v]) => ({ value: k, label: v }))}
            />
          </Form.Item>
          <Form.Item noStyle shouldUpdate={(a, b) => a.dispositions !== b.dispositions}>
            {({ getFieldValue }) => {
              const ds = getFieldValue('dispositions') || []
              return (ds.includes('OUTPATIENT') || ds.includes('IMAGING')) && (
                <Form.Item name="reviewDate" label="复诊/检查日期（将同步患者复诊计划）" rules={[{ required: true, message: '请选择日期' }]}>
                  <DatePicker style={{ width: '100%' }} minDate={dayjs()} />
                </Form.Item>
              )
            }}
          </Form.Item>
          <Form.Item name="conclusion" label="医生介入结论（同步治疗师与家属）" rules={[{ required: true, message: '请填写结论' }]}>
            <Input.TextArea rows={3} placeholder="诊断判断、处置说明、暂停时长、何时复评解除" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 解除风险 */}
      <Modal title={`解除风险（${clearTarget?.patient?.name} · 恢复「${clearTarget?.suspendedItemNames}」）`}
        open={!!clearTarget} onOk={submitClear} onCancel={() => setClearTarget(null)} okText="确认解除，恢复训练">
        <Form form={clearForm} layout="vertical">
          <Form.Item name="note" label="解除说明">
            <Input.TextArea rows={3} placeholder="如：复评疼痛降至 2 分，肿胀消退，恢复训练并注意观察" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 详情抽屉 */}
      <Drawer title="疼痛升级处置详情" open={!!detail} onClose={() => setDetail(null)} width={520}>
        {detail && (
          <div>
            <Descriptions size="small" column={1} bordered>
              <Descriptions.Item label="患者">{detail.patient.name}</Descriptions.Item>
              <Descriptions.Item label="状态"><Tag color={ESCALATION_STATUS[detail.status]?.color}>{ESCALATION_STATUS[detail.status]?.label}</Tag></Descriptions.Item>
              <Descriptions.Item label="触发原因"><Space size={2} wrap>{triggerTags(detail)}</Space>{detail.painScore != null && `（疼痛 ${detail.painScore} 分）`}</Descriptions.Item>
              <Descriptions.Item label="暂停动作"><b style={{ color: '#cf1322' }}>{detail.suspendedItemNames}</b></Descriptions.Item>
              <Descriptions.Item label="来源打卡">{detail.sourceLogDate || '-'}</Descriptions.Item>
              <Descriptions.Item label="触发时间">{detail.createdAt}</Descriptions.Item>
            </Descriptions>
            <h4 style={{ marginTop: 16 }}>处置经过</h4>
            <Timeline
              items={[
                {
                  color: 'red',
                  children: <>平台暂停相关动作，要求家属补充症状<div style={{ fontSize: 12, color: '#999' }}>{detail.createdAt}</div></>
                },
                detail.familyReportedAt && {
                  color: 'orange',
                  children: (
                    <>
                      <b>家属补充</b>（{detail.familyReportedBy}）
                      <div style={{ fontSize: 13 }}>症状：{detail.familySymptoms}</div>
                      <div style={{ fontSize: 13 }}>用药：{detail.familyMedication}</div>
                      <div style={{ fontSize: 13 }}>是否摔倒：{detail.familyFell ? <span style={{ color: '#cf1322' }}>是（{detail.familyFellDetail}）</span> : '否'}</div>
                      <div style={{ fontSize: 12, color: '#999' }}>{detail.familyReportedAt}</div>
                    </>
                  )
                },
                detail.nurseAssessedAt && {
                  color: 'gold',
                  children: (
                    <>
                      <b>护士电话评估</b>（{detail.nurse?.name}）
                      <div style={{ fontSize: 13 }}>{detail.nurseAssessment}</div>
                      <div style={{ fontSize: 12, color: '#999' }}>{detail.nurseAssessedAt}</div>
                    </>
                  )
                },
                detail.doctorReviewedAt && {
                  color: 'purple',
                  children: (
                    <>
                      <b>医生处置</b>（{detail.doctor?.name}）
                      <Space size={2} style={{ marginLeft: 4 }}>
                        {(detail.doctorDispositions || '').split(',').filter(Boolean).map((d) => <Tag key={d} color="purple">{DISPOSITION[d] || d}</Tag>)}
                      </Space>
                      <div style={{ fontSize: 13 }}>{detail.doctorConclusion}</div>
                      {detail.reviewDate && <div style={{ fontSize: 13 }}>复诊/检查日期：{detail.reviewDate}</div>}
                      <div style={{ fontSize: 12, color: '#999' }}>{detail.doctorReviewedAt}</div>
                    </>
                  )
                },
                detail.clearedAt && {
                  color: 'green',
                  children: (
                    <>
                      <b>风险解除，恢复训练</b>（{detail.clearedBy}）
                      {detail.clearNote && <div style={{ fontSize: 13 }}>{detail.clearNote}</div>}
                      <div style={{ fontSize: 12, color: '#999' }}>{detail.clearedAt}</div>
                    </>
                  )
                }
              ].filter(Boolean)}
            />
          </div>
        )}
      </Drawer>
    </div>
  )
}
