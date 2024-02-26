package de.tum.in.www1.artemis.repository;

import static de.tum.in.www1.artemis.config.Constants.PROFILE_CORE;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.tum.in.www1.artemis.domain.AuxiliaryRepository;

/**
 * Spring Data repository for the AuxiliaryRepository entity.
 */
@Profile(PROFILE_CORE)
@Repository
public interface AuxiliaryRepositoryRepository extends JpaRepository<AuxiliaryRepository, Long> {

    List<AuxiliaryRepository> findByExerciseId(Long exerciseId);

}
