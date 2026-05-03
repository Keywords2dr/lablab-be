package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.entity.DataAlias;
import com.keywords2dr.lablab.repository.DataAliasRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataNormalizationService {

    private final DataAliasRepository dataAliasRepository;
    private final Map<String, String> dictionaryCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadDictionaryIntoCache() {
        log.info("⏳ Đang tải Từ điển chuẩn hóa dữ liệu lên RAM...");
        dictionaryCache.clear();
        List<DataAlias> aliases = dataAliasRepository.findAll();
        for (DataAlias alias : aliases) {
            dictionaryCache.put(alias.getWrongTerm().toLowerCase(), alias.getStandardTerm());
        }
        log.info("✅ Đã tải thành công {} quy tắc chuẩn hóa.", aliases.size());
    }

    public void refreshCache() {
        loadDictionaryIntoCache();
    }

    /**
     * Hàm chuẩn hóa thông minh có khả năng "tự học" từ mới.
     * @param input Từ người dùng nhập vào
     * @param aliasType Loại từ điển (ví dụ: "PACKAGING", "UNIT"). Dùng để tự động lưu từ mới.
     */
    @Transactional
    public String normalizeAndLearn(String input, String aliasType) {
        if (!StringUtils.hasText(input)) return null;

        // 1. Chuẩn hóa cơ bản
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFC);
        String cleanedKey = normalized.trim().replaceAll("\\s+", " ").toLowerCase();

        // 2. Tra cứu trong Cache (Từ đã biết)
        if (dictionaryCache.containsKey(cleanedKey)) {
            return dictionaryCache.get(cleanedKey);
        }

        // 3. TỪ HOÀN TOÀN MỚI (Lọt lưới)
        // Tạo ra từ chuẩn (Viết hoa chữ cái đầu)
        String newStandardTerm = cleanedKey.length() > 1
                ? cleanedKey.substring(0, 1).toUpperCase() + cleanedKey.substring(1)
                : cleanedKey.toUpperCase();

        // 4. "TỰ HỌC": Lưu ngay từ mới này vào Database
        try {
            DataAlias newAlias = new DataAlias();
            newAlias.setAliasType(aliasType);
            newAlias.setWrongTerm(cleanedKey); // Lưu key dạng chữ thường
            newAlias.setStandardTerm(newStandardTerm); // Lưu giá trị chuẩn hiển thị đẹp

            dataAliasRepository.save(newAlias);

            // Cập nhật luôn Cache trên RAM để các dòng Excel tiếp theo chạy nhanh
            dictionaryCache.put(cleanedKey, newStandardTerm);

            log.info("🧠 Hệ thống đã tự học từ mới: [{}] -> [{}] (Loại: {})", cleanedKey, newStandardTerm, aliasType);

        } catch (Exception e) {
            log.warn("⚠️ Bỏ qua tự học do lỗi trùng lặp từ: {}", cleanedKey);
        }

        return newStandardTerm;
    }
}