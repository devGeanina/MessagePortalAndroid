package com.messageportal.interfaces;

import com.messageportal.entities.SMSEntity;

public interface SendSMS {
    void sendTextMessage(SMSEntity newSMS);

    boolean isServerStopped();
}
