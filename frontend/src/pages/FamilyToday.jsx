import React, { useEffect, useState } from 'react'
import { Card, Checkbox, Slider, Switch, Input, Button, Tag, message, Row, Col, Alert, Empty, List, Radio } from 'antd'
import { CheckCircleOutlined, WarningOutlined, EyeOutlined, PauseCircleOutlined, MedicineBoxOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import PainChart from '../components/PainChart'
import MediaView from '../components/MediaView'
import FileUpload from '../components/FileUpload'
import { KeyPointTags, CorrectionStatusTag, VideoList } from '../components/Correction'
import { DISEASE_TYPE, STAGE, ESCALATION_STATUS, ESCALATION_TRIGGER, DISPOSITION, parseJson, asArray } from '../utils'

/** 家属端：今日训练任务打卡 + 疼痛升级处置（暂停动作/补充症状/查看医生结论） + 历史记录 */
export default function FamilyToday() {
  const navigate = useNavigate()
  const [patient, setPatient] = useState(null)
  const [tasks, setTasks] = useState(null)
  const [pendingCorrections, setPendingCorrections] = useState([])
  const [activeCorrections, setActiveCorrections] = useState([])
  const [suspendedItems, setSuspendedItems] = useState([])
  const [escalations, setEscalations] = useState([])
  const [doneMap, setDoneMap] = useState({})
  const [painBefore, setPainBefore] = useState(2)
  const [painAfter, setPainAfter] = useState(3)
  const [swelling, setSwelling] = useState(false)
  const [nightPain, setNightPain] = useState(false)
  const [painItemIds, setPainItemIds] = useState(null) // null=未手动选择，默认全部已完成项
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
    // 兼容后端返回数组或单个患者对象两种结构
    const patients = asArray(res.data)
    if (patients.length === 0) return
    const p = patients[0]
    setPatient(p)
    let t
    try {
      t = await api.get(`/patients/${p.id}/today-tasks`, { silent: true })
    } catch {
      t = { data: { prescription: { items: [] }, logSubmitted: false, todayLog: null, pendingCorrections: [], activeCorrections: [], suspendedItems: [], openEscalations: [] } }
    }
    setTasks(t.data)
    setPendingCorrections(t.data.pendingCorrections || [])
    setActiveCorrections(t.data.activeCorrections || [])
    setSuspendedItems(t.data.suspendedItems || [])
    setEscalations(t.data.openEscalations || [])
    if (t.data.todayLog) {
      const log = t.data.todayLog
      const map = {}
      parseJson(log.completedDetail).forEach((d) => { map[d.itemId] = !!d.done })
      setDoneMap(map)
      setPainBefore(log.painBefore ?? 2)
      setPainAfter(log.painAfter ?? 3)
      setSwelling(!!log.swellingNumbness)
      setNightPain(!!log.nightPainWorse)
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
  const threshold = tasks.prescription?.painThreshold ?? 6
  const escalationRisk = painAfter >= threshold || swelling || nightPain
  const doneItemIds = items.filter((i) => doneMap[i.id]).map((i) => i.id)
  const selectedPainItems = painItemIds ?? doneItemIds

  const submit = async () => {
    setSubmitting(true)
    try {
      await api.post(`/patients/${patient.id}/training-logs`, {
        completionRate,
        items: items.map((i) => ({ itemId: i.id, done: !!doneMap[i.id], actualSets: doneMap[i.id] ? i.targetSets : 0, actualReps: doneMap[i.id] ? i.targetReps : 0 })),
        painBefore,
        painAfter,
        swellingNumbness: swelling,
        nightPainWorse: nightPain,
        painItemIds: escalationRisk ? selectedPainItems : undefined,
        abnormalPhotos: photos,
        videoClips: videos,
        familyNote: note,
        companionAvailable: companion,
        compensationObserved: compensation
      })
      if (escalationRisk) {
        message.warning('已触发疼痛升级处置：相关动作已暂停，请立即补充症状信息', 5)
      } else {
        message.success('今日打卡已提交，治疗师会及时查看')
      }
      setPainItemIds(null)
      load()
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Row gutter={16}>
      <Col span={14}>
        {/* 疼痛升级：待家属补充症状（平台要求） */}
        {escalations.filter((e) => e.status === 'PENDING_FAMILY_INFO').map((esc) => (
          <FamilyReportCard key={esc.id} esc={esc} onDone={load} />
        ))}

        {/* 疼痛升级：护士评估中 / 待医生复核 */}
        {escalations.filter((e) => ['NURSE_ASSESSING', 'DOCTOR_REVIEW'].includes(e.status)).map((esc) => (
          <Alert
            key={esc.id}
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
            message={`疼痛升级处置进行中（${ESCALATION_STATUS[esc.status]?.label}）：「${esc.suspendedItemNames}」保持暂停`}
            description={esc.status === 'NURSE_ASSESSING'
              ? '症状信息已提交，康复护士将尽快电话评估，请保持电话畅通。'
              : '护士已完成电话评估并转医生复核，等待医生处置结论。'}
          />
        ))}

        {/* 疼痛升级：医生处置结论（同步家属） */}
        {escalations.filter((e) => e.status === 'DISPOSITION_ACTIVE').map((esc) => (
          <Alert
            key={esc.id}
            type="error"
            style={{ marginBottom: 16 }}
            message={
              <span><MedicineBoxOutlined /> 医生处置结论（{esc.doctor?.name}）：
                {(esc.doctorDispositions || '').split(',').filter(Boolean).map((d) => (
                  <Tag key={d} color="volcano" style={{ marginLeft: 4 }}>{DISPOSITION[d] || d}</Tag>
                ))}
                {esc.reviewDate && <Tag color="purple">复诊/检查：{esc.reviewDate}</Tag>}
              </span>
            }
            description={
              <div>
                <div>{esc.doctorConclusion}</div>
                <div style={{ marginTop: 4, color: '#cf1322' }}>
                  「{esc.suspendedItemNames}」仍暂停，医生解除风险后会自动恢复进每日任务，请勿自行训练。
                </div>
              </div>
            }
          />
        ))}

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
          {items.length === 0 && suspendedItems.length === 0 && <Empty description="当前没有执行中的处方，请联系治疗师" />}
          {items.length === 0 && suspendedItems.length > 0 && (
            <Alert type="error" showIcon style={{ marginBottom: 12 }}
              message="今日全部动作均已暂停" description="疼痛升级处置期间请遵医嘱休息，待医生解除风险后训练任务会自动恢复。" />
          )}
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

          {/* 疼痛升级暂停中的动作（解除风险前不进入每日任务） */}
          {suspendedItems.map((item) => (
            <div key={item.id} className="task-item" style={{ background: '#fff1f0', borderColor: '#ffa39e', opacity: 0.85 }}>
              <Checkbox checked={false} disabled>
                <b style={{ fontSize: 15, color: '#cf1322', textDecoration: 'line-through' }}>{item.exerciseName}</b>
              </Checkbox>
              <Tag icon={<PauseCircleOutlined />} color="error" style={{ marginLeft: 8 }}>已暂停</Tag>
              <div className="task-meta">因疼痛升级处置暂停，医生解除风险前不能训练该项</div>
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
              <div style={{ display: 'flex', gap: 32, margin: '8px 0 12px', flexWrap: 'wrap' }}>
                <span>家属能陪练：<Switch checked={companion} onChange={setCompanion} /></span>
                <span>发现明显动作代偿：<Switch checked={compensation} onChange={setCompensation} /></span>
                <span>出现肿胀/麻木：<Switch checked={swelling} onChange={setSwelling} /></span>
                <span>夜间疼痛加重：<Switch checked={nightPain} onChange={setNightPain} /></span>
              </div>
              {escalationRisk && (
                <Alert
                  style={{ marginBottom: 12 }}
                  type="error"
                  showIcon
                  message={
                    painAfter >= threshold
                      ? `训练后疼痛 ${painAfter} 分已达处方疼痛阈值（${threshold} 分）${swelling || nightPain ? '，且伴有' + [swelling && '肿胀/麻木', nightPain && '夜间痛加重'].filter(Boolean).join('、') : ''}`
                      : `已报告${[swelling && '肿胀/麻木', nightPain && '夜间痛加重'].filter(Boolean).join('、')}`
                  }
                  description={
                    <div>
                      <div style={{ marginBottom: 6 }}>提交后平台将<b>立即暂停相关动作</b>并启动疼痛升级处置（家属补充症状 → 护士电话评估 → 必要时医生复核）。请勾选引起疼痛/不适的动作：</div>
                      <Checkbox.Group
                        style={{ display: 'flex', flexDirection: 'column', gap: 4 }}
                        options={items.filter((i) => doneMap[i.id]).map((i) => ({ value: i.id, label: i.exerciseName }))}
                        value={selectedPainItems}
                        onChange={setPainItemIds}
                      />
                      {selectedPainItems.length === 0 && <div style={{ color: '#cf1322', marginTop: 4 }}>未选择动作时，平台将按疼痛阈值自动判定需暂停的动作</div>}
                    </div>
                  }
                />
              )}
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
                disabled={blocked} danger={escalationRisk} style={{ marginTop: 16 }}>
                {escalationRisk ? '提交并启动疼痛升级处置' : tasks.logSubmitted ? '更新今日打卡' : '提交今日打卡'}
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
                      {log.swellingNumbness && <Tag color="volcano">肿胀麻木</Tag>}
                      {log.nightPainWorse && <Tag color="purple">夜间痛</Tag>}
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

/** 疼痛升级：家属补充症状/用药/是否摔倒 */
function FamilyReportCard({ esc, onDone }) {
  const [symptoms, setSymptoms] = useState('')
  const [medication, setMedication] = useState('')
  const [fell, setFell] = useState(null)
  const [fellDetail, setFellDetail] = useState('')
  const [saving, setSaving] = useState(false)

  const triggers = (esc.triggers || '').split(',').filter(Boolean)

  const submit = async () => {
    if (!symptoms.trim()) return message.warning('请补充症状表现')
    if (!medication.trim()) return message.warning('请填写用药情况（无用药请填“未用药”）')
    if (fell === null) return message.warning('请说明患者是否摔倒')
    if (fell && !fellDetail.trim()) return message.warning('请补充摔倒经过')
    setSaving(true)
    try {
      await api.post(`/escalations/${esc.id}/family-report`, { symptoms, medication, fell, fellDetail })
      message.success('已提交，康复护士将尽快电话评估，请保持电话畅通')
      onDone()
    } finally {
      setSaving(false)
    }
  }

  return (
    <Alert
      type="error"
      style={{ marginBottom: 16 }}
      message={
        <span><WarningOutlined /> 疼痛升级处置：平台已暂停「{esc.suspendedItemNames}」，请立即补充以下信息
          {triggers.map((t) => <Tag key={t} color="red" style={{ marginLeft: 4 }}>{ESCALATION_TRIGGER[t] || t}</Tag>)}
          {esc.painScore != null && <Tag color="red">疼痛 {esc.painScore} 分</Tag>}
        </span>
      }
      description={
        <Card size="small" style={{ marginTop: 8, background: '#fff7f6' }}>
          <div style={{ marginBottom: 8 }}>
            <b>1. 症状表现</b>（肿胀/麻木部位、疼痛规律、是否影响睡眠等）
            <Input.TextArea rows={2} value={symptoms} onChange={(e) => setSymptoms(e.target.value)}
              placeholder="如：右肩肿胀，抬臂时疼痛加重，夜间不痛" style={{ marginTop: 4 }} />
          </div>
          <div style={{ marginBottom: 8 }}>
            <b>2. 用药情况</b>（止痛药/外用药，名称与剂量）
            <Input.TextArea rows={2} value={medication} onChange={(e) => setMedication(e.target.value)}
              placeholder="如：口服布洛芬 0.3g 每日 2 次；未用药请填“未用药”" style={{ marginTop: 4 }} />
          </div>
          <div style={{ marginBottom: 8 }}>
            <b>3. 患者是否摔倒？</b>
            <div style={{ marginTop: 4 }}>
              <Radio.Group value={fell} onChange={(e) => setFell(e.target.value)}>
                <Radio value={false}>没有摔倒</Radio>
                <Radio value={true}>有摔倒</Radio>
              </Radio.Group>
            </div>
            {fell === true && (
              <Input.TextArea rows={2} value={fellDetail} onChange={(e) => setFellDetail(e.target.value)}
                placeholder="请描述摔倒经过：时间、部位、是否磕碰头部、能否自行站起" style={{ marginTop: 4 }} />
            )}
          </div>
          <Button type="primary" danger loading={saving} onClick={submit}>提交症状信息（转护士电话评估）</Button>
        </Card>
      }
    />
  )
}
