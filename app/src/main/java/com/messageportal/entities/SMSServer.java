package com.messageportal.entities;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.JsonObject;
import com.messageportal.services.SMSService;
import com.messageportal.interfaces.SendSMS;
import com.messageportal.utils.Constants;
import com.messageportal.utils.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class SMSServer extends NanoHTTPD {

    private JsonObject jsonObject;
    private SMSService smsService;
    private Context appContext;
    private SMSPreferencesEntity savedPref;
    private SendSMS sendSMSHandler;

    public SMSServer(int port, Context context, SMSPreferencesEntity savedPreferences, SendSMS sendSMS) throws IOException {
        super(port);
        this.appContext = context;
        this.savedPref = savedPreferences;
        this.sendSMSHandler = sendSMS;
        smsService = new SMSService(appContext);
        System.out.println("\nRunning! Point your browsers to http://deviceIp:"+ port +"\n");
    }

    public SMSServer() {
        super(9090); //default port
        System.out.println("\nRunning! Point your browsers to http://deviceIp:8080/ \n");
    }

    public static void main(String[] args) {
        new SMSServer();
    }

    public class SendSMSAsyncTask {
        private ExecutorService executor
                = Executors.newSingleThreadExecutor();
        public Future<?> sendSMS(SMSEntity smsEntity) {
            return executor.submit(() -> {
                sendSMSHandler.sendTextMessage(smsEntity);
            });
        }
    }

    /*
    The server is used to listen on the receiving SMS messages. 
    The URL is called either using the desktop app or calling it from the browser.
     */
    @Override
    public Response serve(IHTTPSession session) {

        if (session.getMethod() == Method.GET) {
            if (session.getUri() == null || !Utils.ApplicationURLs.containsKey(session.getUri())) {
                jsonObject = new JsonObject();
                jsonObject.addProperty("statusCode", Constants.RESPONSE_STATUS.INVALID_URI.getType());
                jsonObject.addProperty("message", Constants.RESPONSE_STATUS.INVALID_URI.getName());
                return newFixedLengthResponse(Status.ACCEPTED, MIME_HTML, jsonObject.toString());
            }

            Map<String, String> params = session.getParms();

            if(savedPref.getAuthKey() == null || (savedPref != null && savedPref.getAuthKey().equals(""))){
                jsonObject = new JsonObject();
                jsonObject.addProperty("statusCode", Constants.RESPONSE_STATUS.AUTH_NOT_FOUND.getType());
                jsonObject.addProperty("message", Constants.RESPONSE_STATUS.AUTH_NOT_FOUND.getName());
                return newFixedLengthResponse(Status.ACCEPTED, MIME_HTML, jsonObject.toString());
            }

            List<String> defaultURIParams = new ArrayList<>();
            List<String> receivedURIParams = new ArrayList<>();
            String [] parametersNoId = {"auth", "smsBody", "phoneNo"};
            List<String> defaultSendURIParamsNoId = Arrays.asList(parametersNoId);
            Collections.sort(defaultSendURIParamsNoId);

            if(!session.getUri().equalsIgnoreCase(Utils.AUTH_URI)) {
                receivedURIParams.addAll(params.keySet());
                Collections.sort(receivedURIParams);

                defaultURIParams = Utils.ApplicationURLs.get(session.getUri());
                Collections.sort(defaultURIParams);

                if (!defaultURIParams.equals(receivedURIParams) && !defaultSendURIParamsNoId.equals(receivedURIParams) ) {
                    jsonObject = new JsonObject();
                    jsonObject.addProperty("statusCode",
                            Constants.RESPONSE_STATUS.INVALID_PARAMS.getType());
                    jsonObject.addProperty("message", Constants.RESPONSE_STATUS.INVALID_PARAMS.getName());
                    return newFixedLengthResponse(Status.ACCEPTED, MIME_HTML, jsonObject.toString());
                }
            }

            String authKey = "";
            switch (session.getUri()) {
                case Utils.SEND_URI:
                    try {
                        authKey = URLDecoder.decode(params.get("auth"), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        Log.e("serve","Exception decoding the auth key: " + e.getMessage());
                    }

                    if (params.isEmpty() || authKey == null || !(authKey.equalsIgnoreCase(savedPref.getAuthKey()))) {
                        jsonObject = new JsonObject();
                        jsonObject.addProperty("statusCode", Constants.RESPONSE_STATUS.AUTH_FAILURE.getType());
                        jsonObject.addProperty("message", Constants.RESPONSE_STATUS.AUTH_FAILURE.getName());
                        return newFixedLengthResponse(Status.ACCEPTED, MIME_HTML, jsonObject.toString());
                    }

                    if (receivedURIParams.equals(defaultURIParams) || receivedURIParams.equals(defaultSendURIParamsNoId)) {
                        if (smsService.isSMSLimitReached(savedPref)) {
                            jsonObject = new JsonObject();
                            jsonObject.addProperty("statusCode",
                                    Constants.RESPONSE_STATUS.INSUFFICIENT_CREDIT.getType());
                            jsonObject.addProperty("message",
                                    Constants.RESPONSE_STATUS.INSUFFICIENT_CREDIT.getName());
                        } else {
                            String smsBody = "";
                            String receiver = "";
                            try {
                                smsBody = URLDecoder.decode(params.get("smsBody"), "UTF-8");
                                receiver = URLDecoder.decode(params.get("phoneNo"), "UTF-8");
                            } catch (UnsupportedEncodingException ex) {
                                Log.e("serve","Exception decoding the SMS body: " + ex.getMessage());
                            }

                            SMSEntity sms = new SMSEntity();
                            sms.setBody(smsBody);
                            sms.setReceiver(receiver.trim());
                            sms.setStatus(Constants.SMS_STATUS.SENT.getName());
                            sms.setSentOn(Calendar.getInstance().getTime());
                            sms.setStatusMessage("SMS sent"); //will be changed later on by the broadcast receiver,
                            //save for now to get the id for the json object returned

                            if(receivedURIParams.equals(defaultURIParams)) {
                                String id = params.get("id");
                                try {
                                    Long sentId = Long.parseLong(id);
                                    sms.setId(sentId); //parsable, save it
                                } catch (NumberFormatException nfe) {
                                    //it's not parsable, faulty id format or null, don't save it
                                }
                            }
                            Long savedSMSId = smsService.saveOrUpdateSMS(sms);
                            new SendSMSAsyncTask().sendSMS(sms);

                            jsonObject = new JsonObject();
                            jsonObject.addProperty("statusCode", Constants.RESPONSE_STATUS.SENDING.getType());
                            jsonObject.addProperty("message", Constants.RESPONSE_STATUS.SENDING.getName());
                            jsonObject.addProperty("id", savedSMSId);
                        }
                    }else{
                        jsonObject.addProperty("statusCode", Constants.RESPONSE_STATUS.INVALID_PARAMS.getType());
                        jsonObject.addProperty("message", Constants.RESPONSE_STATUS.INVALID_PARAMS.getName());
                    }
                    break;

                case Utils.CREDITS_URI:
                    try {
                        authKey = URLDecoder.decode(params.get("auth"), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        Log.e("serve","Exception decoding the auth key: " + e.getMessage());
                    }

                    if (params.isEmpty() || authKey == null || !(authKey.equalsIgnoreCase(savedPref.getAuthKey()))) {
                        jsonObject = new JsonObject();
                        jsonObject.addProperty("statusCode", Constants.RESPONSE_STATUS.AUTH_FAILURE.getType());
                        jsonObject.addProperty("message", Constants.RESPONSE_STATUS.AUTH_FAILURE.getName());
                        return newFixedLengthResponse(Status.ACCEPTED, MIME_HTML, jsonObject.toString());
                    }

                    jsonObject = new JsonObject();
                    if (smsService.isSMSLimitReached(savedPref)) {
                        jsonObject.addProperty("statusCode", Constants.RESPONSE_STATUS.INSUFFICIENT_CREDIT.getType());
                        jsonObject.addProperty("message", Constants.RESPONSE_STATUS.INSUFFICIENT_CREDIT.getName());
                    } else {
                        int smsLeftToSend;

                        if (smsService.isCreditsUnlimited(savedPref)) {
                            smsLeftToSend = Integer.MAX_VALUE;
                        } else {
                            smsLeftToSend = smsService.smsMessagesLeftToSend(savedPref);
                        }

                        if (smsLeftToSend <= 0) {
                            jsonObject.addProperty("statusCode", Constants.RESPONSE_STATUS.INSUFFICIENT_CREDIT.getType());
                            jsonObject.addProperty("message", Constants.RESPONSE_STATUS.INSUFFICIENT_CREDIT.getName());
                            jsonObject.addProperty("smsLeft", 0);
                        } else {
                            jsonObject.addProperty("statusCode", Constants.RESPONSE_STATUS.SUFFICIENT_CREDIT.getType());
                            jsonObject.addProperty("message", Constants.RESPONSE_STATUS.SUFFICIENT_CREDIT.getName());
                            jsonObject.addProperty("smsLeft", smsLeftToSend);
                        }
                        jsonObject.addProperty("creditsLimit", savedPref.getSmsCreditsLimit());
                        jsonObject.addProperty("invoiceDay", savedPref.getInvoiceDay());
                        jsonObject.addProperty("unlimited", savedPref.isUnlimitedMessaging() ?  1 : 0);
                    }
                    break;

                case Utils.AUTH_URI:
                    jsonObject = new JsonObject();
                    if(savedPref == null || (savedPref.getAuthKey() != null && savedPref.getAuthKey().equals(""))){
                        jsonObject.addProperty("statusCode", Constants.RESPONSE_STATUS.AUTH_NOT_FOUND.getType());
                        jsonObject.addProperty("message", Constants.RESPONSE_STATUS.AUTH_NOT_FOUND.getName());
                    }else {
                        jsonObject.addProperty("statusCode", Constants.RESPONSE_STATUS.AUTH_FOUND.getType());
                        jsonObject.addProperty("message", Constants.RESPONSE_STATUS.AUTH_FOUND.getName());
                        jsonObject.addProperty("authKey", savedPref.getAuthKey());
                    }
                    break;
                case Utils.SENT_SMS_URI:
                    jsonObject = new JsonObject();
                    try {
                        authKey = URLDecoder.decode(params.get("auth"), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        Log.e("serve","Exception decoding the auth key: " + e.getMessage());
                    }

                    if (params.isEmpty() || authKey == null || !(authKey.equalsIgnoreCase(savedPref.getAuthKey()))) {
                        jsonObject = new JsonObject();
                        jsonObject.addProperty("statusCode", Constants.RESPONSE_STATUS.AUTH_FAILURE.getType());
                        jsonObject.addProperty("message", Constants.RESPONSE_STATUS.AUTH_FAILURE.getName());
                        return newFixedLengthResponse(Status.ACCEPTED, MIME_HTML, jsonObject.toString());
                    }
                    if (receivedURIParams.equals(defaultURIParams)) {
                        boolean isSMSSent = smsService.isSMSMsgSent(params.get("id"));
                        if(isSMSSent) {
                            jsonObject.addProperty("statusCode", Constants.RESPONSE_STATUS.SENT.getType());
                            jsonObject.addProperty("message", Constants.RESPONSE_STATUS.SENT.getName());
                        }else{
                            jsonObject.addProperty("statusCode", Constants.RESPONSE_STATUS.SENT_ERROR.getType());
                            jsonObject.addProperty("message", Constants.RESPONSE_STATUS.SENT_ERROR.getName());
                        }
                    }else{
                        jsonObject.addProperty("statusCode", Constants.RESPONSE_STATUS.INVALID_PARAMS.getType());
                        jsonObject.addProperty("message", Constants.RESPONSE_STATUS.INVALID_PARAMS.getName());
                    }
                    break;
                default:
                    jsonObject = new JsonObject();
                    jsonObject.addProperty("statusCode", Constants.RESPONSE_STATUS.INVALID_URI.getType());
                    jsonObject.addProperty("message", Constants.RESPONSE_STATUS.INVALID_URI.getName());
            }
        }

        return newFixedLengthResponse(Status.ACCEPTED, MIME_HTML, jsonObject.toString());
    }
}
