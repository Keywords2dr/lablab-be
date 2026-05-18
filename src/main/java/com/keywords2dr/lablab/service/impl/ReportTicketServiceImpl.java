package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.report.ReportTicketRequest;
import com.keywords2dr.lablab.dto.report.ReportTicketResponse;
import com.keywords2dr.lablab.entity.Item;
import com.keywords2dr.lablab.entity.ReportTicket;
import com.keywords2dr.lablab.entity.Room;
import com.keywords2dr.lablab.entity.User;
import com.keywords2dr.lablab.entity.enums.ReportType;
import com.keywords2dr.lablab.exception.BadRequestException;
import com.keywords2dr.lablab.exception.ResourceNotFoundException;
import com.keywords2dr.lablab.mapper.ReportTicketMapper;
import com.keywords2dr.lablab.repository.ItemRepository;
import com.keywords2dr.lablab.repository.ReportTicketRepository;
import com.keywords2dr.lablab.repository.RoomInventoryRepository;
import com.keywords2dr.lablab.repository.RoomRepository;
import com.keywords2dr.lablab.repository.UserRepository;
import com.keywords2dr.lablab.repository.specification.ReportTicketSpecification;
import com.keywords2dr.lablab.security.SecurityUtils;
import com.keywords2dr.lablab.service.ReportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportTicketServiceImpl implements ReportTicketService {

    private final ReportTicketRepository reportTicketRepository;
    private final RoomRepository roomRepository;
    private final ItemRepository itemRepository;
    private final RoomInventoryRepository roomInventoryRepository;
    private final UserRepository userRepository;
    private final ReportTicketMapper reportTicketMapper;

    @Override
    @Transactional
    public ReportTicketResponse createReport(ReportTicketRequest request) {
        UUID currentUserId = getCurrentUserIdOrThrow();

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng"));

        ReportTicket.ReportTicketBuilder builder = ReportTicket.builder()
                .reporter(currentUser)
                .reportType(request.getReportType())
                .description(request.getDescription())
                .room(room);

        switch (request.getReportType()) {
            case CHEMICAL -> {
                Item item = itemRepository.findById(request.getItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa chất"));

                boolean existsInRoom = roomInventoryRepository
                        .existsByItem_ItemIdAndRoom_RoomId(request.getItemId(), request.getRoomId());
                if (!existsInRoom)
                    throw new BadRequestException("Hóa chất này không tồn tại trong phòng đã chọn");

                builder.item(item);
            }
            case ROOM -> {
            }
            default ->
                    throw new BadRequestException("Loại báo cáo không được hỗ trợ: " + request.getReportType());
        }

        ReportTicket saved = reportTicketRepository.save(builder.build());
        return reportTicketMapper.toResponse(saved);
    }

    @Override
    public Page<ReportTicketResponse> getMyReports(Pageable pageable) {
        UUID currentUserId = getCurrentUserIdOrThrow();

        return reportTicketRepository
                .findByReporterIdFetch(currentUserId, pageable)
                .map(reportTicketMapper::toResponse);
    }

    @Override
    public Page<ReportTicketResponse> getAllReports(
            ReportType reportType, UUID roomId, UUID itemId, Pageable pageable) {

        Specification<ReportTicket> spec = ReportTicketSpecification.filter(reportType, roomId, itemId);
        return reportTicketRepository.findAll(spec, pageable)
                .map(reportTicketMapper::toResponse);
    }

    // ==================== HELPER ====================

    private UUID getCurrentUserIdOrThrow() {
        UUID id = SecurityUtils.getCurrentUserId();
        if (id == null) throw new BadRequestException("Phiên làm việc hết hạn!");
        return id;
    }
}