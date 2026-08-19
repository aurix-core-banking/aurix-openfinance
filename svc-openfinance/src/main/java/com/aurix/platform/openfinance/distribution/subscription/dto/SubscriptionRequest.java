package com.aurix.platform.openfinance.distribution.subscription.dto;

/**
 * Request para criacao de assinatura.
 */
public class SubscriptionRequest {

    private String participantId;
    private String dataProductId;
    private String callbackUrl;
    private String events;
    private int durationDays;

    public SubscriptionRequest() {
    }

    public SubscriptionRequest(String participantId, String dataProductId,
                                String callbackUrl, String events, int durationDays) {
        this.participantId = participantId;
        this.dataProductId = dataProductId;
        this.callbackUrl = callbackUrl;
        this.events = events;
        this.durationDays = durationDays;
    }

    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public String getDataProductId() {
        return dataProductId;
    }

    public void setDataProductId(String dataProductId) {
        this.dataProductId = dataProductId;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getEvents() {
        return events;
    }

    public void setEvents(String events) {
        this.events = events;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }
}
