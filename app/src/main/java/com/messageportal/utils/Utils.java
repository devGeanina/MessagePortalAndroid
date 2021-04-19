package com.messageportal.utils;

import android.util.Log;

import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Utils {
    public static final String SEND_URI = "/send";
    public static final String CREDITS_URI = "/credit";
    public static final String AUTH_URI = "/authKey";
    public static final String SENT_SMS_URI = "/sent";
    public static final String [] SEND_PARAMS = new String[]{"auth", "smsBody", "phoneNo", "id"};
    public static final String [] CREDIT_PARAMS = new String[]{"auth"};
    public static final String [] SENT_SMS_PARAMS = new String[]{"auth", "id"};
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_PORT = "9090"; //default value
    public static final Map<String, List<String>> ApplicationURLs = new HashMap<>();
    public static final int INVOICE_DAY = 15; //default value
    public static final int SMS_LIMIT = 50; //default value
    public static final List<String> SMS_SPINNER_TYPES = Arrays.asList("Sent", "Failed", "All");

    public static void createListOfUrlsForServer() {
        ApplicationURLs.put(SEND_URI, Arrays.asList(SEND_PARAMS));
        ApplicationURLs.put(CREDITS_URI, Arrays.asList(CREDIT_PARAMS));
        ApplicationURLs.put(AUTH_URI, null);
        ApplicationURLs.put(SENT_SMS_URI, Arrays.asList(SENT_SMS_PARAMS));
    }

    public static String getStringFromDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT, Locale.getDefault());
        return dateFormat.format(date);
    }

    public static Date getDateFromString(String date){
        SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT, Locale.getDefault());
        Date convertedDate = null;
        try {
            convertedDate = sdf.parse(date);
        } catch (ParseException e) {
            Log.e("getDateFromString", "Can't parse the string date." + e.getMessage());
        }
        return convertedDate;
    }

    public static String generateAuthKey(){
        byte[] randomBytes = new byte[24];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(randomBytes);
        String token = randomBytes.toString();
        return token;
    }
}
