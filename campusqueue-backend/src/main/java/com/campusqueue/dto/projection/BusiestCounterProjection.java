package com.campusqueue.dto.projection;

public interface BusiestCounterProjection {
    Long getCounterId();
    String getCounterName();
    String getCounterCode();
    Long getHandledTickets();
}
