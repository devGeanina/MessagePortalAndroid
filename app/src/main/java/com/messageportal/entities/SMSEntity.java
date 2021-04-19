package com.messageportal.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class SMSEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String receiver;
    private String body;
    private Long id;
    private String status;
    private String statusMessage;
    private Date sentOn;

    public SMSEntity(){}

    public SMSEntity(String receiver, String body, Long id, String status, String statusMessage, Date sentOn) {
        this.receiver = receiver;
        this.body = body;
        this.id = id;
        this.status = status;
        this.statusMessage = statusMessage;
        this.sentOn = sentOn;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Date getSentOn() {
        return sentOn;
    }

    public void setSentOn(Date sentOn) {
        this.sentOn = sentOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SMSEntity smsEntity = (SMSEntity) o;
        return Objects.equals(receiver, smsEntity.receiver) &&
                Objects.equals(body, smsEntity.body) &&
                Objects.equals(id, smsEntity.id) &&
                Objects.equals(status, smsEntity.status) &&
                Objects.equals(statusMessage, smsEntity.statusMessage) &&
                Objects.equals(sentOn, smsEntity.sentOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receiver, body, id, status, statusMessage, sentOn);
    }

    @Override
    public String toString() {
        return "SMSEntity{" +
                "receiver='" + receiver + '\'' +
                ", body='" + body + '\'' +
                ", id=" + id +
                ", status='" + status + '\'' +
                ", statusMessage='" + statusMessage + '\'' +
                ", sentOn=" + sentOn +
                '}';
    }
}
