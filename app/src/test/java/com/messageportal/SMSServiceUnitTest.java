package com.messageportal;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.messageportal.entities.SMSEntity;
import com.messageportal.entities.SMSPreferencesEntity;
import com.messageportal.services.DatabaseService;
import com.messageportal.services.SMSService;
import com.messageportal.utils.Constants;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@ExtendWith(MockitoExtension.class)
public class SMSServiceUnitTest {

    DatabaseService databaseServiceMock;

    Context appContext;

    SMSService smsService;

    public SMSServiceUnitTest() {
    }

    @Before
    public void setup(){
        databaseServiceMock = Mockito.mock(DatabaseService.class);
        appContext = Mockito.mock(Context.class);
        File file = Mockito.mock(File.class);
        Mockito.doReturn(file).when(appContext).getDatabasePath("msgPortal.sqlite");
        smsService = new SMSService(appContext, databaseServiceMock);
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
    }

    private List<SMSEntity> testSMSEntities(){
        List<SMSEntity> smsEntities = new ArrayList<>();
        smsEntities.add(new SMSEntity("+342424425", "Test SMS Sent", 1L, Constants.SMS_STATUS.SENT.getName(), "Message sent", new Date()));
        smsEntities.add(new SMSEntity("+464564646", "Test SMS Error", 2L, Constants.SMS_STATUS.ERROR.getName(), "Message failed", new Date()));
        smsEntities.add(new SMSEntity("+342424425", "Test SMS Sent", 3L, Constants.SMS_STATUS.SENT.getName(), "Message sent", new Date()));
        smsEntities.add(new SMSEntity("+464564646", "Test SMS Sent", 4L, Constants.SMS_STATUS.SENT.getName(), "Message sent", new Date()));
        smsEntities.add(new SMSEntity("+464564646", "Test SMS Error", 4L, Constants.SMS_STATUS.ERROR.getName(), "Message failed", new Date()));
        return smsEntities;
    }

    private SMSPreferencesEntity getLimitedSMSPref(){
        SMSPreferencesEntity smsPrefEntity = new SMSPreferencesEntity();
        smsPrefEntity.setAuthKey("@423B342b2f");
        smsPrefEntity.setDeviceIp("192.22.100.1");
        smsPrefEntity.setDevicePort("9090");
        smsPrefEntity.setId(1L);
        smsPrefEntity.setInvoiceDay(1);
        smsPrefEntity.setSmsCreditsLimit(2);
        smsPrefEntity.setSmsLeft(1);
        smsPrefEntity.setUnlimitedMessaging(false);
        return smsPrefEntity;
    }

    private SMSPreferencesEntity getUnLimitedSMSPref(){
        SMSPreferencesEntity smsPrefEntity = new SMSPreferencesEntity();
        smsPrefEntity.setAuthKey("@423B342b2f");
        smsPrefEntity.setDeviceIp("192.22.100.1");
        smsPrefEntity.setDevicePort("9090");
        smsPrefEntity.setId(1L);
        smsPrefEntity.setInvoiceDay(1);
        smsPrefEntity.setSmsCreditsLimit(2);
        smsPrefEntity.setSmsLeft(1);
        smsPrefEntity.setUnlimitedMessaging(true);
        return smsPrefEntity;
    }

    @Test
    public void testUnlimitedCredits()  {
        Mockito.when(databaseServiceMock.getAllSMSList()).thenReturn(testSMSEntities());
        Mockito.when(databaseServiceMock.getSavedPreferences()).thenReturn(getUnLimitedSMSPref());
        boolean isCreditsUnlimited = smsService.isCreditsUnlimited(getUnLimitedSMSPref());
        assertTrue(isCreditsUnlimited);
        boolean isLimitSurpassed = smsService.isSMSLimitReached(getUnLimitedSMSPref());
        Assertions.assertFalse(isLimitSurpassed);
    }

    @Test
    public void testLimitedCredits()  {
        Mockito.when(databaseServiceMock.getSavedPreferences()).thenReturn(getLimitedSMSPref());
        boolean isCreditsUnlimited = smsService.isCreditsUnlimited(getLimitedSMSPref());
        Assertions.assertFalse(isCreditsUnlimited);

        boolean isLimitSurpassed = smsService.isSMSLimitReached(getLimitedSMSPref());
        Assertions.assertFalse(isLimitSurpassed);

        int smsLeft = smsService.smsMessagesLeftToSend(getLimitedSMSPref());
        assertEquals(2,smsLeft);
    }

    @Test
    public void testIsSMSSent() {
        String id = "1";
        Mockito.when(databaseServiceMock.getSMSStatusById(id)).thenReturn(Constants.SMS_STATUS.SENT.getName());
        boolean result = smsService.isSMSMsgSent(id);
        assertEquals(true, result);
    }

    @Test
    public void testGetSmsStatusMessageFromDB() {
        String id = "1";
        Mockito.when(databaseServiceMock.getSMSStatusMessageById(id)).thenReturn("test");
        String expResult = "test";
        String result = smsService.getStatusMessageFromDB(id);
        assertEquals(expResult, result);
    }


    @Test
    public void testGetSMSStatusFromDB() {
        String id = "1";
        Mockito.when(databaseServiceMock.getSMSStatusById(id)).thenReturn(Constants.SMS_STATUS.ERROR.getName());
        String expResult = Constants.SMS_STATUS.ERROR.getName();
        String result = smsService.getStatusFromDB(id);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetAllSMSList() {
        Mockito.when(databaseServiceMock.getAllSMSList()).thenReturn(testSMSEntities());
        List<SMSEntity> expResult = testSMSEntities();
        List<SMSEntity> result = smsService.getAllSMSList();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetSentSMSList() {
        List<SMSEntity> sentSMS = new ArrayList<>();
        List<SMSEntity> allSMS = testSMSEntities();
        allSMS.stream().filter(smsEntity -> (smsEntity.getStatus().equalsIgnoreCase(Constants.SMS_STATUS.SENT.getName()))).forEachOrdered(smsEntity -> {
            sentSMS.add(smsEntity);
        });
        Mockito.when(databaseServiceMock.getSentSMSList()).thenReturn(sentSMS);
        List<SMSEntity> expResult = sentSMS;
        List<SMSEntity> result = smsService.getSentSMSList();
        assertEquals(expResult, result);
    }


    @Test
    public void testGetErrorSMSList() {
        List<SMSEntity> allSMS = testSMSEntities();
        List<SMSEntity> errorSMS = allSMS.stream()
                .filter(x->x.getStatus().equalsIgnoreCase(Constants.SMS_STATUS.ERROR.getName()))
                .collect(Collectors.toList());
        Mockito.when(databaseServiceMock.getFailedSMSList()).thenReturn(errorSMS);
        List<SMSEntity> expResult = errorSMS;
        List<SMSEntity> result = smsService.getFailedSMSList();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetSMSList() {
        String selectionType = "Failed";
        List<SMSEntity> allSMS = testSMSEntities();
        List<SMSEntity> failedSMS = allSMS.stream()
                .filter(x->x.getStatus().equalsIgnoreCase(Constants.SMS_STATUS.ERROR.getName()))
                .collect(Collectors.toList());
        Mockito.when(databaseServiceMock.getFailedSMSList()).thenReturn(failedSMS);
        List<SMSEntity> expResult = failedSMS;
        List<SMSEntity> result = smsService.getSMSList(selectionType);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetSavedPreferences() {
        Mockito.when(databaseServiceMock.getSavedPreferences()).thenReturn(getLimitedSMSPref());
        SMSPreferencesEntity expResult = getLimitedSMSPref();
        SMSPreferencesEntity result = smsService.getSavedPreferences();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetAuthKey() {
        Mockito.when(databaseServiceMock.getAuthKey()).thenReturn("s#%O6S^");
        String expResult = "s#%O6S^";
        String result = smsService.getAuthKey();
        assertEquals(expResult, result);
    }
}