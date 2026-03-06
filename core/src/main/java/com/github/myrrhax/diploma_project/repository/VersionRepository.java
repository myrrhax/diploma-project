package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VersionRepository extends CrudRepository<VersionEntity, Long> {
    Optional<VersionEntity> findBySchemeIdAndHashSum(UUID schemeId, String hashSum);
    boolean existsBySchemeIdAndTag(UUID schemeId, String tag);
    @Query("select v from VersionEntity v where v.scheme.id = :schemeId order by v.versionedAt nulls last")
    List<VersionEntity> findAllBySchemeId(UUID schemeId);

    @Modifying
    @Query("update VersionEntity v set v.parent = :newParent where v.parent.id = :id")
    void updateParentForChildren(@Param("id") Long id, @Param("newParent") VersionEntity parent);
}
