package com.keywords2dr.lablab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keywords2dr.lablab.entity.AuditLog;
import com.keywords2dr.lablab.entity.User;
import com.keywords2dr.lablab.repository.AuditLogRepository;
import com.keywords2dr.lablab.repository.UserRepository;
import com.keywords2dr.lablab.repository.specification.AuditLogSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRED)
    public void logAction(String action, String entityName, UUID recordId, Object oldObj, Object newObj) {
        try {
            UUID currentActorId = null;
            String currentActorUsername = "System";
            String currentActorRole = "SYSTEM";

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                currentActorUsername = auth.getName();

                if (!auth.getAuthorities().isEmpty()) {
                    GrantedAuthority authority = auth.getAuthorities().iterator().next();
                    currentActorRole = authority.getAuthority().replace("ROLE_", "").toUpperCase();
                }

                User currentUser = userRepository.findByUsername(currentActorUsername).orElse(null);
                if (currentUser != null) {
                    currentActorId = currentUser.getUserId();
                }
            }

            String oldDataJson = (oldObj != null) ? objectMapper.writeValueAsString(oldObj) : null;
            String newDataJson = (newObj != null) ? objectMapper.writeValueAsString(newObj) : null;

            AuditLog logEntry = AuditLog.builder()
                    .actorId(currentActorId)
                    .actorUsername(currentActorUsername)
                    .actorRole(currentActorRole)
                    .action(action)
                    .entityName(entityName)
                    .recordId(recordId)
                    .oldData(oldDataJson)
                    .newData(newDataJson)
                    .build();

            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("❌ Lỗi ghi Audit Log: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getFilteredLogs(String role, String module, Pageable pageable) {
        Specification<AuditLog> spec = AuditLogSpecification.filter(role, module);
        return auditLogRepository.findAll(spec, pageable);
    }
}