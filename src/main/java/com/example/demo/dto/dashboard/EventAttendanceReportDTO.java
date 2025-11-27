package com.example.demo.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventAttendanceReportDTO {
    private Long eventId;
    private String eventTitle;
    private LocalDateTime eventDate;
    private Integer registeredCount;
    private Integer approvedCount;
    private Integer attendedCount;  // Số người thực tế tham dự (có thể cần thêm field trong Registration)
    private Integer maxParticipants;
    private Double attendanceRate;  // Tỷ lệ tham dự (attended/approved * 100)
    private Double fillRate;  // Tỷ lệ lấp đầy (approved/maxParticipants * 100)
}

