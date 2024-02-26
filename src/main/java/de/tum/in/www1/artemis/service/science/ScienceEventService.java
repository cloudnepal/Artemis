package de.tum.in.www1.artemis.service.science;

import static de.tum.in.www1.artemis.config.Constants.PROFILE_CORE;

import java.time.ZonedDateTime;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.science.ScienceEvent;
import de.tum.in.www1.artemis.repository.science.ScienceEventRepository;
import de.tum.in.www1.artemis.web.rest.dto.science.ScienceEventDTO;

/**
 * Service class for {@link ScienceEvent}.
 */
@Profile(PROFILE_CORE)
@Service
public class ScienceEventService {

    private final ScienceEventRepository scienceEventRepository;

    public ScienceEventService(ScienceEventRepository scienceEventRepository) {
        this.scienceEventRepository = scienceEventRepository;
    }

    /**
     * Logs the event for the current principal with the current timestamp.
     *
     * @param eventDTO the DTO of the event that should be logged
     */
    public void logEvent(ScienceEventDTO eventDTO) {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logEvent(eventDTO, auth.getName());
    }

    /**
     * Logs the event for the given principal with the current timestamp.
     *
     * @param eventDTO  the DTO of the event that should be logged
     * @param principal the name of the principal for whom the event should be logged
     */
    private void logEvent(ScienceEventDTO eventDTO, String principal) {
        logEvent(eventDTO, principal, ZonedDateTime.now());
    }

    /**
     * Logs the event for the given principal with the given timestamp.
     *
     * @param eventDTO  the DTO of the event that should be logged
     * @param principal the name of the principal for whom the event should be logged
     * @param timestamp the time when the event happened
     */
    private void logEvent(ScienceEventDTO eventDTO, String principal, ZonedDateTime timestamp) {
        ScienceEvent event = new ScienceEvent();
        event.setIdentity(principal);
        event.setTimestamp(timestamp);
        event.setType(eventDTO.type());
        event.setResourceId(eventDTO.resourceId());
        scienceEventRepository.save(event);
    }
}
