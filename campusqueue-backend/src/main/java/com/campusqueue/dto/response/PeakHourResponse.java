package com.campusqueue.dto.response;

public class PeakHourResponse {

    private Integer hour;
    private String formattedHour;
    private long ticketCount;

    public PeakHourResponse() {
    }

    public PeakHourResponse(Integer hour, String formattedHour, long ticketCount) {
        this.hour = hour;
        this.formattedHour = formattedHour;
        this.ticketCount = ticketCount;
    }

    public Integer getHour() {
        return hour;
    }

    public void setHour(Integer hour) {
        this.hour = hour;
    }

    public String getFormattedHour() {
        return formattedHour;
    }

    public void setFormattedHour(String formattedHour) {
        this.formattedHour = formattedHour;
    }

    public long getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(long ticketCount) {
        this.ticketCount = ticketCount;
    }
}
