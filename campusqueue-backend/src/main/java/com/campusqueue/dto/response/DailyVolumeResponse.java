package com.campusqueue.dto.response;

import java.time.LocalDate;

public class DailyVolumeResponse {

    private LocalDate date;
    private long ticketCount;

    public DailyVolumeResponse() {
    }

    public DailyVolumeResponse(LocalDate date, long ticketCount) {
        this.date = date;
        this.ticketCount = ticketCount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(long ticketCount) {
        this.ticketCount = ticketCount;
    }
}
