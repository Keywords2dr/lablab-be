package com.keywords2dr.lablab.repository.specification;

import com.keywords2dr.lablab.entity.RoomStaffAssignment;
import com.keywords2dr.lablab.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserSpecification {

    public static Specification<User> filter(String role, String keyword, Boolean isActive) {
        return filter(role, keyword, isActive, null);
    }

    public static Specification<User> filter(String role, String keyword, Boolean isActive, Boolean unassigned) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            boolean isCountQuery = Long.class.equals(query.getResultType());

            Join<Object, Object> profile;
            if (isCountQuery) {
                profile = root.join("profile", JoinType.LEFT);
            } else {
                profile = (Join<Object, Object>) root.fetch("profile", JoinType.LEFT);
                query.distinct(true);
            }

            predicates.add(cb.notEqual(root.get("role"), "ADMIN"));

            if (role != null && !role.isBlank()) {
                predicates.add(cb.equal(root.get("role"), role.toUpperCase().trim()));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.toLowerCase().trim() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")),    kw),
                        cb.like(cb.lower(profile.get("fullName")), kw),
                        cb.like(cb.lower(profile.get("email")),    kw)
                ));
            }

            if (Boolean.TRUE.equals(unassigned)) {
                Subquery<UUID> subquery = query.subquery(UUID.class);
                var assignRoot = subquery.from(RoomStaffAssignment.class);
                subquery.select(assignRoot.get("user").get("userId"));
                predicates.add(cb.not(root.get("userId").in(subquery)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}