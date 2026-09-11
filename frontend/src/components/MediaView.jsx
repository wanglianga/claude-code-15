import React from 'react'
import { Image } from 'antd'
import { parseJson, fileUrl } from '../utils'

/** 展示打卡中的照片与视频 */
export default function MediaView({ photos, videos }) {
  const photoList = parseJson(photos)
  const videoList = parseJson(videos)
  if (photoList.length === 0 && videoList.length === 0) return null
  return (
    <div className="media-grid">
      {photoList.map((p) => (
        <Image key={p} src={fileUrl(p)} width={110} height={82} style={{ objectFit: 'cover', borderRadius: 6 }} />
      ))}
      {videoList.map((v) => (
        <video key={v} src={fileUrl(v)} controls preload="metadata" />
      ))}
    </div>
  )
}
