package com.rehab.platform.service;

import com.rehab.platform.config.SecurityUtils;
import com.rehab.platform.dto.Dtos;
import com.rehab.platform.enums.Role;
import com.rehab.platform.enums.TimelineEventType;
import com.rehab.platform.model.Assessment;
import com.rehab.platform.model.Patient;
import com.rehab.platform.repository.AssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final PatientService patientService;
    private final TimelineService timelineService;

    public List<Assessment> listByPatient(Long patientId) {
        patientService.getAccessible(patientId);
        return assessmentRepository.findByPatientIdOrderByAssessmentDateDesc(patientId);
    }

    @Transactional
    public Assessment create(Long patientId, Dtos.AssessmentRequest req) {
        SecurityUtils.requireRole(Role.THERAPIST, Role.ADMIN);
        Patient patient = patientService.getAccessible(patientId);
        Assessment a = new Assessment();
        a.setPatient(patient);
        a.setTherapist(SecurityUtils.currentUser());
        a.setType(req.type());
        a.setRomJson(req.romJson());
        a.setMuscleStrength(req.muscleStrength());
        a.setPainScore(req.painScore());
        a.setAdlScore(req.adlScore());
        a.setGaitVideoPath(req.gaitVideoPath());
        a.setNotes(req.notes());
        a.setAssessmentDate(req.assessmentDate() == null ? LocalDate.now() : req.assessmentDate());
        Assessment saved = assessmentRepository.save(a);
        timelineService.record(patient, TimelineEventType.ASSESSMENT_RECORDED, SecurityUtils.currentUser(),
                req.type().getLabel() + "：疼痛 " + nullToDash(req.painScore()) + " 分，肌力 "
                        + nullToDash(req.muscleStrength()) + " 级",
                req.notes(), "assessment", saved.getId());
        return saved;
    }

    private String nullToDash(Integer v) {
        return v == null ? "—" : String.valueOf(v);
    }
}
