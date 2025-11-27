package com.example.demo.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationStatisticsDTO {
    private Integer totalRegistrations;
    private Integer pendingRegistrations;
    private Integer approvedRegistrations;
    private Integer rejectedRegistrations;
    private Integer cancelledRegistrations;
    private Double approvalRate;  // Tỷ lệ phê duyệt (%)
}

