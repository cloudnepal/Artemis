package de.tum.in.www1.artemis.service.iris.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record IrisCombinedLectureIngestionSubSettingsDTO(boolean enabled) {
}
