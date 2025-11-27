package com.example.demo.dto.dashboard_volunteer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class VolunteerDashBoardDTO {
    List<EventParticipationHistoryDTO> eventParticipationHistoryDTOS;

    List<RecentActivityDTO> recentActivityDTOS;

    List<RecommendEventDTO> recommendEventDTOS;

    List<RegisteredEventDTO> registeredEventDTOS;

    List<UpcomingEventDTO> upcomingEventDTOS;
}
