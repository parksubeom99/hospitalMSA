package kr.co.seoulit.his.admin.domain.master.schedule;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "doctor_schedule_template",
        indexes = {
                @Index(name = "idx_dst_staff_day", columnList = "staff_profile_id, day_of_week")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorScheduleTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_template_id")
    private Long scheduleTemplateId;

    // Master의 StaffProfile PK를 참조(서비스 간 FK는 두지 않고, ID만 저장)
    @Column(name = "staff_profile_id", nullable = false)
    private Long staffProfileId;

    // 1(Mon)~7(Sun)
    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "slot_minutes", nullable = false)
    private Integer slotMinutes = 10;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 200)
    private String note;
}
