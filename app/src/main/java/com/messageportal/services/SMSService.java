package com.messageportal.services;

import android.content.Context;

import com.messageportal.entities.SMSEntity;
import com.messageportal.entities.SMSPreferencesEntity;
import com.messageportal.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class SMSService{

    private DatabaseService dbService;
    private Context appContext;

    public SMSService(Context context) {
        this.appContext = context;
        dbService = new DatabaseService(appContext);
    }

    public SMSService(Context context, DatabaseService databaseService) {
        this.appContext = context;
        dbService = databaseService;
    }

    public boolean isSMSMsgSent(String id) {
        if (id == null) {
            return false;
        } else {
            String smsStatus = dbService.getSMSStatusById(id);
            if (smsStatus != null) {
                if (smsStatus.equals(Constants.SMS_STATUS.SENT.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getStatusMessageFromDB(String id) {
        if (id == null) {
            return null;
        }
        return dbService.getSMSStatusMessageById(id);
    }

    public String getStatusFromDB(String id) {
        if (id == null) {
            return null;
        }

        return dbService.getSMSStatusById(id);
    }

    public boolean isSMSLimitReached(SMSPreferencesEntity smsPreferencesEntity) {
        if (smsPreferencesEntity.getInvoiceDay() == 0) {
            return false;
        }

        if (smsPreferencesEntity.isUnlimitedMessaging() || smsPreferencesEntity.getSmsCreditsLimit() >= 0) {
            return false;
        }

        List<SMSEntity> sentSMSFromTheLastInvoice = dbService
                .getSentSMSListWithinTime(smsPreferencesEntity.getInvoiceDay());
        if (sentSMSFromTheLastInvoice == null) {
            return false;
        }

        if (sentSMSFromTheLastInvoice.size() <= 0
                || sentSMSFromTheLastInvoice.size() >= smsPreferencesEntity.getSmsCreditsLimit()) {
            return true;
        }
        return false;
    }

    public int smsMessagesLeftToSend(SMSPreferencesEntity smsPreferencesEntity) {
        int smsLimit = smsPreferencesEntity.getSmsCreditsLimit();
        List<SMSEntity> sentSMSFromTheLastInvoice = dbService
                .getSentSMSListWithinTime(smsPreferencesEntity.getInvoiceDay());
        if (sentSMSFromTheLastInvoice != null) {
            int smsLeftToSend = smsLimit - sentSMSFromTheLastInvoice.size();
            return smsLeftToSend;
        }
        return 0;
    }

    public boolean isCreditsUnlimited(SMSPreferencesEntity smsPreferencesEntity) {
        if (smsPreferencesEntity.isUnlimitedMessaging()) {
            return true;
        }
        return false;
    }

    public List<SMSEntity> getAllSMSList() {
        List<SMSEntity> smsEntities = new ArrayList<>();
        smsEntities = dbService.getAllSMSList();
        return smsEntities;
    }

    public List<SMSEntity> getSentSMSList() {
        List<SMSEntity> smsEntities = new ArrayList<>();
        smsEntities = dbService.getSentSMSList();
        return smsEntities;
    }

    public List<SMSEntity> getFailedSMSList() {
        List<SMSEntity> smsEntities = new ArrayList<>();
        smsEntities = dbService.getFailedSMSList();
        return smsEntities;
    }

    public List<SMSEntity> getSMSList(String smsListType){
        if (smsListType.equalsIgnoreCase(Constants.SMS_LIST_TYPE.ALL.getName()))
            return getAllSMSList();
        else if(smsListType.equalsIgnoreCase(Constants.SMS_LIST_TYPE.SENT.getName()))
            return getSentSMSList();
        else if(smsListType.equalsIgnoreCase(Constants.SMS_LIST_TYPE.FAILED.getName()))
            return getFailedSMSList();
        else
            return new ArrayList<>();
    }

    public Long saveOrUpdateSMS(SMSEntity sms) {
        if (sms == null) {
            throw new IllegalArgumentException("Cannot save the SMS because it's null.");
        }
        return dbService.saveOrUpdateSMS(sms);
    }

    public void saveSMSPref(SMSPreferencesEntity smsPref) {
        if (smsPref == null) {
            throw new IllegalArgumentException("Cannot save the item because it's null.");
        }
        if (smsPref.getId() != null) {
            dbService.updateSMSPref(smsPref);
        } else {
            dbService.saveSMSPref(smsPref);
        }
    }

    public void deleteSMS(Long smsId) {
        if (smsId == null) {
            throw new IllegalArgumentException("Cannot delete the item because the id is null.");
        }
        dbService.deleteSMS(smsId);
    }

    public SMSPreferencesEntity getSavedPreferences() {
        return dbService.getSavedPreferences();
    }

    public void updateSIMPref(String simChoice, Long sharedPrefId){
        if(simChoice == null)
            throw new IllegalArgumentException("Cannot update the item because the SIM choice is null.");

        if(sharedPrefId == null)
            throw new IllegalArgumentException("Cannot update the item because the id is null.");

        dbService.updateSMSSIM(Constants.SIM.valueOfLabel(simChoice).getType(),sharedPrefId);
    }

    public String getAuthKey(){
        return dbService.getAuthKey();
    }

    public void refreshDB(){
        dbService.close();
        dbService.getWritableDatabase();
    }
}
