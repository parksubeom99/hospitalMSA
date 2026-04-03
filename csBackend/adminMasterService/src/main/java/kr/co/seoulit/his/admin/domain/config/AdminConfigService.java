package kr.co.seoulit.his.admin.domain.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminConfigService {

    private static final String EMERGENCY_COUNT_KEY = "emergency_count";
    private static final int DEFAULT_EMERGENCY_COUNT = 3;

    private final AdminConfigRepository repository;

    @Transactional(readOnly = true)
    public int getEmergencyCount() {
        return repository.findById(EMERGENCY_COUNT_KEY)
                .map(c -> {
                    try { return Integer.parseInt(c.getConfigValue()); }
                    catch (NumberFormatException e) { return DEFAULT_EMERGENCY_COUNT; }
                })
                .orElse(DEFAULT_EMERGENCY_COUNT);
    }

    @Transactional
    public int setEmergencyCount(int value) {
        int clamped = Math.max(0, Math.min(10, value)); // 0~10 범위 강제
        AdminConfig config = repository.findById(EMERGENCY_COUNT_KEY)
                .orElseGet(() -> AdminConfig.builder()
                        .configKey(EMERGENCY_COUNT_KEY)
                        .description("응급 병상 운영자 수동 설정값 (0~10)")
                        .build());
        config.setConfigValue(String.valueOf(clamped));
        config.setUpdatedAt(LocalDateTime.now());
        repository.save(config);
        return clamped;
    }
}
