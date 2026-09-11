import React, { useEffect, useState } from 'react'
import {
  Tabs, Descriptions, Tag, Button, Card, Table, Modal, Form, Input, InputNumber,
  Select, DatePicker, Switch, Space, Statistic, Row, Col, message, Popconfirm, Empty, List
} from 'antd'
import { PlusOutlined, MinusCircleOutlined, ToolOutlined } from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import dayjs from 'dayjs'
import api from '../api'
import { useAuth } from '../auth'
import PainChart from '../components/PainChart'
import TimelineView from '../components/TimelineView'
import MediaView from '../components/MediaView'
import FileUpload from '../components/FileUpload'
import { DISEASE_TYPE, STAGE, RISK, DECISION, age, parseJson, fileUrl } from '../utils'

const INSURANCE_TYPES = ['职工医保', '城乡居民医保', '新农合', '自费']

export default function PatientDetail() {
  const { id } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()
  const canEdit = ['THERAPIST', 'ADMIN'].includes(user.role)

  const [patient, setPatient] = useState(null)
  const [risk, setRisk] = useState(null)

  const reloadPatient = () => {
    api.get(`/patients/${id}`).then((res) => setPatient(res.data))
    api.get(`/patients/${id}/risk`).then((res) => setRisk(res.data)).catch(() => {})
  }
  useEffect(reloadPatient, [id])

  if (!patient) return null

  return (
    <div>
      <Card
        title={
          <Space>
            <span style={{ fontSize: 18 }}>{patient.name}</span>
            <Tag>{patient.patientNo}</Tag>
            <Tag color="blue">{DISEASE_TYPE[patient.diseaseType]}</Tag>
            <Tag color={STAGE[patient.diseaseStage]?.color}>{STAGE[patient.diseaseStage]?.label}</Tag>
            {risk && <Tag color={RISK[risk.level]?.color}>{RISK[risk.level]?.label}</Tag>}
          </Space>
        }
        extra={canEdit && (
          <Button type="primary" onClick={() => navigate(`/patients/${id}/review`)}>复诊工作台</Button>
        )}
      >
        {risk && risk.reasons.length > 0 && (
          <div style={{ marginBottom: 8, color: '#cf1322', fontSize: 13 }}>
            风险提示：{risk.reasons.join('；')}
          </div>
        )}
        <Tabs
          items={[
            { key: 'profile', label: '档案信息', children: <ProfileTab patient={patient} risk={risk} canEdit={canEdit} onChanged={reloadPatient} /> },
            { key: 'assessments', label: '评估记录', children: <AssessmentTab patientId={id} canEdit={canEdit} /> },
            { key: 'prescriptions', label: '处方管理', children: <PrescriptionTab patientId={id} canEdit={canEdit} /> },
            { key: 'logs', label: '训练反馈', children: <LogsTab patientId={id} canEdit={canEdit} /> },
            { key: 'timeline', label: '患者时间线', children: <TimelineTab patientId={id} /> },
            { key: 'insurance', label: '医保结算', children: <InsuranceTab patientId={id} canEdit={canEdit} insuranceType={patient.insuranceType} /> }
          ]}
        />
      </Card>
    </div>
  )
}

/* ---------------- 档案 ---------------- */
function ProfileTab({ patient, risk, canEdit, onChanged }) {
  const [stageModal, setStageModal] = useState(false)
  const [form] = Form.useForm()

  const changeStage = async () => {
    const values = await form.validateFields()
    await api.put(`/patients/${patient.id}/stage`, values)
    message.success('疾病阶段已调整')
    setStageModal(false)
    onChanged()
  }

  return (
    <Row gutter={24}>
      <Col span={16}>
        <Descriptions bordered size="small" column={2}>
          <Descriptions.Item label="姓名">{patient.name}</Descriptions.Item>
          <Descriptions.Item label="性别/年龄">{patient.gender} / {age(patient.birthDate)}</Descriptions.Item>
          <Descriptions.Item label="联系电话">{patient.phone || '-'}</Descriptions.Item>
          <Descriptions.Item label="出院日期">{patient.dischargeDate || '-'}</Descriptions.Item>
          <Descriptions.Item label="诊断" span={2}>{patient.diagnosis}</Descriptions.Item>
          <Descriptions.Item label="家庭照护人">{patient.caregiverName}（{patient.caregiverRelation}）</Descriptions.Item>
          <Descriptions.Item label="照护人电话">{patient.caregiverPhone}</Descriptions.Item>
          <Descriptions.Item label="住址" span={2}>{patient.address || '-'}</Descriptions.Item>
          <Descriptions.Item label="医保类型">{patient.insuranceType}</Descriptions.Item>
          <Descriptions.Item label="医保号">{patient.insuranceNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="负责治疗师">{patient.therapist?.name}</Descriptions.Item>
          <Descriptions.Item label="家属账号">{patient.familyUser ? `${patient.familyUser.name}（${patient.familyUser.username}）` : '未绑定'}</Descriptions.Item>
          <Descriptions.Item label="下次复诊">{patient.nextReviewDate || '-'}</Descriptions.Item>
          <Descriptions.Item label="建档时间">{patient.createdAt}</Descriptions.Item>
        </Descriptions>
        {canEdit && (
          <Button style={{ marginTop: 12 }} onClick={() => { form.setFieldsValue({ stage: patient.diseaseStage }); setStageModal(true) }}>
            调整疾病阶段
          </Button>
        )}
      </Col>
      <Col span={8}>
        <Card size="small" title="当前风险">
          {risk && (
            <>
              <Statistic title="风险等级" value={RISK[risk.level]?.label} valueStyle={{ color: risk.level === 'HIGH' ? '#cf1322' : risk.level === 'MEDIUM' ? '#fa8c16' : '#52c41a', fontSize: 22 }} />
              <div style={{ fontSize: 13, color: '#666', marginTop: 8 }}>近 7 天依从性：{risk.adherence7d}% ｜ 连续漏练：{risk.missedDays} 天 ｜ 未处理预警：{risk.openAlerts} 条</div>
              {risk.reasons.map((r, i) => <div key={i} style={{ fontSize: 13, color: '#cf1322', marginTop: 4 }}>· {r}</div>)}
            </>
          )}
        </Card>
      </Col>
      <Modal title="调整疾病阶段" open={stageModal} onOk={changeStage} onCancel={() => setStageModal(false)} okText="保存">
        <Form form={form} layout="vertical">
          <Form.Item name="stage" label="疾病阶段" rules={[{ required: true }]}>
            <Select options={Object.entries(STAGE).map(([k, v]) => ({ value: k, label: `${v.label}（${v.hint}）` }))} />
          </Form.Item>
          <Form.Item name="reason" label="调整原因（将记录到时间线）" rules={[{ required: true, message: '请填写调整原因' }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </Row>
  )
}

/* ---------------- 评估 ---------------- */
function AssessmentTab({ patientId, canEdit }) {
  const [list, setList] = useState([])
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()
  const [gaitVideo, setGaitVideo] = useState([])

  const load = () => api.get(`/patients/${patientId}/assessments`).then((res) => setList(res.data))
  useEffect(() => { load() }, [patientId])

  const create = async () => {
    const values = await form.validateFields()
    const rom = {}
    ;(values.romEntries || []).forEach((e) => { if (e?.joint) rom[e.joint] = e.angle })
    await api.post(`/patients/${patientId}/assessments`, {
      type: values.type,
      romJson: JSON.stringify(rom),
      muscleStrength: values.muscleStrength,
      painScore: values.painScore,
      adlScore: values.adlScore,
      gaitVideoPath: gaitVideo[0] || null,
      notes: values.notes,
      assessmentDate: values.assessmentDate?.format('YYYY-MM-DD')
    })
    message.success('评估已保存')
    setOpen(false)
    form.resetFields()
    setGaitVideo([])
    load()
  }

  return (
    <div>
      {canEdit && <Button type="primary" icon={<PlusOutlined />} style={{ marginBottom: 12 }} onClick={() => setOpen(true)}>录入评估</Button>}
      <Table
        rowKey="id" size="small" dataSource={list} pagination={false}
        columns={[
          { title: '日期', dataIndex: 'assessmentDate', width: 110 },
          { title: '类型', dataIndex: 'type', width: 100, render: (v) => <Tag color={v === 'INITIAL' ? 'blue' : 'green'}>{v === 'INITIAL' ? '初次评估' : '复诊评估'}</Tag> },
          {
            title: '关节活动度', dataIndex: 'romJson',
            render: (v) => {
              const rom = parseJson(v, {})
              return Object.entries(rom).map(([k, val]) => <Tag key={k} style={{ marginBottom: 2 }}>{k} {val}°</Tag>)
            }
          },
          { title: '肌力', dataIndex: 'muscleStrength', width: 70, render: (v) => v == null ? '-' : `${v} 级` },
          { title: '疼痛', dataIndex: 'painScore', width: 70, render: (v) => v == null ? '-' : `${v} 分` },
          { title: 'ADL', dataIndex: 'adlScore', width: 70, render: (v) => v ?? '-' },
          {
            title: '步态视频', dataIndex: 'gaitVideoPath', width: 90,
            render: (v) => v ? <a href={fileUrl(v)} target="_blank" rel="noreferrer">查看</a> : '-'
          },
          { title: '评估意见', dataIndex: 'notes', ellipsis: true },
          { title: '评估人', width: 90, render: (_, r) => r.therapist?.name }
        ]}
      />
      <Modal title="录入康复评估" open={open} onOk={create} onCancel={() => setOpen(false)} width={720} okText="保存">
        <Form form={form} layout="vertical" initialValues={{ type: 'FOLLOWUP', assessmentDate: dayjs(), romEntries: [{ joint: '肩关节前屈' }, { joint: '膝关节屈曲' }] }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0 16px' }}>
            <Form.Item name="type" label="评估类型" rules={[{ required: true }]}>
              <Select options={[{ value: 'INITIAL', label: '初次评估' }, { value: 'FOLLOWUP', label: '复诊评估' }]} />
            </Form.Item>
            <Form.Item name="assessmentDate" label="评估日期" rules={[{ required: true }]}><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="painScore" label="疼痛评分 VAS（0-10）"><InputNumber min={0} max={10} style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="muscleStrength" label="肌力（0-5 级）"><InputNumber min={0} max={5} style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="adlScore" label="ADL 评分（0-100）"><InputNumber min={0} max={100} style={{ width: '100%' }} /></Form.Item>
            <Form.Item label="步态视频">
              <FileUpload value={gaitVideo} onChange={setGaitVideo} accept="video/*" label="上传步态视频" max={1} />
            </Form.Item>
          </div>
          <Form.Item label="关节活动度（°）">
            <Form.List name="romEntries">
              {(fields, { add, remove }) => (
                <>
                  {fields.map((field) => (
                    <Space key={field.key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                      <Form.Item name={[field.name, 'joint']} noStyle rules={[{ required: true, message: '部位' }]}>
                        <Input placeholder="关节/动作" style={{ width: 180 }} />
                      </Form.Item>
                      <Form.Item name={[field.name, 'angle']} noStyle rules={[{ required: true, message: '角度' }]}>
                        <InputNumber placeholder="角度°" min={-30} max={360} />
                      </Form.Item>
                      <MinusCircleOutlined onClick={() => remove(field.name)} />
                    </Space>
                  ))}
                  <Button type="dashed" onClick={() => add()} icon={<PlusOutlined />} size="small">添加关节</Button>
                </>
              )}
            </Form.List>
          </Form.Item>
          <Form.Item name="notes" label="评估意见"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

/* ---------------- 处方 ---------------- */
function PrescriptionTab({ patientId, canEdit }) {
  const [list, setList] = useState([])
  const [createOpen, setCreateOpen] = useState(false)
  const [adjustTarget, setAdjustTarget] = useState(null)
  const [form] = Form.useForm()
  const [adjustForm] = Form.useForm()
  const [assessments, setAssessments] = useState([])

  const load = () => api.get(`/patients/${patientId}/prescriptions`).then((res) => setList(res.data))
  useEffect(() => {
    load()
    if (canEdit) api.get(`/patients/${patientId}/assessments`).then((res) => setAssessments(res.data)).catch(() => {})
  }, [patientId])

  const create = async () => {
    const values = await form.validateFields()
    await api.post(`/patients/${patientId}/prescriptions`, {
      assessmentId: values.assessmentId,
      startDate: values.startDate?.format('YYYY-MM-DD'),
      endDate: values.endDate?.format('YYYY-MM-DD'),
      nextReviewDate: values.nextReviewDate?.format('YYYY-MM-DD'),
      painThreshold: values.painThreshold,
      notes: values.notes,
      adjustReason: values.adjustReason,
      items: values.items
    })
    message.success('新阶段处方已开具，旧处方已归档')
    setCreateOpen(false)
    form.resetFields()
    load()
  }

  const adjust = async () => {
    const values = await adjustForm.validateFields()
    await api.post(`/prescriptions/${adjustTarget.id}/adjust`, {
      decision: values.decision,
      reason: values.reason,
      nextReviewDate: values.nextReviewDate?.format('YYYY-MM-DD')
    })
    message.success('决策已记录到时间线')
    setAdjustTarget(null)
    adjustForm.resetFields()
    load()
  }

  const itemColumns = [
    { title: '训练动作', dataIndex: 'exerciseName', width: 150, render: (v) => <b>{v}</b> },
    { title: '剂量', width: 130, render: (_, r) => `${r.targetSets} 组 × ${r.targetReps} 次，每日 ${r.frequencyPerDay} 次` },
    { title: '禁忌动作', dataIndex: 'contraindication', render: (v) => v ? <span style={{ color: '#cf1322' }}>{v}</span> : '-' },
    { title: '辅助器具', dataIndex: 'assistiveDevice', width: 110, render: (v) => v || '-' },
    { title: '疼痛阈值', dataIndex: 'painThreshold', width: 80, render: (v) => v == null ? '-' : `${v} 分` },
    { title: '家属观察点', dataIndex: 'familyObservation', ellipsis: true },
    {
      title: '医保', width: 130,
      render: (_, r) => r.reimbursable ? <Tag color="green">{r.insuranceCode} ¥{r.unitPrice}/次</Tag> : <Tag>自费</Tag>
    }
  ]

  return (
    <div>
      {canEdit && (
        <Button type="primary" icon={<PlusOutlined />} style={{ marginBottom: 12 }} onClick={() => setCreateOpen(true)}>
          开具新阶段处方
        </Button>
      )}
      {list.length === 0 && <Empty description="尚未开具处方" />}
      {list.map((rx) => (
        <Card
          key={rx.id} size="small" style={{ marginBottom: 16 }}
          title={
            <Space>
              <span>第 {rx.phase} 阶段处方</span>
              <Tag color={rx.status === 'ACTIVE' ? 'green' : 'default'}>{rx.status === 'ACTIVE' ? '执行中' : rx.status === 'SUPERSEDED' ? '已被替代' : '已完成'}</Tag>
              <span style={{ fontWeight: 400, fontSize: 12, color: '#888' }}>
                {rx.startDate} ~ {rx.endDate || '…'} ｜ 整体疼痛阈值 {rx.painThreshold} 分 ｜ 计划复诊 {rx.nextReviewDate || '-'}
              </span>
            </Space>
          }
          extra={canEdit && rx.status === 'ACTIVE' && (
            <Button size="small" onClick={() => setAdjustTarget(rx)}>处方决策</Button>
          )}
        >
          {rx.adjustReason && <div style={{ marginBottom: 8, fontSize: 13, color: '#fa8c16' }}>生成原因：{rx.adjustReason}</div>}
          {rx.notes && <div style={{ marginBottom: 8, fontSize: 13, color: '#666' }}>备注：{rx.notes}</div>}
          <Table rowKey="id" size="small" dataSource={rx.items} columns={itemColumns} pagination={false} />
        </Card>
      ))}

      {/* 新建处方 */}
      <Modal title="开具新阶段处方" open={createOpen} onOk={create} onCancel={() => setCreateOpen(false)} width={900} okText="开具处方">
        <Form form={form} layout="vertical" initialValues={{ startDate: dayjs(), painThreshold: 6, items: [{}] }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0 16px' }}>
            <Form.Item name="assessmentId" label="关联评估">
              <Select allowClear options={assessments.map((a) => ({ value: a.id, label: `${a.assessmentDate} ${a.type === 'INITIAL' ? '初评' : '复诊评估'}` }))} />
            </Form.Item>
            <Form.Item name="startDate" label="开始日期" rules={[{ required: true }]}><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="endDate" label="结束日期"><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="nextReviewDate" label="计划复诊日期"><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="painThreshold" label="整体疼痛阈值（VAS）"><InputNumber min={1} max={10} style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="notes" label="处方备注" style={{ gridColumn: 'span 3' }}><Input /></Form.Item>
          </div>
          <Form.Item name="adjustReason" label="本处方生成原因（将写入时间线，解释为什么开/为什么变）">
            <Input.TextArea rows={2} placeholder="如：复诊评估肌力恢复至 4 级，进入强化期；或：疼痛升高，降低强度" />
          </Form.Item>
          <Form.List name="items">
            {(fields, { add, remove }) => (
              <>
                {fields.map((field) => (
                  <Card key={field.key} size="small" style={{ marginBottom: 8, background: '#fafafa' }}
                    title={`训练项目 ${field.name + 1}`}
                    extra={fields.length > 1 && <MinusCircleOutlined onClick={() => remove(field.name)} />}>
                    <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 1fr 1fr', gap: '0 12px' }}>
                      <Form.Item name={[field.name, 'exerciseName']} label="动作名称" rules={[{ required: true, message: '必填' }]}><Input placeholder="如：桥式运动" /></Form.Item>
                      <Form.Item name={[field.name, 'targetSets']} label="组数"><InputNumber min={1} max={20} style={{ width: '100%' }} /></Form.Item>
                      <Form.Item name={[field.name, 'targetReps']} label="每组次数"><InputNumber min={1} max={100} style={{ width: '100%' }} /></Form.Item>
                      <Form.Item name={[field.name, 'frequencyPerDay']} label="每日次数"><InputNumber min={1} max={10} style={{ width: '100%' }} /></Form.Item>
                      <Form.Item name={[field.name, 'painThreshold']} label="疼痛阈值"><InputNumber min={1} max={10} style={{ width: '100%' }} /></Form.Item>
                      <Form.Item name={[field.name, 'contraindication']} label="禁忌动作" style={{ gridColumn: 'span 2' }}><Input /></Form.Item>
                      <Form.Item name={[field.name, 'assistiveDevice']} label="辅助器具"><Input placeholder="如：弹力带/助行器" /></Form.Item>
                      <Form.Item name={[field.name, 'familyObservation']} label="家属观察点" style={{ gridColumn: 'span 2' }}><Input /></Form.Item>
                      <Form.Item name={[field.name, 'reimbursable']} label="医保可报销" valuePropName="checked"><Switch /></Form.Item>
                      <Form.Item name={[field.name, 'insuranceCode']} label="医保编码"><Input placeholder="如 340200020" /></Form.Item>
                      <Form.Item name={[field.name, 'unitPrice']} label="单价（元/次）"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
                    </div>
                  </Card>
                ))}
                <Button type="dashed" block onClick={() => add({ frequencyPerDay: 1, targetSets: 3, targetReps: 10, reimbursable: false })} icon={<PlusOutlined />}>添加训练项目</Button>
              </>
            )}
          </Form.List>
        </Form>
      </Modal>

      {/* 处方决策 */}
      <Modal title={`处方决策（第 ${adjustTarget?.phase} 阶段）`} open={!!adjustTarget} onOk={adjust} onCancel={() => setAdjustTarget(null)} okText="记录决策">
        <Form form={adjustForm} layout="vertical">
          <Form.Item name="decision" label="决策" rules={[{ required: true, message: '请选择决策' }]}>
            <Select options={Object.entries(DECISION).map(([k, v]) => ({ value: k, label: v }))} />
          </Form.Item>
          <Form.Item name="reason" label="决策依据（结合打卡/疼痛/随访数据，写入时间线）" rules={[{ required: true, message: '请填写决策依据' }]}>
            <Input.TextArea rows={3} placeholder="如：近 7 天依从性 90%，疼痛稳定在 3 分以下，维持原处方" />
          </Form.Item>
          <Form.Item noStyle shouldUpdate={(a, b) => a.decision !== b.decision}>
            {({ getFieldValue }) => getFieldValue('decision') === 'ADD_OFFLINE_VISIT' && (
              <Form.Item name="nextReviewDate" label="线下复诊日期" rules={[{ required: true, message: '请选择复诊日期' }]}>
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            )}
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

/* ---------------- 训练反馈 ---------------- */
function LogsTab({ patientId, canEdit }) {
  const [logs, setLogs] = useState([])
  const [curve, setCurve] = useState([])
  const [adherence, setAdherence] = useState(null)
  const [feedbackTarget, setFeedbackTarget] = useState(null)
  const [feedbackText, setFeedbackText] = useState('')

  const load = () => {
    api.get(`/patients/${patientId}/training-logs`).then((res) => setLogs(res.data))
    api.get(`/patients/${patientId}/pain-curve?days=30`).then((res) => setCurve(res.data))
    api.get(`/patients/${patientId}/adherence?days=14`).then((res) => setAdherence(res.data))
  }
  useEffect(() => { load() }, [patientId])

  const submitFeedback = async () => {
    if (!feedbackText.trim()) return message.warning('请输入纠错内容')
    await api.post(`/training-logs/${feedbackTarget.id}/feedback`, { feedback: feedbackText })
    message.success('纠错反馈已保存并写入时间线')
    setFeedbackTarget(null)
    setFeedbackText('')
    load()
  }

  return (
    <div>
      {adherence && (
        <Row gutter={16} style={{ marginBottom: 12 }}>
          <Col span={6}><Card size="small"><Statistic title="近 14 天应练天数" value={adherence.plannedDays} suffix="天" /></Card></Col>
          <Col span={6}><Card size="small"><Statistic title="实际打卡" value={adherence.loggedDays} suffix="天" /></Card></Col>
          <Col span={6}><Card size="small"><Statistic title="训练依从性" value={adherence.adherencePercent} suffix="%" valueStyle={{ color: adherence.adherencePercent >= 80 ? '#52c41a' : '#fa8c16' }} /></Card></Col>
          <Col span={6}><Card size="small"><Statistic title="平均完成率" value={adherence.avgCompletion} suffix="%" /></Card></Col>
        </Row>
      )}
      <Card size="small" title="疼痛曲线与完成率（近 30 天）" style={{ marginBottom: 12 }}>
        <PainChart data={curve} />
      </Card>
      <List
        header={<b>每日打卡记录（{logs.length}）</b>}
        dataSource={logs}
        pagination={{ pageSize: 8 }}
        renderItem={(log) => (
          <List.Item
            actions={canEdit ? [<Button key="fb" size="small" icon={<ToolOutlined />} onClick={() => { setFeedbackTarget(log); setFeedbackText(log.therapistFeedback || '') }}>视频纠错</Button>] : undefined}
          >
            <List.Item.Meta
              title={
                <Space wrap>
                  <b>{log.logDate}</b>
                  <Tag color={log.completionRate >= 80 ? 'green' : log.completionRate >= 50 ? 'orange' : 'red'}>完成率 {log.completionRate}%</Tag>
                  <Tag>疼痛 {log.painBefore ?? '-'} → {log.painAfter ?? '-'}</Tag>
                  {log.compensationObserved && <Tag color="volcano">动作代偿</Tag>}
                  {log.companionAvailable === false && <Tag color="orange">无法陪练</Tag>}
                </Space>
              }
              description={
                <div>
                  {log.familyNote && <div>家属备注：{log.familyNote}</div>}
                  <MediaView photos={log.abnormalPhotos} videos={log.videoClips} />
                  {log.therapistFeedback && (
                    <div style={{ marginTop: 6, background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: 6, padding: '6px 10px', fontSize: 13 }}>
                      <b>治疗师纠错（{log.feedbackBy}）：</b>{log.therapistFeedback}
                    </div>
                  )}
                </div>
              }
            />
          </List.Item>
        )}
      />
      <Modal title={`视频纠错（${feedbackTarget?.logDate} 打卡）`} open={!!feedbackTarget} onOk={submitFeedback} onCancel={() => setFeedbackTarget(null)} okText="保存反馈">
        <Input.TextArea rows={4} value={feedbackText} onChange={(e) => setFeedbackText(e.target.value)}
          placeholder="指出动作问题与纠正方法，将同步给家属并写入时间线" />
      </Modal>
    </div>
  )
}

/* ---------------- 时间线 ---------------- */
function TimelineTab({ patientId }) {
  const [events, setEvents] = useState([])
  useEffect(() => {
    api.get(`/patients/${patientId}/timeline`).then((res) => setEvents(res.data))
  }, [patientId])
  return <TimelineView events={events} />
}

/* ---------------- 医保结算 ---------------- */
function InsuranceTab({ patientId, canEdit, insuranceType }) {
  const [treatments, setTreatments] = useState([])
  const [settlements, setSettlements] = useState([])
  const [prescription, setPrescription] = useState(null)
  const [treatOpen, setTreatOpen] = useState(false)
  const [settleOpen, setSettleOpen] = useState(false)
  const [detail, setDetail] = useState(null)
  const [treatForm] = Form.useForm()
  const [settleForm] = Form.useForm()

  const load = () => {
    api.get(`/patients/${patientId}/treatments`).then((res) => setTreatments(res.data))
    api.get(`/patients/${patientId}/settlements`).then((res) => setSettlements(res.data))
    api.get(`/patients/${patientId}/prescriptions/active`, { silent: true }).then((res) => setPrescription(res.data)).catch(() => {})
  }
  useEffect(() => { load() }, [patientId])

  const addTreatment = async () => {
    const values = await treatForm.validateFields()
    await api.post(`/patients/${patientId}/treatments`, {
      ...values,
      treatmentDate: values.treatmentDate?.format('YYYY-MM-DD')
    })
    message.success('线下治疗记录已保存')
    setTreatOpen(false)
    treatForm.resetFields()
    load()
  }

  const createSettlement = async () => {
    const values = await settleForm.validateFields()
    await api.post(`/patients/${patientId}/settlements`, {
      periodStart: values.period[0].format('YYYY-MM-DD'),
      periodEnd: values.period[1].format('YYYY-MM-DD')
    })
    message.success('结算单已生成（结合处方执行 + 复诊评估 + 线下治疗）')
    setSettleOpen(false)
    settleForm.resetFields()
    load()
  }

  const confirm = async (id) => {
    await api.post(`/settlements/${id}/confirm`)
    message.success('结算单已确认')
    load()
  }

  const reimbursableItems = (prescription?.items || []).filter((i) => i.reimbursable)

  return (
    <div>
      <Row gutter={16}>
        <Col span={12}>
          <Card size="small" title="当前处方医保可报销项目" style={{ marginBottom: 16 }}>
            {reimbursableItems.length === 0 ? <Empty description="无可报销项目" image={Empty.PRESENTED_IMAGE_SIMPLE} /> : (
              <Table rowKey="id" size="small" pagination={false} dataSource={reimbursableItems}
                columns={[
                  { title: '项目', dataIndex: 'exerciseName' },
                  { title: '医保编码', dataIndex: 'insuranceCode', width: 110 },
                  { title: '单价', dataIndex: 'unitPrice', width: 90, render: (v) => `¥${v}/次` },
                  { title: '每日次数', dataIndex: 'frequencyPerDay', width: 80 }
                ]} />
            )}
            <div style={{ fontSize: 12, color: '#888', marginTop: 8 }}>患者医保类型：{insuranceType || '自费'}（报销比例：职工 80% / 居民 70% / 新农合 60%）</div>
          </Card>
          <Card size="small" title="线下治疗记录"
            extra={canEdit && <Button size="small" icon={<PlusOutlined />} onClick={() => setTreatOpen(true)}>添加</Button>}>
            <Table rowKey="id" size="small" pagination={false} dataSource={treatments}
              columns={[
                { title: '日期', dataIndex: 'treatmentDate', width: 110 },
                { title: '项目', dataIndex: 'itemName' },
                { title: '编码', dataIndex: 'insuranceCode', width: 100 },
                { title: '金额', dataIndex: 'amount', width: 80, render: (v) => `¥${v}` },
                { title: '报销', dataIndex: 'reimbursable', width: 70, render: (v) => v ? <Tag color="green">可</Tag> : <Tag>否</Tag> }
              ]} />
          </Card>
        </Col>
        <Col span={12}>
          <Card size="small" title="医保结算单"
            extra={canEdit && <Button size="small" type="primary" onClick={() => setSettleOpen(true)}>生成结算单</Button>}>
            <Table
              rowKey="id" size="small" pagination={false} dataSource={settlements}
              onRow={(r) => ({ onClick: () => setDetail(r), style: { cursor: 'pointer' } })}
              columns={[
                { title: '结算周期', render: (_, r) => `${r.periodStart} ~ ${r.periodEnd}`, width: 190 },
                { title: '依从性', dataIndex: 'adherencePercent', width: 70, render: (v) => `${v}%` },
                { title: '合计', dataIndex: 'totalAmount', width: 90, render: (v) => `¥${v}` },
                { title: '报销', dataIndex: 'reimbursableAmount', width: 90, render: (v) => <span className="money">¥{v}</span> },
                {
                  title: '状态', dataIndex: 'status', width: 110,
                  render: (v, r) => (
                    <Space>
                      <Tag color={v === 'CONFIRMED' ? 'green' : 'orange'}>{v === 'CONFIRMED' ? '已确认' : '草稿'}</Tag>
                      {v === 'DRAFT' && canEdit && (
                        <Popconfirm title="确认该结算单？" onConfirm={() => confirm(r.id)}>
                          <Button size="small" onClick={(e) => e.stopPropagation()}>确认</Button>
                        </Popconfirm>
                      )}
                    </Space>
                  )
                }
              ]} />
            <div style={{ fontSize: 12, color: '#888', marginTop: 8 }}>点击行查看费用明细</div>
          </Card>
        </Col>
      </Row>

      <Modal title="添加线下治疗记录" open={treatOpen} onOk={addTreatment} onCancel={() => setTreatOpen(false)} okText="保存">
        <Form form={treatForm} layout="vertical" initialValues={{ treatmentDate: dayjs(), reimbursable: true }}>
          <Form.Item name="itemName" label="治疗项目" rules={[{ required: true, message: '必填' }]}>
            <Select showSearch options={['运动疗法（门诊）', '中频脉冲电治疗', '关节松动训练（门诊）', '针灸治疗', '推拿治疗', '作业疗法'].map((v) => ({ value: v, label: v }))} />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
            <Form.Item name="insuranceCode" label="医保编码"><Input placeholder="如 340200020" /></Form.Item>
            <Form.Item name="amount" label="金额（元）" rules={[{ required: true, message: '必填' }]}><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="treatmentDate" label="治疗日期" rules={[{ required: true }]}><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="reimbursable" label="医保可报销" valuePropName="checked"><Switch /></Form.Item>
          </div>
          <Form.Item name="note" label="备注"><Input.TextArea rows={2} /></Form.Item>
        </Form>
      </Modal>

      <Modal title="生成医保结算单" open={settleOpen} onOk={createSettlement} onCancel={() => setSettleOpen(false)} okText="生成">
        <Form form={settleForm} layout="vertical" initialValues={{ period: [dayjs().subtract(1, 'month'), dayjs()] }}>
          <Form.Item name="period" label="结算周期" rules={[{ required: true, message: '请选择周期' }]}>
            <DatePicker.RangePicker style={{ width: '100%' }} />
          </Form.Item>
          <div style={{ fontSize: 13, color: '#666' }}>
            结算将自动合并：① 居家训练可报销项目（按处方执行率折算次数）② 周期内复诊评估费 ③ 线下治疗记录。
          </div>
        </Form>
      </Modal>

      <Modal title="结算单明细" open={!!detail} footer={null} onCancel={() => setDetail(null)} width={640}>
        {detail && (
          <div>
            <Descriptions size="small" column={2} bordered style={{ marginBottom: 12 }}>
              <Descriptions.Item label="周期">{detail.periodStart} ~ {detail.periodEnd}</Descriptions.Item>
              <Descriptions.Item label="训练依从性">{detail.adherencePercent}%</Descriptions.Item>
              <Descriptions.Item label="居家训练">¥{detail.homeTrainingAmount}</Descriptions.Item>
              <Descriptions.Item label="复诊评估">¥{detail.assessmentAmount}</Descriptions.Item>
              <Descriptions.Item label="线下治疗">¥{detail.outpatientAmount}</Descriptions.Item>
              <Descriptions.Item label="合计">¥{detail.totalAmount}</Descriptions.Item>
              <Descriptions.Item label="医保报销"><span className="money">¥{detail.reimbursableAmount}</span></Descriptions.Item>
              <Descriptions.Item label="个人自付">¥{detail.selfPayAmount}</Descriptions.Item>
            </Descriptions>
            <Table
              rowKey={(r) => r.name} size="small" pagination={false}
              dataSource={parseJson(detail.detailJson)}
              columns={[
                { title: '费用项目', dataIndex: 'name' },
                { title: '编码', dataIndex: 'code', width: 100 },
                { title: '说明', dataIndex: 'note' },
                { title: '金额', dataIndex: 'amount', width: 90, render: (v) => `¥${v}` },
                { title: '报销', dataIndex: 'reimbursable', width: 60, render: (v) => v ? '可' : '否' }
              ]} />
          </div>
        )}
      </Modal>
    </div>
  )
}
