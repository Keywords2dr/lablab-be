package com.keywords2dr.lablab.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keywords2dr.lablab.dto.chat.ChatRequest;
import com.keywords2dr.lablab.dto.chat.ChatResponse;
import com.keywords2dr.lablab.service.ChatContextService;
import com.keywords2dr.lablab.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatContextService chatContextService;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.google.genai.api-key}")
    private String geminiApiKey;

    @Value("${spring.ai.google.genai.chat.options.model:gemini-2.5-flash}")
    private String geminiModel;

    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final int MAX_HISTORY_TURNS = 10;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Entry point ───────────────────────────────────────────────────────────

    @Override
    public ChatResponse chat(ChatRequest request, String role, UUID userId) {
        try {
            // 1. Lấy dữ liệu thực tế từ DB (context chung)
            String dbContext = chatContextService.buildContext();

            // 2. Nếu là TEACHER, bổ sung thêm thống kê phiếu của phòng mình quản lý
            if ("TEACHER".equalsIgnoreCase(role)) {
                dbContext += chatContextService.buildTeacherTicketSummary(userId);
            }

            // 3. Xây dựng payload JSON cho Gemini
            String payload = buildGeminiPayload(request, dbContext, role);

            // 4. Gọi Gemini API
            String rawResponse = callGeminiApi(payload);

            // 5. Parse response
            String reply = parseGeminiResponse(rawResponse);

            // 6. Nhận diện intent để FE hiển thị icon
            String intentType = detectIntent(request.getMessage());

            log.info("[CHATBOX] model={} | role={} | userId={} | intent={} | question='{}'",
                    geminiModel, role, userId, intentType, request.getMessage());

            return ChatResponse.builder()
                    .reply(reply)
                    .intentType(intentType)
                    .build();

        } catch (Exception e) {
            log.error("[CHATBOX] Lỗi: {} | class: {}", e.getMessage(), e.getClass().getSimpleName(), e);

            String userMessage = e.getMessage() != null && e.getMessage().contains("RATE_LIMIT_429")
                    ? "⏳ Hệ thống AI đang bận, vui lòng chờ khoảng 1 phút rồi thử lại!"
                    : "Xin lỗi, hệ thống AI đang gặp sự cố. Vui lòng thử lại sau!";

            return ChatResponse.builder()
                    .reply(userMessage)
                    .intentType("ERROR")
                    .build();
        }
    }

    // ── Xây dựng JSON payload cho Gemini ─────────────────────────────────────

    private String buildGeminiPayload(ChatRequest request, String dbContext, String role) throws Exception {
        String systemInstruction = buildSystemInstruction(dbContext, role);

        List<Map<String, Object>> contents = new ArrayList<>();

        // Thêm lịch sử hội thoại (tối đa MAX_HISTORY_TURNS turn gần nhất)
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            List<ChatRequest.ChatTurn> history = request.getHistory();
            int startIdx = Math.max(0, history.size() - MAX_HISTORY_TURNS);
            for (int i = startIdx; i < history.size(); i++) {
                ChatRequest.ChatTurn turn = history.get(i);
                contents.add(Map.of(
                        "role", turn.getRole(),
                        "parts", List.of(Map.of("text", turn.getContent()))
                ));
            }
        }

        // Tin nhắn hiện tại của user
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", request.getMessage()))
        ));

        Map<String, Object> payload = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemInstruction))
                ),
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 1024,
                        "topP", 0.8
                ),
                "safetySettings", List.of(
                        Map.of("category", "HARM_CATEGORY_HARASSMENT",        "threshold", "BLOCK_NONE"),
                        Map.of("category", "HARM_CATEGORY_HATE_SPEECH",       "threshold", "BLOCK_NONE"),
                        Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_NONE"),
                        Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_NONE")
                )
        );

        return objectMapper.writeValueAsString(payload);
    }

    // ── System instruction + DB context ──────────────────────────────────────

    private String buildSystemInstruction(String dbContext, String role) {
        String now = LocalDateTime.now().format(DT_FMT);
        String roleInstruction = buildRoleInstruction(role);

        return """
                Bạn là trợ lý AI của hệ thống quản lý phòng Lab và hóa chất trường đại học (LabLab).
                Thời gian hiện tại: %s (giờ Việt Nam).
                Người dùng hiện tại có vai trò: %s.

                ## NHIỆM VỤ
                Trả lời các câu hỏi của người dùng dựa HOÀN TOÀN vào dữ liệu thực tế từ hệ thống được cung cấp bên dưới.
                Không được bịa đặt thông tin không có trong dữ liệu.

                ## PHẠM VI THEO VAI TRÒ
                %s

                ## NGUYÊN TẮC TRẢ LỜI
                1. Trả lời ngắn gọn, rõ ràng bằng tiếng Việt.
                2. Nếu không có thông tin → nói rõ "Không tìm thấy dữ liệu" thay vì đoán mò.
                3. Với câu hỏi về lịch trùng: so sánh khoảng thời gian người dùng hỏi với dữ liệu lịch mượn.
                4. Với câu hỏi về hóa chất: tìm kiếm tên gần đúng (không phân biệt hoa/thường).
                5. Nếu người dùng hỏi ngoài phạm vi vai trò → lịch sự từ chối, giải thích ngắn gọn.
                6. Chỉ trả lời các chủ đề liên quan đến phòng Lab, hóa chất, lịch mượn.
                   Với câu hỏi không liên quan → lịch sự từ chối và gợi ý câu hỏi phù hợp.

                ## DỮ LIỆU THỰC TẾ TỪ HỆ THỐNG
                %s
                """.formatted(now, role, roleInstruction, dbContext);
    }

    private String buildRoleInstruction(String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return """
                    Người dùng là ADMIN — có toàn quyền xem mọi thông tin trong hệ thống.
                    Có thể trả lời mọi câu hỏi về:
                    - Tất cả phiếu mượn của mọi người dùng (mọi trạng thái).
                    - Tồn kho hóa chất toàn bộ các phòng.
                    - Lịch sử mượn/trả, thống kê sử dụng phòng Lab.
                    - Danh sách người dùng, phân công giáo viên phụ trách phòng.
                    - Các báo cáo sự cố phòng/hóa chất.
                    """;
        }

        if ("TEACHER".equalsIgnoreCase(role)) {
            return """
                    Người dùng là GIẢNG VIÊN (TEACHER) — quản lý các phòng Lab được phân công.
                    Có thể trả lời các câu hỏi về:
                    - Thống kê phiếu mượn tại phòng mình quản lý:
                        + Có bao nhiêu phiếu đang chờ duyệt (PENDING_OWNER)?
                        + Có bao nhiêu phiếu đang yêu cầu trả (PENDING_RETURN)?
                        + Có bao nhiêu phiếu đang được mượn (BORROWED)?
                        + Có bao nhiêu phiếu đã duyệt chờ bàn giao (APPROVED)?
                        + Có bao nhiêu phiếu chờ Admin duyệt thêm (PENDING_ADMIN)?
                    - Chi tiết từng phiếu: ai mượn, loại phiếu, thời gian mượn/trả.
                    - Phòng nào đang trống hoặc đang được sử dụng trong khung giờ/ngày cụ thể.
                    - Hóa chất nào đó đang nằm ở phòng nào, còn bao nhiêu, đơn vị tính.
                    - Lịch mượn sắp tới để kiểm tra xem phòng có bị trùng không.
                    Dữ liệu thống kê phiếu đã được tổng hợp sẵn trong mục
                    "THỐNG KÊ PHIẾU MƯỢN (dành cho Giảng viên)" ở phần DỮ LIỆU THỰC TẾ.
                    KHÔNG được cung cấp thông tin về:
                    - Phiếu mượn của các phòng khác (ngoài phòng mình quản lý).
                    - Thông tin cá nhân của người dùng khác.
                    - Các chức năng quản trị hệ thống (phân quyền, tạo/xóa user...).
                    """;
        }

        // STUDENT
        return """
                Người dùng là SINH VIÊN (STUDENT) — có thể tra cứu thông tin chung của hệ thống.
                Có thể trả lời các câu hỏi về:
                - Phòng nào đang trống hoặc đang được sử dụng trong khung giờ/ngày cụ thể.
                - Hóa chất nào đó đang nằm ở phòng nào, còn bao nhiêu, đơn vị tính.
                - Lịch mượn sắp tới để kiểm tra xem phòng có bị trùng không.
                - Trạng thái các phiếu mượn (đang chờ duyệt, đã duyệt, đang mượn...).
                - Hướng dẫn cách tạo phiếu mượn phòng hoặc phiếu mượn hóa chất.
                KHÔNG được cung cấp thông tin về:
                - Thông tin cá nhân của người dùng khác.
                - Các chức năng quản trị hệ thống (phân quyền, tạo/xóa user...).
                - Các thao tác duyệt/từ chối phiếu (đó là quyền của Teacher/Admin).
                """;
    }

    // ── Gọi Gemini REST API ───────────────────────────────────────────────────

    private String callGeminiApi(String jsonPayload) throws Exception {
        String url = GEMINI_BASE_URL + geminiModel + ":generateContent?key=" + geminiApiKey;

        log.debug("[CHATBOX] Calling Gemini URL: {}", GEMINI_BASE_URL + geminiModel + ":generateContent");

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        }

        if (response.statusCode() == 429) {
            log.warn("[CHATBOX] Gemini rate limit (429) — model={}", geminiModel);
            throw new RuntimeException("RATE_LIMIT_429: Vượt giới hạn request/phút của Gemini API");
        }

        if (response.statusCode() != 200) {
            log.error("[CHATBOX] Gemini lỗi HTTP {} (model={}): {}",
                    response.statusCode(), geminiModel, response.body());
            throw new RuntimeException(
                    "Gemini API lỗi HTTP " + response.statusCode()
                            + " | model: " + geminiModel
                            + " | chi tiết: " + response.body());
        }

        return response.body();
    }

    // ── Parse Gemini response ─────────────────────────────────────────────────

    private String parseGeminiResponse(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);

        JsonNode promptFeedback = root.path("promptFeedback");
        if (!promptFeedback.isMissingNode()) {
            String blockReason = promptFeedback.path("blockReason").asText("");
            if (!blockReason.isEmpty() && !"null".equals(blockReason)) {
                return "Câu hỏi này không thể xử lý do vi phạm chính sách nội dung.";
            }
        }

        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty()) {
            log.warn("[CHATBOX] Gemini trả về candidates rỗng. Raw: {}", rawResponse);
            return "Không nhận được phản hồi từ AI. Vui lòng thử lại!";
        }

        JsonNode firstCandidate = candidates.get(0);

        String finishReason = firstCandidate.path("finishReason").asText("");
        if ("SAFETY".equals(finishReason)) {
            return "Câu hỏi này bị từ chối do chính sách an toàn nội dung.";
        }
        if ("MAX_TOKENS".equals(finishReason)) {
            log.warn("[CHATBOX] Response bị cắt do MAX_TOKENS");
        }

        JsonNode parts = firstCandidate.path("content").path("parts");
        if (parts.isEmpty()) {
            return "AI không trả về nội dung. Vui lòng thử lại!";
        }

        String text = parts.get(0).path("text").asText("");
        return text.isEmpty() ? "AI không trả về nội dung. Vui lòng thử lại!" : text;
    }

    // ── Nhận diện intent ──────────────────────────────────────────────────────

    private String detectIntent(String message) {
        if (message == null) return "GENERAL";
        String lower = message.toLowerCase();

        if (lower.contains("bao nhiêu") || lower.contains("thống kê")
                || lower.contains("đếm") || lower.contains("số lượng phiếu")
                || lower.contains("chờ duyệt") || lower.contains("cần duyệt")
                || lower.contains("yêu cầu trả") || lower.contains("chờ trả")) {
            return "TICKET_STATS";
        }
        if (lower.contains("phòng") && (lower.contains("lịch") || lower.contains("trùng")
                || lower.contains("giờ") || lower.contains("ngày") || lower.contains("có ai")
                || lower.contains("đang dùng") || lower.contains("sử dụng")
                || lower.contains("trống"))) {
            return "ROOM_SCHEDULE";
        }
        if (lower.contains("hóa chất") || lower.contains("hoá chất")
                || lower.contains("tồn kho") || lower.contains("nằm ở")
                || lower.contains("có trong") || lower.contains("kho")
                || lower.contains("còn bao nhiêu")) {
            return "CHEMICAL_QUERY";
        }
        if (lower.contains("phiếu") || lower.contains("mượn") || lower.contains("trả")) {
            return "TICKET_QUERY";
        }
        if (lower.contains("phòng") || lower.contains("lab")) {
            return "ROOM_INFO";
        }
        return "GENERAL";
    }
}