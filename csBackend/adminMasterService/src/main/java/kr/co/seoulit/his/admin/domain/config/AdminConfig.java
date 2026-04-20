package kr.co.seoulit.his.admin.domain.config;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_config")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AdminConfig {

    @Id
    @Column(name = "config_key", length = 100, nullable = false)
    private String configKey;

    @Column(name = "config_value", length = 500, nullable = false)
    private String configValue;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
}
