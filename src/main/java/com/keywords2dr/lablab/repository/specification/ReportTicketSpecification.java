package com.keywords2dr.lablab.repository.specification;

import com.keywords2dr.lablab.entity.ReportTicket;
import com.keywords2dr.lablab.entity.enums.ReportType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ReportTicketSpecification {

    private ReportTicketSpecification() {}

    /**
     * Specification chỉ dùng để lọc (WHERE clause).
     * KHÔNG fetch join ở đây để tránh lỗi Hibernate HHH90003004:
     * "firstResult/maxResults specified with collection fetch; applying in memory"
     * khi kết hợp với Pageable.
     *
     * Việc eager-load các relation được xử lý riêng ở Repository
     * bằng @EntityGraph hoặc JOIN FETCH query.
     */
    public static Specification<ReportTicket> filter(ReportType reportType, UUID roomId, UUID itemId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (reportType != null) {
                predicates.add(cb.equal(root.get("reportType"), reportType));
            }

            if (roomId != null) {
                predicates.add(cb.equal(root.get("room").get("roomId"), roomId));
            }

            if (itemId != null) {
                predicates.add(cb.equal(root.get("item").get("itemId"), itemId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}