package com.keywords2dr.lablab.repository.specification;

import com.keywords2dr.lablab.entity.Room;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

public class RoomSpecification {

    public static Specification<Room> filter(String keyword, Boolean isActive) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchKeyword = "%" + keyword.toLowerCase().trim() + "%";
                Predicate nameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("roomName")), searchKeyword);
                Predicate descMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchKeyword);
                predicates.add(criteriaBuilder.or(nameMatch, descMatch));
            }

            if (isActive != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), isActive));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}