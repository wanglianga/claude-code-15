import React, { useEffect, useState } from 'react'
import { Row, Col, Card, Tag, Badge, Empty, Statistic } from 'antd'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import { STAGE, RISK } from '../utils'

/** 风险看板：按疾病阶段（刚出院 / 稳定训练 / 复发预警）分组，不同处理节奏 */
export default function RiskBoard() {
  const [board, setBoard] = useState(null)
  const navigate = useNavigate()

  useEffect(() => {
    api.get('/dashboard/risk-board').then((res) => setBoard(res.data))
  }, [])

  if (!board) return null

  return (
    <div>
      <Row gutter={16}>
        {Object.entries(STAGE).map(([stage, meta]) => {
          const patients = board[stage] || []
          return (
            <Col span={8} key={stage}>
              <div className="stage-col">
                <div className="stage-col-title">
                  <Badge color={meta.color === 'green' ? '#52c41a' : meta.color === 'red' ? '#f5222d' : '#2f54eb'} />
                  {' '}{meta.label}（{patients.length}）
                </div>
                <div className="stage-col-hint">处理节奏：{meta.hint}</div>
                {patients.length === 0 && <Empty description="暂无患者" image={Empty.PRESENTED_IMAGE_SIMPLE} />}
                {patients.map((p) => (
                  <Card key={p.id} size="small" className="patient-card" onClick={() => navigate(`/patients/${p.id}`)}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <b>{p.name}</b>
                      <Tag color={RISK[p.risk.level]?.color}>{RISK[p.risk.level]?.label}</Tag>
                    </div>
                    <div style={{ fontSize: 12, color: '#888', margin: '4px 0' }}>
                      {p.patientNo} ｜ {p.diseaseType} ｜ 治疗师：{p.therapist || '-'}
                    </div>
                    <Row gutter={8} style={{ textAlign: 'center' }}>
                      <Col span={8}><Statistic title="7天依从性" value={p.risk.adherence7d} suffix="%" valueStyle={{ fontSize: 16 }} /></Col>
                      <Col span={8}><Statistic title="连续漏练" value={p.risk.missedDays} suffix="天" valueStyle={{ fontSize: 16, color: p.risk.missedDays >= 2 ? '#cf1322' : undefined }} /></Col>
                      <Col span={8}><Statistic title="未处理预警" value={p.risk.openAlerts} suffix="条" valueStyle={{ fontSize: 16, color: p.risk.openAlerts > 0 ? '#cf1322' : undefined }} /></Col>
                    </Row>
                    {p.risk.reasons.length > 0 && (
                      <div style={{ fontSize: 12, color: '#cf1322', marginTop: 6 }}>
                        {p.risk.reasons.map((r, i) => <div key={i}>· {r}</div>)}
                      </div>
                    )}
                    {p.nextReviewDate && <div style={{ fontSize: 12, color: '#1677ff', marginTop: 4 }}>下次复诊：{p.nextReviewDate}</div>}
                  </Card>
                ))}
              </div>
            </Col>
          )
        })}
      </Row>
    </div>
  )
}
