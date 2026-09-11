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
  VIDEO_CORRECTION: '连续纠错未掌握',
  DOCTOR_REFERRAL: '转诊医生处理',
  PAIN_ESCALATION: '疼痛升级'
}

export const ESCALATION_TRIGGER = {
  PAIN_OVER_THRESHOLD: '疼痛超阈值',
  SWELLING_NUMBNESS: '肿胀/麻木',
  NIGHT_PAIN_WORSE: '夜间痛加重'
}

export const ESCALATION_STATUS = {
  PENDING_FAMILY_INFO: { label: '待家属补充', color: 'orange' },
  NURSE_ASSESSING: { label: '护士评估中', color: 'processing' },
  DOCTOR_REVIEW: { label: '待医生复核', color: 'purple' },
  DISPOSITION_ACTIVE: { label: '医生已处置·暂停中', color: 'volcano' },
  CLEARED: { label: '风险已解除', color: 'success' }
}

export const DISPOSITION = {
  REST: '休息',
  ICE: '冰敷',
  IMAGING: '影像检查',
  OUTPATIENT: '门诊复诊'
}

export const NURSE_DECISION = {
  OBSERVE_RESUME: '继续观察，恢复训练',
  ESCALATE_DOCTOR: '转医生复核'
}

export const CORRECTION_STATUS = {
  PENDING_CONFIRM: { label: '待患者确认', color: 'orange' },
  CONFIRMED: { label: '已确认，待下次视频', color: 'blue' },
  RECHECK: { label: '新视频待复评', color: 'purple' },
  MASTERED: { label: '已掌握', color: 'green' },
  NOT_MASTERED: { label: '未掌握，继续纠正', color: 'red' }
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
  REVIEW_PLANNED: 'geekblue',
  CORRECTION_CREATED: 'volcano',
  CORRECTION_CONFIRMED: 'cyan',
  CORRECTION_RECHECK: 'purple',
  CORRECTION_REVIEWED: 'magenta',
  PAIN_ESCALATION_CREATED: 'red',
  ESCALATION_FAMILY_REPORT: 'orange',
  ESCALATION_NURSE_ASSESSMENT: 'gold',
  ESCALATION_DOCTOR_DISPOSITION: 'magenta',
  ESCALATION_CLEARED: 'green'
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
  REVIEW_PLANNED: '复诊计划',
  CORRECTION_CREATED: '视频打回纠错',
  CORRECTION_CONFIRMED: '确认观看纠错',
  CORRECTION_RECHECK: '复评视频已提交',
  CORRECTION_REVIEWED: '纠错复评结论',
  PAIN_ESCALATION_CREATED: '疼痛升级·暂停动作',
  ESCALATION_FAMILY_REPORT: '家属补充症状',
  ESCALATION_NURSE_ASSESSMENT: '护士电话评估',
  ESCALATION_DOCTOR_DISPOSITION: '医生处置结论',
  ESCALATION_CLEARED: '风险解除·恢复训练'
}

export function parseJson(str, fallback = []) {
  if (!str) return fallback
  try { return JSON.parse(str) } catch { return fallback }
}

/** 归一化列表接口返回：兼容数组与单对象两种结构（家属端 /patients 可能返回单个患者对象） */
export function asArray(data) {
  if (!data) return []
  return Array.isArray(data) ? data : [data]
}

export function fileUrl(path) {
  return `/api/files/${path}`
}

export function age(birthDate) {
  if (!birthDate) return '-'
  const diff = Date.now() - new Date(birthDate).getTime()
  return Math.floor(diff / (365.25 * 24 * 3600 * 1000)) + ' 岁'
}
