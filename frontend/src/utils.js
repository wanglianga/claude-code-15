// 枚举标签与颜色工具
export const DISEASE_TYPE = {
  STROKE: '脑卒中',
  FRACTURE_POST_OP: '骨折术后',
  NECK_SHOULDER_PAIN: '颈肩腰腿痛',
  CHILD_DEVELOPMENT_DELAY: '儿童发育迟缓'
}

export const STAGE = {
  NEWLY_DISCHARGED: { label: '刚出院', color: 'geekblue', hint: '每日跟进：密切观察训练启动情况，第一周每日查看打卡' },
  STABLE_TRAINING: { label: '稳定训练', color: 'green', hint: '每周评估：按周查看依从性与疼痛曲线，按计划复诊' },
  RECURRENCE_WARNING: { label: '复发预警', color: 'red', hint: '立即处理：优先处理预警，必要时追加线下复诊或医生介入' }
}

export const ALERT_TYPE = {
  MISSED_TRAINING: '连续漏练',
  PAIN_RISE: '疼痛升高',
  COMPENSATION: '动作代偿明显',
  NO_COMPANION: '家属无法陪练',
  DOCTOR_REFERRAL: '转诊医生处理'
}

export const ALERT_STATUS = {
  PENDING: { label: '待随访', color: 'orange' },
  FOLLOWING: { label: '随访中', color: 'processing' },
  ESCALATED: { label: '已转医生', color: 'purple' },
  RESOLVED: { label: '已解决', color: 'success' }
}

export const ALERT_LEVEL = {
  LOW: { label: '低', color: 'default' },
  MEDIUM: { label: '中', color: 'orange' },
  HIGH: { label: '高', color: 'red' }
}

export const RISK = {
  LOW: { label: '低风险', color: 'green' },
  MEDIUM: { label: '中风险', color: 'orange' },
  HIGH: { label: '高风险', color: 'red' }
}

export const DECISION = {
  MAINTAIN: '维持原处方',
  REDUCE_INTENSITY: '降低训练强度',
  ADD_OFFLINE_VISIT: '追加线下复诊',
  DOCTOR_REFERRAL: '提醒医生介入'
}

export const EVENT_COLOR = {
  PATIENT_CREATED: 'blue',
  ASSESSMENT_RECORDED: 'geekblue',
  PRESCRIPTION_CREATED: 'green',
  PRESCRIPTION_ADJUSTED: 'orange',
  TRAINING_LOG_SUBMITTED: 'cyan',
  VIDEO_CORRECTED: 'purple',
  ALERT_CREATED: 'red',
  ALERT_ASSIGNED: 'gold',
  NURSE_FOLLOWUP: 'gold',
  ALERT_ESCALATED: 'volcano',
  DOCTOR_OPINION: 'magenta',
  ALERT_RESOLVED: 'green',
  OUTPATIENT_TREATMENT: 'blue',
  SETTLEMENT_CREATED: 'green',
  STAGE_CHANGED: 'orange',
  REVIEW_PLANNED: 'geekblue'
}

export const EVENT_LABEL = {
  PATIENT_CREATED: '建立档案',
  ASSESSMENT_RECORDED: '评估记录',
  PRESCRIPTION_CREATED: '开具处方',
  PRESCRIPTION_ADJUSTED: '处方调整决策',
  TRAINING_LOG_SUBMITTED: '居家训练打卡',
  VIDEO_CORRECTED: '视频纠错',
  ALERT_CREATED: '风险预警',
  ALERT_ASSIGNED: '护士接单',
  NURSE_FOLLOWUP: '护士电话随访',
  ALERT_ESCALATED: '转诊医生',
  DOCTOR_OPINION: '医生处理意见',
  ALERT_RESOLVED: '预警解除',
  OUTPATIENT_TREATMENT: '线下治疗记录',
  SETTLEMENT_CREATED: '医保结算',
  STAGE_CHANGED: '疾病阶段调整',
  REVIEW_PLANNED: '复诊计划'
}

export function parseJson(str, fallback = []) {
  if (!str) return fallback
  try { return JSON.parse(str) } catch { return fallback }
}

export function fileUrl(path) {
  return `/api/files/${path}`
}

export function age(birthDate) {
  if (!birthDate) return '-'
  const diff = Date.now() - new Date(birthDate).getTime()
  return Math.floor(diff / (365.25 * 24 * 3600 * 1000)) + ' 岁'
}
