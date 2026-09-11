import React, { useEffect, useState } from 'react'
import { Row, Col, Card, Statistic, Tag, List, Empty, Badge } from 'antd'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import { STAGE, RISK } from '../utils'

/** 治疗师工作台：统计 + 按疾病阶段分组的患者卡片 + 近 7 天复诊计划 */
export default function Dashboard() {
  const [data, setData] = useState(null)
  const navigate = useNavigate()

  useEffect(() => {
    api.get('/dashboard/summary').then((res) => setData(res.data)).catch(() => {})
  }, [])

  if (!data) return null

  const stageGroups = ['NEWLY_DISCHARGED', 'STABLE_TRAINING', 'RECURRENCE_WARNING']

  return (
    <div>
      <Row gutter={16}>
        <Col flex="1"><Card className="stat-card"><Statistic title="我的患者" value={data.patientCount} suffix="人" /></Card></Col>
        <Col flex="1"><Card className="stat-card"><Statistic title="未处理预警" value={data.openAlertCount} suffix="条" valueStyle={{ color: data.openAlertCount > 0 ? '#cf1322' : undefined }} /></Card></Col>
        <Col flex="1"><Card className="stat-card"><Statistic title="疼痛升级处置中" value={data.openEscalationCount ?? 0} suffix="起" valueStyle={{ color: data.openEscalationCount > 0 ? '#fa541c' : undefined }} /></Card></Col>
        <Col flex="1"><Card className="stat-card"><Statistic title="7 天内待复诊" value={data.upcomingReviews.length} suffix="人" /></Card></Col>
        <Col flex="1"><Card className="stat-card"><Statistic title="刚出院 / 稳定 / 预警" value={`${data.stageCounts.NEWLY_DISCHARGED || 0} / ${data.stageCounts.STABLE_TRAINING || 0} / ${data.stageCounts.RECURRENCE_WARNING || 0}`} /></Card></Col>
      </Row>

      <Card title="近 7 天复诊计划" size="small" style={{ marginTop: 16 }}>
        {data.upcomingReviews.length === 0 ? <Empty description="暂无复诊计划" image={Empty.PRESENTED_IMAGE_SIMPLE} /> : (
          <List
            size="small"
            dataSource={data.upcomingReviews}
            renderItem={(r) => (
              <List.Item style={{ cursor: 'pointer' }} onClick={() => navigate(`/patients/${r.id}/review`)}>
                <span><Tag color="blue">{r.nextReviewDate}</Tag>{r.name}（{r.diseaseType}）— 点击查看复诊工作台</span>
              </List.Item>
            )}
          />
        )}
      </Card>

      <Row gutter={16} style={{ marginTop: 16 }}>
        {stageGroups.map((stage) => {
          const patients = (data.patients || []).filter((p) => p.diseaseStage === stage)
          const meta = STAGE[stage]
          return (
            <Col span={8} key={stage}>
              <div className="stage-col">
                <div className="stage-col-title"><Badge color={meta.color === 'green' ? '#52c41a' : meta.color === 'red' ? '#f5222d' : '#2f54eb'} /> {meta.label}（{patients.length}）</div>
                <div className="stage-col-hint">{meta.hint}</div>
                {patients.map((p) => (
                  <Card key={p.id} size="small" className="patient-card" onClick={() => navigate(`/patients/${p.id}`)}>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <b>{p.name}</b>
                      <Tag color={RISK[p.risk.level]?.color}>{RISK[p.risk.level]?.label}</Tag>
                    </div>
                    <div style={{ fontSize: 12, color: '#888', marginTop: 4 }}>
                      {p.patientNo} ｜ {p.diseaseType}
                      {p.nextReviewDate && <span> ｜ 复诊 {p.nextReviewDate}</span>}
                    </div>
                    {p.risk.reasons.length > 0 && (
                      <div style={{ fontSize: 12, color: '#cf1322', marginTop: 4 }}>{p.risk.reasons[0]}</div>
                    )}
                  </Card>
                ))}
                {patients.length === 0 && <Empty description="暂无患者" image={Empty.PRESENTED_IMAGE_SIMPLE} />}
              </div>
            </Col>
          )
        })}
      </Row>
    </div>
  )
}
