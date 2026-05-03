package com.keywords2dr.lablab.repository.specification;

import com.keywords2dr.lablab.entity.Chemical;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

public class ChemicalSpecification {

    public static Specification<Chemical> filter(String keyword, String packaging, String supplier, String unit, String category) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.isFalse(root.get("isDeleted")));

            if (keyword != null) {
                String searchKeyword = "%" + keyword.toLowerCase() + "%";
                Predicate nameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchKeyword);
                Predicate codeMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("itemCode")), searchKeyword);
                predicates.add(criteriaBuilder.or(nameMatch, codeMatch));
            }

            if (packaging != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("packaging")),
                        packaging.toLowerCase()
                ));
            }
            if (supplier != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("supplier")),
                        supplier.toLowerCase()
                ));
            }
            if (unit != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("unit")),
                        unit.toLowerCase()
                ));
            }
            if (category != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("categoryType")),
                        category.toLowerCase()
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}