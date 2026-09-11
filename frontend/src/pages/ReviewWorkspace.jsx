import React, { useEffect, useState } from 'react'
import {
  Card, Row, Col, Statistic, Table, Tag, Button, Modal, Form, Input, InputNumber,
  Select, DatePicker, Space, message, Empty, List, Switch
} from 'antd'
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import dayjs from 'dayjs'
import api from '../api'
import PainChart from '../components/PainChart'
import MediaView from '../components/MediaView'
import FileUpload from '../components/FileUpload'
import { DECISION, DISEASE_TYPE, STAGE, parseJson } from '../utils'

/**
 * 复诊工作台：复诊时治疗师在此查看
 * 训练依从性 / 疼痛曲线 / 视频纠错记录 / 器具使用 / 医保可报销项目，
 * 录入复诊评估并决定下一阶段处方。
 */
export default function ReviewWorkspace() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [patient, setPatient] = useState(null)
  const [adherence, setAdherence] = useState(null)
  const [curve, setCurve] = useState([])
  const [corrections, setCorrections] = useState([])
  const [prescription, setPrescription] = useState(null)
  const [logs, setLogs] = useState([])
  const [assessOpen, setAssessOpen] = useState(false)
  const [decisionOpen, setDecisionOpen] = useState(false)
  const [rxOpen, setRxOpen] = useState(false)
  const [gaitVideo, setGaitVideo] = useState([])
  const [assessForm] = Form.useForm()
  const [decisionForm] = Form.useForm()
  const [rxForm] = Form.useForm()
  const [newAssessmentId, setNewAssessmentId] = useState(null)

  const load = () => {
    api.get(`/patients/${id}`).then((res) => setPatient(res.data))
    api.get(`/patients/${id}/adherence?days=14`).then((res) => setAdherence(res.data))
    api.get(`/patients/${id}/pain-curve?days=30`).then((res) => setCurve(res.data))
    api.get(`/patients/${id}/corrections`).then((res) => setCorrections(res.data))
    api.get(`/patients/${id}/prescriptions/active`, { silent: true }).then((res) => setPrescription(res.data)).catch(() => {})
    api.get(`/patients/${id}/training-logs?days=30`).then((res) => setLogs(res.data))
  }
  useEffect(() => { load() }, [id])

  if (!patient) return null

  const saveAssessment = async () => {
    const values = await assessForm.validateFields()
    const rom = {}
    ;(values.romEntries || []).forEach((e) => { if (e?.joint) rom[e.joint] = e.angle })
    const res = await api.post(`/patients/${id}/assessments`, {
      type: 'FOLLOWUP',
      romJson: JSON.stringify(rom),
      muscleStrength: values.muscleStrength,
      painScore: values.painScore,
      adlScore: values.adlScore,
      gaitVideoPath: gaitVideo[0] || null,
      notes: values.notes,
      assessmentDate: values.assessmentDate?.format('YYYY-MM-DD')
    })
    setNewAssessmentId(res.data.id)
    message.success('复诊评估已保存，请继续决策')
    setAssessOpen(false)
    setGaitVideo([])
  }

  const saveDecision = async () => {
    const values = await decisionForm.validateFields()
    await api.post(`/prescriptions/${prescription.id}/adjust`, {
      decision: values.decision,
      reason: values.reason,
      nextReviewDate: values.nextReviewDate?.format('YYYY-MM-DD')
    })
    message.success('决策已记录')
    setDecisionOpen(false)
    if (values.decision === 'REDUCE_INTENSITY') {
      message.info('请开具降低强度后的新阶段处方')
      setRxOpen(true)
    }
    load()
  }

  const createNextRx = async () => {
    const values = await rxForm.validateFields()
    await api.post(`/patients/${id}/prescriptions`, {
      assessmentId: newAssessmentId || undefined,
      startDate: values.startDate?.format('YYYY-MM-DD'),
      endDate: values.endDate?.format('YYYY-MM-DD'),
      nextReviewDate: values.nextReviewDate?.format('YYYY-MM-DD'),
      painThreshold: values.painThreshold,
      notes: values.notes,
      adjustReason: values.adjustReason,
      items: values.items
    })
    message.success('下一阶段处方已开具')
    setRxOpen(false)
    navigate(`/patients/${id}`)
  }

  // 器具使用统计：从打卡 completedDetail 统计各项目完成次数
  const deviceUsage = (prescription?.items || [])
    .filter((i) => i.assistiveDevice && i.assistiveDevice !== '无')
    .map((item) => {
      let used = 0
      logs.forEach((log) => {
        parseJson(log.completedDetail).forEach((d) => {
          if (d.itemId === item.id && d.done) used++
        })
      })
      return { ...item, usedCount: used }
    })

  const reimbursableItems = (prescription?.items || []).filter((i) => i.reimbursable)

  return (
    <div>
      <Card
        title={<Space>复诊工作台 — {patient.name}<Tag color="blue">{DISEASE_TYPE[patient.diseaseType]}</Tag><Tag color={STAGE[patient.diseaseStage]?.color}>{STAGE[patient.diseaseStage]?.label}</Tag></Space>}
        extra={
          <Space>
            <Button onClick={() => navigate(`/patients/${id}`)}>返回患者详情</Button>
            <Button type="primary" onClick={() => setAssessOpen(true)}>① 录入复诊评估</Button>
            <Button onClick={() => setDecisionOpen(true)} disabled={!prescription}>② 处方决策</Button>
            <Button type="primary" ghost onClick={() => setRxOpen(true)}>③ 开具下阶段处方</Button>
          </Space>
        }
      >
        <Row gutter={16} className="review-section">
          <Col span={6}><Card size="small"><Statistic title="近 14 天训练依从性" value={adherence?.adherencePercent ?? '-'} suffix="%" valueStyle={{ color: (adherence?.adherencePercent ?? 0) >= 80 ? '#52c41a' : '#fa8c16' }} /></Card></Col>
          <Col span={6}><Card size="small"><Statistic title="打卡天数" value={adherence ? `${adherence.loggedDays}/${adherence.plannedDays}` : '-'} suffix="天" /></Card></Col>
          <Col span={6}><Card size="small"><Statistic title="平均完成率" value={adherence?.avgCompletion ?? '-'} suffix="%" /></Card></Col>
          <Col span={6}><Card size="small"><Statistic title="视频纠错记录" value={corrections.length} suffix="条" /></Card></Col>
        </Row>

        <Card size="small" title="疼痛曲线与完成率（近 30 天）" className="review-section">
          <PainChart data={curve} height={260} />
        </Card>

        <Row gutter={16} className="review-section">
          <Col span={12}>
            <Card size="small" title="视频纠错记录">
              {corrections.length === 0 ? <Empty description="暂无纠错记录" image={Empty.PRESENTED_IMAGE_SIMPLE} /> : (
                <List
                  size="small"
                  dataSource={corrections}
                  renderItem={(c) => (
                    <List.Item>
                      <List.Item.Meta
                        title={<span><Tag>{c.logDate}</Tag>{c.feedbackBy}</span>}
                        description={
                          <div>
                            <div>{c.therapistFeedback}</div>
                            <MediaView photos={c.abnormalPhotos} videos={c.videoClips} />
                          </div>
                        }
                      />
                    </List.Item>
                  )}
                />
              )}
            </Card>
          </Col>
          <Col span={12}>
            <Card size="small" title="辅助器具使用（近 30 天打卡确认次数）">
              {deviceUsage.length === 0 ? <Empty description="当前处方无需器具" image={Empty.PRESENTED_IMAGE_SIMPLE} /> : (
                <Table rowKey="id" size="small" pagination={false} dataSource={deviceUsage}
                  columns={[
                    { title: '训练动作', dataIndex: 'exerciseName' },
                    { title: '辅助器具', dataIndex: 'assistiveDevice' },
                    { title: '完成次数', dataIndex: 'usedCount', width: 90, render: (v) => <Tag color={v > 0 ? 'green' : 'orange'}>{v} 次</Tag> }
                  ]} />
              )}
            </Card>
            <Card size="small" title="医保可报销项目（当前处方）" style={{ marginTop: 16 }}>
              {reimbursableItems.length === 0 ? <Empty description="无可报销项目" image={Empty.PRESENTED_IMAGE_SIMPLE} /> : (
                <Table rowKey="id" size="small" pagination={false} dataSource={reimbursableItems}
                  columns={[
                    { title: '项目', dataIndex: 'exerciseName' },
                    { title: '医保编码', dataIndex: 'insuranceCode', width: 110 },
                    { title: '单价', dataIndex: 'unitPrice', width: 90, render: (v) => `¥${v}/次` }
                  ]} />
              )}
            </Card>
          </Col>
        </Row>
      </Card>

      {/* 复诊评估 */}
      <Modal title="录入复诊评估" open={assessOpen} onOk={saveAssessment} onCancel={() => setAssessOpen(false)} width={720} okText="保存评估">
        <Form form={assessForm} layout="vertical" initialValues={{ assessmentDate: dayjs(), romEntries: [{ joint: '肩关节前屈' }, { joint: '膝关节屈曲' }] }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0 16px' }}>
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

      {/* 处方决策 */}
      <Modal title="处方决策（基于上述复诊数据）" open={decisionOpen} onOk={saveDecision} onCancel={() => setDecisionOpen(false)} okText="记录决策">
        <Form form={decisionForm} layout="vertical">
          <Form.Item name="decision" label="决策" rules={[{ required: true, message: '请选择决策' }]}>
            <Select options={Object.entries(DECISION).map(([k, v]) => ({ value: k, label: v }))} />
          </Form.Item>
          <Form.Item name="reason" label="决策依据" rules={[{ required: true, message: '请填写决策依据' }]}>
            <Input.TextArea rows={3} placeholder={`如：近 14 天依从性 ${adherence?.adherencePercent ?? '-'}%，疼痛曲线平稳，维持原处方`} />
          </Form.Item>
          <Form.Item noStyle shouldUpdate={(a, b) => a.decision !== b.decision}>
            {({ getFieldValue }) => getFieldValue('decision') === 'ADD_OFFLINE_VISIT' && (
              <Form.Item name="nextReviewDate" label="线下复诊日期" rules={[{ required: true, message: '请选择日期' }]}>
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            )}
          </Form.Item>
        </Form>
      </Modal>

      {/* 下阶段处方 */}
      <Modal title="开具下一阶段处方（当前处方将自动归档）" open={rxOpen} onOk={createNextRx} onCancel={() => setRxOpen(false)} width={900} okText="开具处方">
        <Form
          form={rxForm} layout="vertical"
          initialValues={{
            startDate: dayjs(),
            painThreshold: prescription?.painThreshold ?? 6,
            items: (prescription?.items || []).map((i) => ({
              exerciseName: i.exerciseName, targetSets: i.targetSets, targetReps: i.targetReps,
              frequencyPerDay: i.frequencyPerDay, contraindication: i.contraindication,
              assistiveDevice: i.assistiveDevice, painThreshold: i.painThreshold,
              familyObservation: i.familyObservation, reimbursable: i.reimbursable,
              insuranceCode: i.insuranceCode, unitPrice: i.unitPrice ? Number(i.unitPrice) : 0
            }))
          }}
        >
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0 16px' }}>
            <Form.Item name="startDate" label="开始日期" rules={[{ required: true }]}><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="endDate" label="结束日期"><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="nextReviewDate" label="计划复诊日期"><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="painThreshold" label="整体疼痛阈值"><InputNumber min={1} max={10} style={{ width: '100%' }} /></Form.Item>
          </div>
          <Form.Item name="notes" label="处方备注"><Input /></Form.Item>
          <Form.Item name="adjustReason" label="本阶段处方生成原因（写入时间线）" rules={[{ required: true, message: '请说明为什么生成该处方' }]}>
            <Input.TextArea rows={2} placeholder="如：复诊评估肌力恢复至 4 级，进入强化期；或：疼痛升高，降低训练强度" />
          </Form.Item>
          <Form.List name="items">
            {(fields, { add, remove }) => (
              <>
                {fields.map((field) => (
                  <Card key={field.key} size="small" style={{ marginBottom: 8, background: '#fafafa' }}
                    title={`训练项目 ${field.name + 1}`}
                    extra={fields.length > 1 && <MinusCircleOutlined onClick={() => remove(field.name)} />}>
                    <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 1fr 1fr', gap: '0 12px' }}>
                      <Form.Item name={[field.name, 'exerciseName']} label="动作名称" rules={[{ required: true, message: '必填' }]}><Input /></Form.Item>
                      <Form.Item name={[field.name, 'targetSets']} label="组数"><InputNumber min={1} max={20} style={{ width: '100%' }} /></Form.Item>
                      <Form.Item name={[field.name, 'targetReps']} label="每组次数"><InputNumber min={1} max={100} style={{ width: '100%' }} /></Form.Item>
                      <Form.Item name={[field.name, 'frequencyPerDay']} label="每日次数"><InputNumber min={1} max={10} style={{ width: '100%' }} /></Form.Item>
                      <Form.Item name={[field.name, 'painThreshold']} label="疼痛阈值"><InputNumber min={1} max={10} style={{ width: '100%' }} /></Form.Item>
                      <Form.Item name={[field.name, 'contraindication']} label="禁忌动作" style={{ gridColumn: 'span 2' }}><Input /></Form.Item>
                      <Form.Item name={[field.name, 'assistiveDevice']} label="辅助器具"><Input /></Form.Item>
                      <Form.Item name={[field.name, 'familyObservation']} label="家属观察点" style={{ gridColumn: 'span 2' }}><Input /></Form.Item>
                      <Form.Item name={[field.name, 'reimbursable']} label="医保可报销" valuePropName="checked"><Switch /></Form.Item>
                      <Form.Item name={[field.name, 'insuranceCode']} label="医保编码"><Input /></Form.Item>
                      <Form.Item name={[field.name, 'unitPrice']} label="单价（元/次）"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
                    </div>
                  </Card>
                ))}
                <Button type="dashed" block onClick={() => add({ frequencyPerDay: 1, targetSets: 3, targetReps: 10 })} icon={<PlusOutlined />}>添加训练项目</Button>
              </>
            )}
          </Form.List>
        </Form>
      </Modal>
    </div>
  )
}
