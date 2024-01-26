package de.tum.in.www1.artemis.web.rest.dto.competency;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.tum.in.www1.artemis.domain.competency.CompetencyRelation;

/**
 * DTO containing {@link CompetencyRelation} data. It only contains ids of the linked competencies to reduce data sent.
 *
 * @param tailCompetencyId the id of the tail competency
 * @param headCompetencyId the id of the head competency
 * @param relationType     the relation type
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record CompetencyRelationDTO(long id, long tailCompetencyId, long headCompetencyId, CompetencyRelation.RelationType relationType) {

    public CompetencyRelationDTO(CompetencyRelation competencyRelation) {
        this(competencyRelation.getId(), competencyRelation.getTailCompetency().getId(), competencyRelation.getHeadCompetency().getId(), competencyRelation.getType());
    }
}
