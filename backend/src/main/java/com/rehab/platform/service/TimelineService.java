package com.rehab.platform.service;

import com.rehab.platform.enums.TimelineEventType;
import com.rehab.platform.model.Patient;
import com.rehab.platform.model.TimelineEvent;
import com.rehab.platform.model.User;
import com.rehab.platform.repository.TimelineEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimelineService {

    private final TimelineEventRepository timelineEventRepository;

    /** 记录时间线事件（actor 可空表示系统事件） */
    public TimelineEvent record(Patient patient, TimelineEventType type, User actor,
                                String title, String content, String refType, Long refId) {
        TimelineEvent event = new TimelineEvent();
        event.setPatient(patient);
        event.setEventType(type);
        if (actor != null) {
            event.setActorId(actor.getId());
            event.setActorName(actor.getName());
            event.setActorRole(actor.getRole().getLabel());
        } else {
            event.setActorName("系统");
            event.setActorRole("系统");
        }
        event.setTitle(title);
        event.setContent(content);
        event.setRefType(refType);
        event.setRefId(refId);
        return timelineEventRepository.save(event);
    }

    public List<TimelineEvent> listByPatient(Long patientId) {
        return timelineEventRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }
}
