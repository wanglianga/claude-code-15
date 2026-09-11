import React from 'react'
import { Tag } from 'antd'
import { parseJson, fileUrl, CORRECTION_STATUS } from '../utils'

/** 关键动作点标签（含时间点与问题描述） */
export function KeyPointTags({ keyPoints }) {
  const points = parseJson(keyPoints)
  if (!points.length) return null
  return (
    <div style={{ margin: '4px 0' }}>
      {points.map((p, i) => (
        <Tag key={i} color="volcano" style={{ marginBottom: 4 }}>
          {p.label || p.code}{p.timestamp ? ` [${p.timestamp}]` : ''}{p.note ? `：${p.note}` : ''}
        </Tag>
      ))}
    </div>
  )
}

export function CorrectionStatusTag({ status }) {
  const meta = CORRECTION_STATUS[status] || {}
  return <Tag color={meta.color}>{meta.label || status}</Tag>
}

/** 竖排视频列表 */
export function VideoList({ videosJson, maxWidth = 300 }) {
  const videos = parseJson(videosJson)
  if (!videos.length) return <div style={{ color: '#999', fontSize: 12, padding: '12px 0' }}>（无视频文件）</div>
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
      {videos.map((v) => (
        <video key={v} src={fileUrl(v)} controls preload="metadata"
          style={{ maxWidth, width: '100%', borderRadius: 6, background: '#000' }} />
      ))}
    </div>
  )
}
