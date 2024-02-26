package de.tum.in.www1.artemis.repository;

import static de.tum.in.www1.artemis.config.Constants.PROFILE_CORE;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.tum.in.www1.artemis.domain.modeling.ModelCluster;

/**
 * Spring Data JPA repository for the ModelCluster entity.
 */
@Profile(PROFILE_CORE)
@Repository
public interface ModelClusterRepository extends JpaRepository<ModelCluster, Long> {

    @Query("""
            SELECT COUNT (DISTINCT cluster)
            FROM ModelCluster cluster
            WHERE cluster.exercise.id = :exerciseId
            """)
    Integer countByExerciseIdWithEagerElements(@Param("exerciseId") Long exerciseId);

    @Query("""
            SELECT DISTINCT cluster
            FROM ModelCluster cluster
                LEFT JOIN FETCH cluster.modelElements element
            WHERE cluster.exercise.id = :exerciseId
            """)
    List<ModelCluster> findAllByExerciseIdWithEagerElements(@Param("exerciseId") Long exerciseId);

    @Query("""
            SELECT DISTINCT cluster
            FROM ModelCluster cluster
                LEFT JOIN FETCH cluster.modelElements element
            WHERE cluster.id IN :clusterIds
            """)
    List<ModelCluster> findAllByIdInWithEagerElements(@Param("clusterIds") List<Long> clusterIds);
}
