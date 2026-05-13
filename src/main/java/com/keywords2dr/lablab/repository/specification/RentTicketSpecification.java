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
}