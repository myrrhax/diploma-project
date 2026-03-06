package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VersionRepository extends CrudRepository<VersionEntity, Long> {
    Optional<VersionEntity> findBySchemeIdAndHashSum(UUID schemeId, String hashSum);
    boolean existsBySchemeIdAndTag(UUID schemeId, String tag);
    @Query("select v from VersionEntity v where v.scheme.id = :schemeId order by v.versionedAt nulls last")
    List<VersionEntity> findAllBySchemeId(UUID schemeId);
}
