package com.messageportal.entities;

import java.io.Serializable;
import java.util.Objects;

public class SMSPreferencesEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private int invoiceDay;
    private int smsCreditsLimit;
    private boolean unlimitedMessaging;
    private String deviceIp;
    private String devicePort;
    private Long id;
    private String simChoice;
    private String authKey;
    private int smsLeft;

    public int getInvoiceDay() {
        return invoiceDay;
    }

    public void setInvoiceDay(int invoiceDay) {
        this.invoiceDay = invoiceDay;
    }

    public int getSmsCreditsLimit() {
        return smsCreditsLimit;
    }

    public void setSmsCreditsLimit(int smsCreditsLimit) {
        this.smsCreditsLimit = smsCreditsLimit;
    }

    public boolean isUnlimitedMessaging() {
        return unlimitedMessaging;
    }

    public void setUnlimitedMessaging(boolean unlimitedMessaging) {
        this.unlimitedMessaging = unlimitedMessaging;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceIp() {
        return deviceIp;
    }

    public void setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
    }

    public String getDevicePort() {
        return devicePort;
    }

    public void setDevicePort(String devicePort) {
        this.devicePort = devicePort;
    }

    public String getSimChoice() {
        return simChoice;
    }

    public void setSimChoice(String simChoice) {
        this.simChoice = simChoice;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public int getSmsLeft() {
        return smsLeft;
    }

    public void setSmsLeft(int smsLeft) {
        this.smsLeft = smsLeft;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SMSPreferencesEntity that = (SMSPreferencesEntity) o;
        return invoiceDay == that.invoiceDay &&
                smsCreditsLimit == that.smsCreditsLimit &&
                unlimitedMessaging == that.unlimitedMessaging &&
                smsLeft == that.smsLeft &&
                Objects.equals(deviceIp, that.deviceIp) &&
                Objects.equals(devicePort, that.devicePort) &&
                Objects.equals(id, that.id) &&
                Objects.equals(simChoice, that.simChoice) &&
                Objects.equals(authKey, that.authKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invoiceDay, smsCreditsLimit, unlimitedMessaging, deviceIp, devicePort, id, simChoice, authKey, smsLeft);
    }

    @Override
    public String toString() {
        return "SMSPreferencesEntity{" +
                "invoiceDay=" + invoiceDay +
                ", smsCreditsLimit=" + smsCreditsLimit +
                ", unlimitedMessaging=" + unlimitedMessaging +
                ", deviceIp='" + deviceIp + '\'' +
                ", devicePort='" + devicePort + '\'' +
                ", id=" + id +
                ", simChoice='" + simChoice + '\'' +
                ", authKey='" + authKey + '\'' +
                ", smsLeft=" + smsLeft +
                '}';
    }
}
