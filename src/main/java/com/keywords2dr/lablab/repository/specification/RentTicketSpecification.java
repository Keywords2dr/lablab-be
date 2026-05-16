package com.keywords2dr.lablab.repository.specification;

import com.keywords2dr.lablab.entity.RentTicket;
import com.keywords2dr.lablab.entity.enums.TicketStatus;
import com.keywords2dr.lablab.entity.enums.TicketType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RentTicketSpecification {

    /**
     * Admin filter: theo roomId, status, ticketType, requesterId.
     * Tất cả tham số đều optional — null thì bỏ qua.
     */
    public static Specification<RentTicket> filter(
            UUID roomId,
            TicketStatus status,
            TicketType ticketType,
            UUID requesterId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (roomId != null) {
                predicates.add(cb.equal(root.get("fromRoom").get("roomId"), roomId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (ticketType != null) {
                predicates.add(cb.equal(root.get("ticketType"), ticketType));
            }
            if (requesterId != null) {
                predicates.add(cb.equal(root.get("requester").get("userId"), requesterId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Requester filter: lấy phiếu của chính mình,
     * loại trừ một số status (excludedStatuses) và lọc theo ticketType (optional).
     */
    public static Specification<RentTicket> filterForRequester(
            UUID requesterId,
            List<TicketStatus> excludedStatuses,
            TicketType ticketType
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("requester").get("userId"), requesterId));

            // Loại trừ các status không mong muốn
            if (excludedStatuses != null && !excludedStatuses.isEmpty()) {
                predicates.add(root.get("status").in(excludedStatuses).not());
            }

            // Lọc theo loại phiếu nếu có
            if (ticketType != null) {
                predicates.add(cb.equal(root.get("ticketType"), ticketType));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}