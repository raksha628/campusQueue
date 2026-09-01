package com.campusqueue.dto.projection;

import java.time.LocalDate;

public interface DailyVolumeProjection {
    LocalDate getDate();
    Long getTicketCount();
}
