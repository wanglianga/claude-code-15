import React, { useEffect, useRef } from 'react'
import * as echarts from 'echarts'
import { Empty } from 'antd'

/** 疼痛曲线 + 完成率图 */
export default function PainChart({ data, height = 300 }) {
  const ref = useRef(null)

  useEffect(() => {
    if (!ref.current || !data || data.length === 0) return
    const chart = echarts.init(ref.current)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['训练前疼痛', '训练后疼痛', '完成率%'] },
      grid: { left: 40, right: 50, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: data.map((d) => d.date) },
      yAxis: [
        { type: 'value', name: '疼痛(VAS)', min: 0, max: 10 },
        { type: 'value', name: '完成率%', min: 0, max: 100 }
      ],
      series: [
        { name: '训练前疼痛', type: 'line', smooth: true, data: data.map((d) => d.painBefore), itemStyle: { color: '#1677ff' } },
        { name: '训练后疼痛', type: 'line', smooth: true, data: data.map((d) => d.painAfter), itemStyle: { color: '#cf1322' }, areaStyle: { opacity: 0.08 } },
        { name: '完成率%', type: 'bar', yAxisIndex: 1, data: data.map((d) => d.completionRate), itemStyle: { color: '#95de64' }, barMaxWidth: 14 }
      ]
    })
    const onResize = () => chart.resize()
    window.addEventListener('resize', onResize)
    return () => { window.removeEventListener('resize', onResize); chart.dispose() }
  }, [data])

  if (!data || data.length === 0) return <Empty description="暂无打卡数据" style={{ padding: 40 }} />
  return <div ref={ref} style={{ width: '100%', height }} />
}
