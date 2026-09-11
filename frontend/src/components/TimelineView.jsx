import React from 'react'
import { Timeline, Tag, Empty } from 'antd'
import { EVENT_COLOR, EVENT_LABEL } from '../utils'

/** 患者时间线：串联治疗师/护士/家属/医生的全部处理记录 */
export default function TimelineView({ events }) {
  if (!events || events.length === 0) return <Empty description="暂无时间线记录" style={{ padding: 40 }} />
  return (
    <Timeline
      style={{ marginTop: 16 }}
      items={events.map((e) => ({
        color: EVENT_COLOR[e.eventType] || 'blue',
        children: (
          <div>
            <div>
              <Tag color={EVENT_COLOR[e.eventType]}>{EVENT_LABEL[e.eventType] || e.eventType}</Tag>
              <b>{e.title}</b>
            </div>
            {e.content && <div className="timeline-content" style={{ marginTop: 4 }}>{e.content}</div>}
            <div style={{ color: '#999', fontSize: 12, marginTop: 4 }}>
              {e.createdAt} ｜ {e.actorName}（{e.actorRole}）
            </div>
          </div>
        )
      }))}
    />
  )
}
