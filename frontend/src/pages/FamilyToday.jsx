import React, { useEffect, useState } from 'react'
import { Card, Checkbox, Slider, Switch, Input, Button, Tag, message, Row, Col, Statistic, Alert, Empty, List } from 'antd'
import { CheckCircleOutlined, WarningOutlined, EyeOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import PainChart from '../components/PainChart'
import MediaView from '../components/MediaView'
import FileUpload from '../components/FileUpload'
import { KeyPointTags, CorrectionStatusTag, VideoList } from '../components/Correction'
import { DISEASE_TYPE, STAGE, parseJson } from '../utils'

/** 家属端：今日训练任务打卡 + 历史记录 */
export default function FamilyToday() {
  const navigate = useNavigate()
  const [patient, setPatient] = useState(null)
  const [tasks, setTasks] = useState(null)
  const [pendingCorrections, setPendingCorrections] = useState([])
  const [activeCorrections, setActiveCorrections] = useState([])
  const [doneMap, setDoneMap] = useState({})
  const [painBefore, setPainBefore] = useState(2)
  const [painAfter, setPainAfter] = useState(3)
  const [companion, setCompanion] = useState(true)
  const [compensation, setCompensation] = useState(false)
  const [note, setNote] = useState('')
  const [photos, setPhotos] = useState([])
  const [videos, setVideos] = useState([])
  const [submitting, setSubmitting] = useState(false)
  const [logs, setLogs] = useState([])
  const [curve, setCurve] = useState([])

  const load = async () => {
    const res = await api.get('/patients')
    if (res.data.length === 0) return
    const p = res.data[0]
    setPatient(p)
    let t
    try {
      t = await api.get(`/patients/${p.id}/today-tasks`, { silent: true })
    } catch {
      t = { data: { prescription: { items: [] }, logSubmitted: false, todayLog: null, pendingCorrections: [], activeCorrections: [] } }
    }
    setTasks(t.data)
    setPendingCorrections(t.data.pendingCorrections || [])
    setActiveCorrections(t.data.activeCorrections || [])
    if (t.data.todayLog) {
      const log = t.data.todayLog
      const map = {}
      parseJson(log.completedDetail).forEach((d) => { map[d.itemId] = !!d.done })
      setDoneMap(map)
      setPainBefore(log.painBefore ?? 2)
      setPainAfter(log.painAfter ?? 3)
      setCompanion(log.companionAvailable !== false)
      setCompensation(!!log.compensationObserved)
      setNote(log.familyNote || '')
      setPhotos(parseJson(log.abnormalPhotos))
      setVideos(parseJson(log.videoClips))
    }
    api.get(`/patients/${p.id}/training-logs?days=14`).then((r) => setLogs(r.data))
    api.get(`/patients/${p.id}/pain-curve?days=14`).then((r) => setCurve(r.data))
  }

  useEffect(() => { load().catch(() => {}) }, [])

  const confirmCorrection = async (taskId) => {
    await api.post(`/correction-tasks/${taskId}/confirm`)
    message.success('已确认观看纠错内容，可以继续打卡')
    load()
  }

  if (!patient) return <Empty description="暂未绑定患者档案，请联系康复中心" style={{ padding: 60 }} />
  if (!tasks) return null

  const items = tasks.prescription?.items || []
  const doneCount = items.filter((i) => doneMap[i.id]).length
  const completionRate = items.length === 0 ? 0 : Math.round((doneCount * 100) / items.length)
  const blocked = pendingCorrections.length > 0

  const submit = async () => {
    setSubmitting(true)
    try {
      await api.post(`/patients/${patient.id}/training-logs`, {
        completionRate,
        items: items.map((i) => ({ itemId: i.id, done: !!doneMap[i.id], actualSets: doneMap[i.id] ? i.targetSets : 0, actualReps: doneMap[i.id] ? i.targetReps : 0 })),
        painBefore,
        painAfter,
        abnormalPhotos: photos,
        videoClips: videos,
        familyNote: note,
        companionAvailable: companion,
        compensationObserved: compensation
      })
      message.success('今日打卡已提交，治疗师会及时查看')
      load()
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Row gutter={16}>
      <Col span={14}>
        {/* 待确认的视频纠错任务（打卡闸门） */}
        {pendingCorrections.map((task) => (
          <Alert
            key={task.id}
            type="warning"
            style={{ marginBottom: 16 }}
            message={
              <span><WarningOutlined /> 治疗师打回了 {task.sourceLog?.logDate} 的训练视频，请先观看纠错内容并确认，确认后才能打卡</span>
            }
            description={
              <Card size="small" style={{ marginTop: 8, background: '#fffbe6' }}>
                <div style={{ marginBottom: 4 }}>
                  <b>训练动作：{task.prescriptionItem?.exerciseName || '训练动作'}</b>
                  <CorrectionStatusTag status={task.status} />
                </div>
                <div style={{ marginBottom: 4 }}>关键动作点：</div>
                <KeyPointTags keyPoints={task.keyPoints} />
                <div style={{ margin: '8px 0', padding: 8, background: '#fff', borderRadius: 6, border: '1px solid #ffe58f' }}>
                  <b>纠错说明：</b>{task.correctionNote}
                </div>
                <Row gutter={16}>
                  <Col span={12}>
                    <div style={{ fontSize: 12, color: '#888', marginBottom: 4 }}>被纠错的视频（{task.sourceLog?.logDate}）</div>
                    <VideoList videosJson={task.sourceLog?.videoClips} maxWidth={260} />
                  </Col>
                </Row>
                <Button type="primary" icon={<EyeOutlined />} onClick={() => confirmCorrection(task.id)}>
                  我已观看并理解纠错内容
                </Button>
              </Card>
            }
          />
        ))}

        {/* 进行中的纠错提醒（已确认/待复评） */}
        {activeCorrections.length > 0 && (
          <Card size="small" title="动作纠正提醒（请在本轮训练中重点注意）" style={{ marginBottom: 16 }}>
            {activeCorrections.map((task) => (
              <div key={task.id} style={{ marginBottom: 10 }}>
                <CorrectionStatusTag status={task.status} />
                <b>{task.prescriptionItem?.exerciseName || '训练动作'}</b>
                {task.consecutiveErrors > 0 && <Tag color="red">已连续 {task.consecutiveErrors} 次未掌握</Tag>}
                <KeyPointTags keyPoints={task.keyPoints} />
                <div style={{ fontSize: 13, color: '#666' }}>{task.correctionNote}</div>
              </div>
            ))}
            <div style={{ fontSize: 12, color: '#1677ff' }}>提示：今天打卡请上传训练视频，系统会与之前被纠错的视频对比，供治疗师判断动作是否真正掌握。</div>
          </Card>
        )}

        <Card
          title={
            <span>
              今日训练任务（{patient.name}）
              <Tag color="blue" style={{ marginLeft: 8 }}>{DISEASE_TYPE[patient.diseaseType]}</Tag>
              <Tag color={STAGE[patient.diseaseStage]?.color}>{STAGE[patient.diseaseStage]?.label}</Tag>
            </span>
          }
          extra={tasks.logSubmitted && <Tag icon={<CheckCircleOutlined />} color="success">今日已打卡，可修改后重新提交</Tag>}
        >
          {items.length === 0 && <Empty description="当前没有执行中的处方，请联系治疗师" />}
          {items.map((item) => (
            <div key={item.id} className={`task-item ${doneMap[item.id] ? 'done' : ''}`}>
              <Checkbox checked={!!doneMap[item.id]} onChange={(e) => setDoneMap({ ...doneMap, [item.id]: e.target.checked })}>
                <b style={{ fontSize: 15 }}>{item.exerciseName}</b>
              </Checkbox>
              <div className="task-meta">
                剂量：{item.targetSets} 组 × {item.targetReps} 次，每日 {item.frequencyPerDay} 次
                {item.assistiveDevice && item.assistiveDevice !== '无' && <span> ｜ 辅助器具：{item.assistiveDevice}</span>}
                {item.painThreshold != null && <span> ｜ 疼痛超过 {item.painThreshold} 分请停止该项</span>}
              </div>
              {item.contraindication && <div className="task-warn">⚠ 禁忌：{item.contraindication}</div>}
              {item.familyObservation && <div className="task-observe">👀 家属观察点：{item.familyObservation}</div>}
            </div>
          ))}

          {items.length > 0 && (
            <Card size="small" title="今日训练反馈" style={{ marginTop: 16 }}>
              <div style={{ marginBottom: 12 }}>
                <b>完成率：{completionRate}%</b>（已完成 {doneCount}/{items.length} 项）
              </div>
              <Row gutter={24}>
                <Col span={12}>
                  <div>训练前疼痛（{painBefore} 分）</div>
                  <Slider min={0} max={10} value={painBefore} onChange={setPainBefore} marks={{ 0: '无', 5: '中', 10: '剧痛' }} />
                </Col>
                <Col span={12}>
                  <div>训练后疼痛（{painAfter} 分）</div>
                  <Slider min={0} max={10} value={painAfter} onChange={setPainAfter} marks={{ 0: '无', 5: '中', 10: '剧痛' }} />
                </Col>
              </Row>
              {painAfter >= (tasks.prescription?.painThreshold ?? 6) && (
                <Alert style={{ marginBottom: 12 }} type="warning" showIcon
                  message={`训练后疼痛达到 ${painAfter} 分，已超过处方疼痛阈值（${tasks.prescription?.painThreshold} 分），提交后将自动通知康复护士随访。`} />
              )}
              <div style={{ display: 'flex', gap: 32, margin: '8px 0 12px' }}>
                <span>家属能陪练：<Switch checked={companion} onChange={setCompanion} /></span>
                <span>发现明显动作代偿：<Switch checked={compensation} onChange={setCompensation} /></span>
              </div>
              <Input.TextArea rows={2} value={note} onChange={(e) => setNote(e.target.value)}
                placeholder="补充说明：今天训练情况、疼痛变化、异常表现……" style={{ marginBottom: 12 }} />
              <Row gutter={16}>
                <Col span={12}>
                  <div style={{ marginBottom: 4, color: '#666' }}>异常照片（红肿/破皮等）</div>
                  <FileUpload value={photos} onChange={setPhotos} accept="image/*" label="上传照片" />
                </Col>
                <Col span={12}>
                  <div style={{ marginBottom: 4, color: '#666' }}>训练视频片段（供治疗师纠错）</div>
                  <FileUpload value={videos} onChange={setVideos} accept="video/*" label="上传视频" />
                </Col>
              </Row>
              {blocked && (
                <Alert style={{ marginTop: 16 }} type="error" showIcon
                  message={`还有 ${pendingCorrections.length} 条视频纠错未确认，请先在页面上方观看并确认`} />
              )}
              <Button type="primary" size="large" block loading={submitting} onClick={submit}
                disabled={blocked} style={{ marginTop: 16 }}>
                {tasks.logSubmitted ? '更新今日打卡' : '提交今日打卡'}
              </Button>
            </Card>
          )}
        </Card>
      </Col>
      <Col span={10}>
        <Card size="small" title="近 14 天疼痛与完成率" style={{ marginBottom: 16 }}>
          <PainChart data={curve} height={240} />
        </Card>
        <Card size="small" title="近期打卡记录"
          extra={<Button type="link" size="small" onClick={() => navigate(`/patients/${patient.id}`)}>查看完整档案与时间线</Button>}>
          <List
            size="small"
            dataSource={logs.slice(-7).reverse()}
            renderItem={(log) => (
              <List.Item>
                <List.Item.Meta
                  title={
                    <span>
                      {log.logDate}
                      <Tag color={log.completionRate >= 80 ? 'green' : 'orange'} style={{ marginLeft: 8 }}>{log.completionRate}%</Tag>
                      <Tag>疼痛 {log.painBefore ?? '-'}→{log.painAfter ?? '-'}</Tag>
                    </span>
                  }
                  description={
                    <div>
                      {log.therapistFeedback && <div style={{ color: '#52c41a' }}>治疗师纠错：{log.therapistFeedback}</div>}
                      <MediaView photos={log.abnormalPhotos} videos={log.videoClips} />
                    </div>
                  }
                />
              </List.Item>
            )}
          />
        </Card>
      </Col>
    </Row>
  )
}
