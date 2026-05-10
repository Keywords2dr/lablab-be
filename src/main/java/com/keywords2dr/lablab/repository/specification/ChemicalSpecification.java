package com.keywords2dr.lablab.repository.specification;

import com.keywords2dr.lablab.entity.Chemical;
import com.keywords2dr.lablab.exception.BadRequestException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ChemicalSpecification {

    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_FILTER_LENGTH = 100;

    public static Specification<Chemical> filter(
            String keyword,
            String packaging,
            String supplier,
            String unit,
            String category
    ) {
        validateLengths(keyword, packaging, supplier, unit, category);

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("isDeleted"), false));

            if (keyword != null) {

                String escaped = escapeLikePattern(keyword);
                String searchPattern = "%" + escaped.toLowerCase() + "%";

                Predicate nameMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")), searchPattern);
                Predicate codeMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("itemCode")), searchPattern);

                Predicate formulaMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(
                                criteriaBuilder.coalesce(root.get("formula"), "")),
                        searchPattern);

                predicates.add(criteriaBuilder.or(nameMatch, codeMatch, formulaMatch));
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

    private static void validateLengths(
            String keyword, String packaging, String supplier, String unit, String category) {
        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            throw new BadRequestException(
                    String.format("Từ khóa tìm kiếm không được vượt quá %d ký tự!", MAX_KEYWORD_LENGTH));
        }
        if (packaging != null && packaging.length() > MAX_FILTER_LENGTH) {
            throw new BadRequestException(
                    String.format("Tham số 'packaging' không được vượt quá %d ký tự!", MAX_FILTER_LENGTH));
        }
        if (supplier != null && supplier.length() > MAX_FILTER_LENGTH) {
            throw new BadRequestException(
                    String.format("Tham số 'supplier' không được vượt quá %d ký tự!", MAX_FILTER_LENGTH));
        }
        if (unit != null && unit.length() > MAX_FILTER_LENGTH) {
            throw new BadRequestException(
                    String.format("Tham số 'unit' không được vượt quá %d ký tự!", MAX_FILTER_LENGTH));
        }
        if (category != null && category.length() > MAX_FILTER_LENGTH) {
            throw new BadRequestException(
                    String.format("Tham số 'category' không được vượt quá %d ký tự!", MAX_FILTER_LENGTH));
        }
    }

    private static String escapeLikePattern(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}