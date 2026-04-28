package com.github.myrrhax.diploma_project.repository.specification;

import com.github.myrrhax.diploma_project.model.entity.AuthorityEntity;
import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface SchemeSpecification {
    static Specification<SchemeEntity> findSchemesFiltered(boolean takeParticipation,
                                                           String searchQuery,
                                                           UUID userId) {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class) {
                root.fetch("creator", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();
            if (takeParticipation) {
                query.distinct(true);

                Join<SchemeEntity, AuthorityEntity> authority =
                        root.join("userAuthorities", JoinType.INNER);

                predicates.add(
                        cb.equal(authority.get("user").get("id"), userId)
                );
            } else {
                predicates.add(
                        cb.equal(root.get("creator").get("id"), userId)
                );
            }

            if (searchQuery != null && !searchQuery.isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("name")),
                                "%" + searchQuery.toLowerCase() + "%"
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
