import React, { useState } from 'react'
import { Upload, Button } from 'antd'
import { UploadOutlined } from '@ant-design/icons'
import api from '../api'

/** 文件上传按钮：上传成功后把后端返回的 path 放进列表 */
export default function FileUpload({ value = [], onChange, accept, label, max = 4 }) {
  const [uploading, setUploading] = useState(false)

  const customRequest = async ({ file, onSuccess, onError }) => {
    const form = new FormData()
    form.append('file', file)
    setUploading(true)
    try {
      const res = await api.post('/files', form, { headers: { 'Content-Type': 'multipart/form-data' } })
      onChange?.([...(value || []), res.data.path])
      onSuccess(res.data)
    } catch (e) {
      onError(e)
    } finally {
      setUploading(false)
    }
  }

  return (
    <div>
      <Upload customRequest={customRequest} showUploadList={false} accept={accept} disabled={uploading || (value || []).length >= max}>
        <Button icon={<UploadOutlined />} loading={uploading}>{label || '上传文件'}</Button>
      </Upload>
      {(value || []).length > 0 && (
        <div style={{ marginTop: 6, fontSize: 12, color: '#52c41a' }}>
          已上传 {value.length} 个文件
          <Button type="link" size="small" onClick={() => onChange?.([])}>清空</Button>
        </div>
      )}
    </div>
  )
}
