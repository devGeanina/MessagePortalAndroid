package com.messageportal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.messageportal.adapters.CustomRecyclerAdapter;
import com.messageportal.interfaces.RecyclerClickHandler;
import com.messageportal.adapters.RecyclerTouchAdapter;
import com.messageportal.entities.SMSEntity;
import com.messageportal.entities.SMSPreferencesEntity;
import com.messageportal.entities.SMSServer;
import com.messageportal.services.BackgroundService;
import com.messageportal.services.SMSService;
import com.messageportal.interfaces.SendSMS;
import com.messageportal.utils.Constants;
import com.messageportal.utils.Utils;
import net.steamcrafted.materialiconlib.MaterialMenuInflater;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends AppCompatActivity implements SendSMS {

    private List<SMSEntity> smsList = new ArrayList<>();
    private SMSPreferencesEntity savedPreferences;
    private SMSService smsService;
    private CustomRecyclerAdapter customRecyclerViewAdapter;
    private BackgroundService backgroundService;
    private CheckBox serverStatusToggle;
    private ProgressBar loader;
    private boolean isServerOn = false;
    private static BroadcastReceiver sentSMSBroadcastReceiver;
    private static BroadcastReceiver receivedSMSBroadcastReceiver;
    private static final String inAppPrefFile = "MessagePortalPref";
    private SharedPreferences messagePortalPref;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private PowerManager.WakeLock powerLock;
    private Ringtone smsNotifSound;
    private boolean smsNotifRingtoneOn = true;
    private RecyclerView recyclerView;
    private SMSServer smsServer;
    private String deviceIP;
    private String selectedSMSListType = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
        setSupportActionBar(toolbar);

        serverStatusToggle = (CheckBox) findViewById(R.id.server_status);
        TextView noSMSText = (TextView)findViewById(R.id.noSMSTxt);
        Spinner listTypeSelect = (Spinner)findViewById(R.id.selectListSpinner);
        loader = (ProgressBar)findViewById(R.id.loaderId);
        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeLayout);

        //better scale the view
        DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        ViewGroup.LayoutParams params=swipeRefreshLayout.getLayoutParams();
        int height = (int)(displayMetrics.heightPixels*0.75);
        params.height = height;
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        swipeRefreshLayout.setLayoutParams(params);

        //request app permissions for various Android versions
        requestPermissions();

        //get device network ip
        getDeviceIP();

        if(deviceIP == null || deviceIP.equals("") || deviceIP.equals("0.0.0.0")) {
            Toast.makeText(getApplicationContext(), "IP not found, check Internet connection.", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(getApplicationContext(), "Connected IP: " +deviceIP, Toast.LENGTH_LONG).show();
        }

        //create the db and retrieve the app preferences from it
        smsService = new SMSService(this);
        savedPreferences = smsService.getSavedPreferences();

        //get the shared preferences for the ringtone & server status
        messagePortalPref = getSharedPreferences(inAppPrefFile, Context.MODE_PRIVATE);

        if (messagePortalPref != null) {
            isServerOn = messagePortalPref.getBoolean("isServerOn", false);
            smsNotifRingtoneOn = messagePortalPref.getBoolean("smsNotifRingtoneOn", false);
            selectedSMSListType = messagePortalPref.getString("smsListType", null);
        }

        //get the SMS list
        if(selectedSMSListType == null)
            selectedSMSListType = Constants.SMS_LIST_TYPE.ALL.getName(); //default value

        smsList = smsService.getSMSList(selectedSMSListType);

        if (smsList == null || smsList.isEmpty()) {
            Toast.makeText(getApplicationContext(), "No saved SMS yet.", Toast.LENGTH_LONG).show();
            if (noSMSText.getVisibility() == View.INVISIBLE)
                noSMSText.setVisibility(View.VISIBLE);
        } else {
            if (noSMSText.getVisibility() == View.VISIBLE)
                noSMSText.setVisibility(View.INVISIBLE);
            List<SMSEntity> smsListCopy = smsList;
            customRecyclerViewAdapter = new CustomRecyclerAdapter(smsListCopy, getApplicationContext());
        }

        //set main view
        recyclerView = (RecyclerView) findViewById(R.id.sms_list);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(customRecyclerViewAdapter);
        LinearSnapHelper snapHelper  = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                smsList = smsService.getSMSList(selectedSMSListType);
                if(customRecyclerViewAdapter != null)
                    customRecyclerViewAdapter.refresh(smsList);
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        swipeRefreshLayout.setColorSchemeResources(R.color.dialog_primary);

        backgroundService = new BackgroundService(this);
        Intent backgroundServiceIntent = new Intent(getApplicationContext(), backgroundService.getClass());
        startBackgroundService(backgroundService.getClass(), backgroundServiceIntent);

        Utils.createListOfUrlsForServer();

        if (isServerOn) { //user left the server checked on
            startServer(deviceIP);
        }

        handleListItemClick();
        setAppPreferences();
        handleServerUIStatus();
        handleListSelection(listTypeSelect);
    }

    private void handleListSelection(Spinner listTypeSelect) {
        listTypeSelect.setAdapter(new ArrayAdapter<String>(MainActivity.this, R.layout.spinner_dropdown_item, Utils.SMS_SPINNER_TYPES));
        listTypeSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                ((TextView) view).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.dialog_primary));

                if(customRecyclerViewAdapter != null) {
                    loader.setVisibility(View.VISIBLE);
                    String selectedItem = (String) parent.getItemAtPosition(position);
                    if (selectedItem.equalsIgnoreCase("Sent")) {
                        selectedSMSListType = Constants.SMS_LIST_TYPE.SENT.getName();
                    } else if (selectedItem.equalsIgnoreCase("Failed")) {
                        selectedSMSListType = Constants.SMS_LIST_TYPE.FAILED.getName();
                    } else if (selectedItem.equalsIgnoreCase("All")) {
                        selectedSMSListType = Constants.SMS_LIST_TYPE.ALL.getName();
                    }

                    smsList = smsService.getSMSList(selectedSMSListType);

                    SharedPreferences.Editor editor = messagePortalPref.edit();
                    editor.putString("smsListType", selectedSMSListType);
                    editor.commit();

                    customRecyclerViewAdapter.refresh(smsList);
                    loader.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            List<String> notGrantedPermissions = checkGrantedPermission();
            //request for permissions for devices where the manifest file is not valid
            if(!notGrantedPermissions.isEmpty())
                ActivityCompat.requestPermissions(this, notGrantedPermissions.toArray(new String[0]), 101);
        }
    }

    private List<String> checkGrantedPermission() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.SEND_SMS);
        permissions.add(Manifest.permission.WAKE_LOCK);
        permissions.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);
        permissions.add(Manifest.permission.GET_TASKS);
        permissions.add(Manifest.permission.READ_PHONE_STATE);
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        permissions.add(Manifest.permission.INTERNET);
        List<String> notGrantedPermissions = new ArrayList<>();

        for(String permission: permissions) {
            int result = ContextCompat.checkSelfPermission(this, permission);
            if (result == PackageManager.PERMISSION_GRANTED)
                continue;
            else
                notGrantedPermissions.add(permission);
        }
        return notGrantedPermissions;
    }

    private void handleServerUIStatus() {
        if (isServerOn) {
            serverStatusToggle.setChecked(true);
        } else {
            serverStatusToggle.setChecked(false);
        }
        serverStatusToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startServer(deviceIP);
                    isServerOn = true;
                } else {
                    if (isServerOn) {
                        stopServer();
                    }
                    Toast.makeText(getApplicationContext(), "Server is stopping...", Toast.LENGTH_LONG).show();
                    isServerOn = false;
                }

                SharedPreferences.Editor editor = messagePortalPref.edit();
                editor.putBoolean("isServerOn", isServerOn);
                editor.commit();
            }
        });
    }

    private void setAppPreferences() {
        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

                switch (key) {
                    case "isServerOn":
                        isServerOn = messagePortalPref.getBoolean("serverIsStopped", false);
                        break;
                    case "smsNotifRingtoneOn":
                        smsNotifRingtoneOn = messagePortalPref.getBoolean("smsNotifRingtoneOn", false);
                        break;
                    case "smsListType":
                        selectedSMSListType = messagePortalPref.getString("smsListType", null);
                        break;
                    default:
                        // don't perform any action
                }
            }
        };
        messagePortalPref.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void sendTextMessage(SMSEntity newSMS) {
        String SMS_SENT_TAG = "SMS_SENT";
        String SMS_DELIVERED_TAG = "SMS_DELIVERED";

        try {
            if (newSMS == null) {
                Log.e("sendTextMessage", "The SMS object is null, can't send it.");
                return;
            }

            PendingIntent sentPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(SMS_SENT_TAG), 0);
            PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(SMS_DELIVERED_TAG), 0);

            // Receive when each part of the SMS has been sent
            sentSMSBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            if (smsNotifRingtoneOn) {
                                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                smsNotifSound = RingtoneManager.getRingtone(getApplicationContext(), notification);
                                smsNotifSound.play();
                            }
                            newSMS.setStatus(Constants.SMS_STATUS.SENT.getName());
                            newSMS.setStatusMessage("SMS sent.");
                            newSMS.setSentOn(Calendar.getInstance().getTime());
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            newSMS.setStatus(Constants.SMS_STATUS.ERROR.getName());
                            newSMS.setStatusMessage("SMS generic failure.");
                            Toast.makeText(getBaseContext(), "A generic occurred. Verify phone number and credit.", Toast.LENGTH_SHORT).show();
                            break;

                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            newSMS.setStatus(Constants.SMS_STATUS.ERROR.getName());
                            newSMS.setStatusMessage("SMS no service.");
                            Toast.makeText(getBaseContext(), "SMS no service.", Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            newSMS.setStatus(Constants.SMS_STATUS.ERROR.getName());
                            newSMS.setStatusMessage("SMS null PDU.");
                            Toast.makeText(getBaseContext(), "SMS null PDU.", Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            newSMS.setStatus(Constants.SMS_STATUS.ERROR.getName());
                            newSMS.setStatusMessage("SMS radio off.");
                            Toast.makeText(getBaseContext(), "SMS radio off.", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    newSMS.setSentOn(new Date(System.currentTimeMillis()));
                    saveOrUpdateSMS(newSMS);
                }
            };

            receivedSMSBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode()) {
                        case Activity.RESULT_CANCELED:
                            Toast.makeText(getApplicationContext(), "SMS not sent, action cancelled.",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case Activity.RESULT_OK:
                            Toast.makeText(getBaseContext(), "SMS delivered.",
                                    Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            };


            registerReceiver(sentSMSBroadcastReceiver, new IntentFilter(SMS_SENT_TAG));
            registerReceiver(receivedSMSBroadcastReceiver, new IntentFilter(SMS_DELIVERED_TAG));
            SmsManager smsMgr = SmsManager.getDefault();

            boolean isMultipart = false;
            if(newSMS.getBody().length() > 160)
                isMultipart = true;

            SubscriptionManager localSubscriptionManager = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

            //permission check needed
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "You need to provide phone permission before sending an SMS. Retry after.", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
                return;
            }

            //check SMS body size
            ArrayList<String> messageParts = null;
            ArrayList<PendingIntent> pendingIntents = null;

            if(isMultipart){
                messageParts = smsMgr.divideMessage(newSMS.getBody());
                pendingIntents = new ArrayList<>(messageParts.size());
                for (int i = 0; i < messageParts.size(); i++) {
                    pendingIntents.add(PendingIntent.getBroadcast(this, 0, new Intent(SMS_SENT_TAG), 0));
                }
            }

            //get SIM info
            if (localSubscriptionManager.getActiveSubscriptionInfoCount() > 1) {
                if(savedPreferences == null) {
                    Toast.makeText(getApplicationContext(), "Please fill in the settings", Toast.LENGTH_SHORT).show();
                }else if(savedPreferences.getSimChoice() == null || savedPreferences.getSimChoice().equals("")) {
                    generateSIMChoiceDialog();
                }

                //dual SIM phone
                if(savedPreferences != null && savedPreferences.getSimChoice() != null && !savedPreferences.getSimChoice().equals("")) {

                    List localList = localSubscriptionManager.getActiveSubscriptionInfoList();
                    SubscriptionInfo sim1 = (SubscriptionInfo) localList.get(0);
                    SubscriptionInfo sim2 = (SubscriptionInfo) localList.get(1);

                    //SendSMS From SIM One
                    if (savedPreferences.getSimChoice().equalsIgnoreCase(Constants.SIM.SIM1.getName())) {
                        if (isMultipart)
                            smsMgr.getSmsManagerForSubscriptionId(sim1.getSubscriptionId()).sendMultipartTextMessage(newSMS.getReceiver(), null, messageParts, pendingIntents, null);
                        else
                            smsMgr.getSmsManagerForSubscriptionId(sim1.getSubscriptionId()).sendTextMessage(newSMS.getReceiver(), null, newSMS.getBody(), sentPendingIntent, deliveredPendingIntent);
                    }
                    //SendSMS From SIM Two
                    if (savedPreferences.getSimChoice().equalsIgnoreCase(Constants.SIM.SIM2.getName())) {
                        if(isMultipart)
                            smsMgr.getSmsManagerForSubscriptionId(sim2.getSubscriptionId()).sendMultipartTextMessage(newSMS.getReceiver(), null, messageParts, pendingIntents, null);
                        else
                            smsMgr.getSmsManagerForSubscriptionId(sim2.getSubscriptionId()).sendTextMessage(newSMS.getReceiver(), null, newSMS.getBody(), sentPendingIntent, deliveredPendingIntent);
                    }
                }
            } else if(localSubscriptionManager.getActiveSubscriptionInfoCount() == 1){
                List localList = localSubscriptionManager.getActiveSubscriptionInfoList();
                SubscriptionInfo simInfo1 = (SubscriptionInfo) localList.get(0);
                if(isMultipart)
                    smsMgr.getSmsManagerForSubscriptionId(simInfo1.getSubscriptionId()).sendMultipartTextMessage(newSMS.getReceiver(), null, messageParts, pendingIntents, null);
                else
                    smsMgr.getSmsManagerForSubscriptionId(simInfo1.getSubscriptionId()).sendTextMessage(newSMS.getReceiver(), null, newSMS.getBody(), sentPendingIntent, deliveredPendingIntent);
            }
        } catch (Exception e) {
            Log.e("sendTextMessage","Failed to send a new SMS message:" + e);
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveOrUpdateSMS(final SMSEntity newSMS) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(new Runnable() {
            @Override
            public void run() {
                smsService.refreshDB();
                smsService.saveOrUpdateSMS(newSMS);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        smsList = smsService.getSentSMSList();
                        customRecyclerViewAdapter.refresh(smsList);
                    }
                });
            }
        });
    }

    private void handleListItemClick() {
        recyclerView.addOnItemTouchListener(new RecyclerTouchAdapter(getApplicationContext(), recyclerView, new RecyclerClickHandler() {
            @Override
            public void onClick(View view, int position) {
                if(smsList != null) {
                    SMSEntity selectedEntity = smsList.get(position);
                    generateSMSDetailDialog(selectedEntity);
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                if(smsList != null) {
                    SMSEntity selectedEntity = smsList.get(position);
                    generateConfirmDialog("Delete the selected SMS?", (SMSEntity)selectedEntity);
                }
            }
        }));
    }

    private void startBackgroundService(Class<?> serviceClass, Intent backgroundServiceIntent) {
        if(backgroundService != null && !isServiceUp(backgroundService.getClass())){
            startService(backgroundServiceIntent);
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            powerLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"messageportal:powerlock");
            powerLock.acquire();
        }
    }

    private boolean isServiceUp(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isServiceUp?", true + "");
                return true;
            }
        }
        Log.i ("isServiceUp?", false + "");
        return false;
    }

    private String getWifiDeviceIP(){
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wm.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        try {
            String deviceIP = InetAddress.getByAddress(
                    ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ip).array())
                    .getHostAddress();
        } catch (UnknownHostException e) {
            //wifi may be off
        }
        return null;
    }

    private void getDeviceIP(){
        String networkDeviceIP = null;
        networkDeviceIP = getWifiDeviceIP();
        if(networkDeviceIP == null || (networkDeviceIP != null && networkDeviceIP.equals(""))) { //wifi was possibly off, try network
            try {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                    NetworkInterface networkInterface = en.nextElement();
                    for (Enumeration<InetAddress> inetAddressEnumeration = networkInterface.getInetAddresses(); inetAddressEnumeration.hasMoreElements(); ) {
                        InetAddress inetAddress = inetAddressEnumeration.nextElement();
                        if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                            deviceIP = inetAddress.getHostAddress();
                        }
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            deviceIP = networkDeviceIP;
        }
    }

    private void startServer(String deviceIp){
        try {
            if(savedPreferences != null) {
                smsServer = new SMSServer(Integer.valueOf(savedPreferences.getDevicePort()), this, savedPreferences, this);
                smsServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
                Toast.makeText(getApplicationContext(), "Server is running on IP:" + deviceIp, Toast.LENGTH_SHORT).show();
                isServerOn = true;
                SharedPreferences.Editor editor = messagePortalPref.edit();
                editor.putBoolean("isServerOn", true);
                editor.apply();
                editor.commit();
            } else if(savedPreferences == null) {
                Toast.makeText(getApplicationContext(), "Please set the device port in settings.", Toast.LENGTH_LONG).show();
            }
        }catch (IOException ioe){
            Log.e("startServer","Can't start server." + ioe.getMessage());
        }
    }

    private void stopServer(){
        isServerOn = false;
        SharedPreferences.Editor editor = messagePortalPref.edit();
        editor.putBoolean("isServerOn", false);
        editor.apply();
        editor.commit();
        if(smsServer != null) {
            smsServer.stop();
        }
    }

    @Override
    public boolean isServerStopped() {
        if (messagePortalPref != null) {
            isServerOn = messagePortalPref.getBoolean("isServerOn", false);
        }
        return isServerOn;
    }

    private void generateSettingsDialog(){
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogMessagePortal);
        alertDialog.setTitle(Html.fromHtml("<font color='#FFFFFF'>Settings</font>"));
        Drawable dialogDrawable = getResources().getDrawable(R.drawable.baseline_manage_accounts_24);
        dialogDrawable.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_ATOP));
        alertDialog.setIcon(dialogDrawable);
        LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = li.inflate(R.layout.settings_dialog, null);
        alertDialog.setView(dialogView);
        Switch soundSwitch = (Switch) dialogView.findViewById(R.id.ringtoneSwitch);
        EditText devicePort = (EditText)dialogView.findViewById(R.id.devicePortText);
        EditText invoiceDay = (EditText)dialogView.findViewById(R.id.invoiceText);
        EditText creditsLimit = (EditText)dialogView.findViewById(R.id.creditsText);
        Switch unlimitedSwitch = (Switch) dialogView.findViewById(R.id.unlimitedSwitch);

        if(smsNotifRingtoneOn){
            soundSwitch.setChecked(true);
        }

        if(savedPreferences != null){
           devicePort.setText(savedPreferences.getDevicePort());
           invoiceDay.setText(String.valueOf(savedPreferences.getInvoiceDay()));
           creditsLimit.setText(String.valueOf(savedPreferences.getSmsCreditsLimit()));
           if(savedPreferences.isUnlimitedMessaging())
               unlimitedSwitch.setChecked(true);
           else
               unlimitedSwitch.setChecked(false);
        }

        unlimitedSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    creditsLimit.setEnabled(false);
                    invoiceDay.setEnabled(false);
                } else {
                    creditsLimit.setEnabled(true);
                    invoiceDay.setEnabled(true);
                }
            }
        });

        alertDialog.setPositiveButton("Save", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    SharedPreferences.Editor editor = messagePortalPref.edit();
                    if (soundSwitch.isChecked()) {
                        smsNotifRingtoneOn = true;
                        editor.putBoolean("smsNotifRingtoneOn", true);
                    } else {
                        smsNotifRingtoneOn = false;
                        editor.putBoolean("smsNotifRingtoneOn", false);
                    }
                    editor.commit();
                    SMSPreferencesEntity smsPreferencesEntity = new SMSPreferencesEntity();
                    if (savedPreferences != null) {
                        smsPreferencesEntity = savedPreferences;
                    }

                    if (!TextUtils.isEmpty(devicePort.getText()))
                    smsPreferencesEntity.setDevicePort(devicePort.getText().toString());

                    if (!TextUtils.isEmpty(invoiceDay.getText()))
                    smsPreferencesEntity.setInvoiceDay(Integer.valueOf(invoiceDay.getText().toString()));

                    if (!TextUtils.isEmpty(creditsLimit.getText()))
                    smsPreferencesEntity.setSmsCreditsLimit(Integer.valueOf(creditsLimit.getText().toString()));

                    smsPreferencesEntity.setUnlimitedMessaging(unlimitedSwitch.isChecked());
                    smsService.saveSMSPref(smsPreferencesEntity);
                    savedPreferences = smsPreferencesEntity;
                    Toast.makeText(getApplicationContext(), "Preferences saved.", Toast.LENGTH_SHORT).show();
                }catch (Exception e){
                    if (TextUtils.isEmpty(creditsLimit.getText()))
                        Toast.makeText(getApplicationContext(), "Please fill in the credits limit.", Toast.LENGTH_SHORT).show();
                    else if (TextUtils.isEmpty(invoiceDay.getText()))
                        Toast.makeText(getApplicationContext(), "Please fill in the invoice day.", Toast.LENGTH_SHORT).show();
                    else if (TextUtils.isEmpty(devicePort.getText()))
                        Toast.makeText(getApplicationContext(), "Please fill in the device port.", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getApplicationContext(), "Error saving the app settings.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog alert = alertDialog.create();
        alert.show();

        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.90);
        alert.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void generateSMSDetailDialog(SMSEntity selectedEntity) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogMessagePortal);
        alertDialog.setTitle(Html.fromHtml("<font color='#FFFFFF'>To: " + selectedEntity.getReceiver() + "</font>"));
        Drawable dialogDrawable = getResources().getDrawable(R.drawable.baseline_mark_chat_read_24);
        dialogDrawable.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_ATOP));
        alertDialog.setIcon(dialogDrawable);

        if(selectedEntity.getStatus().equalsIgnoreCase(Constants.SMS_STATUS.SENT.getName()))
            alertDialog.setMessage(selectedEntity.getBody());
        else if(selectedEntity.getStatus().equalsIgnoreCase(Constants.SMS_STATUS.ERROR.getName()))
            alertDialog.setMessage(selectedEntity.getBody().concat(". Error status: ").concat(selectedEntity.getStatusMessage()));

        alertDialog.setPositiveButton("Close", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog alert = alertDialog.create();
        alert.show();

        int dialogWidth = (int)(getResources().getDisplayMetrics().widthPixels*0.90);
        alert.getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void generateConfirmDialog(String message, Object optional){
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogMessagePortal);
        alertDialog.setTitle(Html.fromHtml("<font color='#FFFFFF'>Please confirm: " + "</font>"));
        Drawable dialogDrawable = getResources().getDrawable(R.drawable.baseline_mark_chat_read_24);
        dialogDrawable.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_ATOP));
        alertDialog.setIcon(dialogDrawable);
        alertDialog.setMessage(message);

        SMSEntity selectedSMSEntity = (SMSEntity) optional;

        alertDialog.setPositiveButton("Confirm", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(selectedSMSEntity != null && selectedSMSEntity.getId() != null) {
                    smsList.remove(selectedSMSEntity);
                    Toast.makeText(getApplicationContext(), "Deleting SMS...", Toast.LENGTH_LONG).show();
                    smsService.deleteSMS(selectedSMSEntity.getId());
                    if(customRecyclerViewAdapter != null)
                        customRecyclerViewAdapter.refresh(smsService.getSMSList(selectedSMSListType));
                }
                dialogInterface.dismiss();
            }
        });

        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog alert = alertDialog.create();
        alert.show();
        int dialogWidth = (int)(getResources().getDisplayMetrics().widthPixels*0.90);
        alert.getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void generateSessionInfoDialog(){
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogMessagePortal);
        alertDialog.setTitle(Html.fromHtml("<font color='#FFFFFF'>Session info: </font>"));
        Drawable dialogDrawable = getResources().getDrawable(R.drawable.baseline_language_24);
        dialogDrawable.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_ATOP));
        alertDialog.setIcon(dialogDrawable);

        LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = li.inflate(R.layout.session_info_dialog, null);
        alertDialog.setView(dialogView);
        TextView authText = (TextView) dialogView.findViewById(R.id.authTxt);
        TextView deviceIpText = (TextView)dialogView.findViewById(R.id.deviceIpTxt);
        TextView smsLimit = (TextView)dialogView.findViewById(R.id.creditsTxt);

        String creditsLimitText = "";
        int creditsLimit = Utils.SMS_LIMIT;
        boolean isUnlimited = false;

        if(savedPreferences != null) {
            isUnlimited = savedPreferences.isUnlimitedMessaging();
            authText.setText(savedPreferences.getAuthKey());
        }else{
            authText.setText("Not generated yet.");
        }

        if(isUnlimited)
            creditsLimitText = "Unlimited";
        else{
            //set number of sms messages left
            if(savedPreferences != null) {
                creditsLimit = smsService.smsMessagesLeftToSend(savedPreferences);
                creditsLimitText = Integer.toString(creditsLimit);
            } else {
                creditsLimitText = "Unknown";
            }
        }

        smsLimit.setText(creditsLimitText);

        if(deviceIP != null || (deviceIP != null && !deviceIP.equals("")))
            deviceIpText.setText(deviceIP);
        else
            deviceIpText.setText("IP: unknown");

        alertDialog.setPositiveButton("Close", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog alert = alertDialog.create();
        alert.show();
        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.90);
        alert.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void generateInfoDialog(){
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogMessagePortal);
        alertDialog.setTitle(Html.fromHtml("<font color='#FFFFFF'>About: </font>"));
        Drawable dialogDrawable = getResources().getDrawable(R.drawable.baseline_live_help_24);
        dialogDrawable.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_ATOP));
        alertDialog.setIcon(dialogDrawable);

        LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = li.inflate(R.layout.about_dialog, null);
        alertDialog.setView(dialogView);
        TextView appVersion = (TextView) dialogView.findViewById(R.id.appVersionText);
        TextView phoneNoFormat = (TextView)dialogView.findViewById(R.id.phoneNoFormatTxt);

        phoneNoFormat.setText("+countryPrefix, followed by phone number");

        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            appVersion.setText(info.versionName);
         } catch (PackageManager.NameNotFoundException e) {
            Log.e("", "App version couldn't be retrieved: " + e.getMessage());
            appVersion.setText("Unknown");
         }
     
        alertDialog.setPositiveButton("Close", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog alert = alertDialog.create();
        alert.show();
        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.90);
        alert.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void generateSIMChoiceDialog(){
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogMessagePortal);
        alertDialog.setTitle(Html.fromHtml("<font color='#FFFFFF'>Select SIM for SMS</font>"));
        Drawable dialogDrawable = getResources().getDrawable(R.drawable.baseline_sim_card_24);
        dialogDrawable.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_ATOP));
        alertDialog.setIcon(dialogDrawable);
        LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = li.inflate(R.layout.sim_card_dialog, null);
        alertDialog.setView(dialogView);

        RadioButton sim1 = (RadioButton)dialogView.findViewById(R.id.sim1Btn);
        RadioButton sim2 = (RadioButton)dialogView.findViewById(R.id.sim2Btn);

        if(savedPreferences != null){
            if(savedPreferences.getSimChoice().equalsIgnoreCase(Constants.SIM.SIM1.getName()))
                sim1.setSelected(true);
            else if(savedPreferences.getSimChoice().equalsIgnoreCase(Constants.SIM.SIM2.getName()))
                sim2.setSelected(true);
        }

        alertDialog.setPositiveButton("Select", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    SMSPreferencesEntity smsPreferencesEntity = new SMSPreferencesEntity();
                    if (savedPreferences != null) {
                        smsPreferencesEntity = savedPreferences;
                    }

                    if(sim1.isChecked())
                        smsPreferencesEntity.setSimChoice(Constants.SIM.SIM1.getName());
                    if(sim2.isChecked())
                        smsPreferencesEntity.setSimChoice(Constants.SIM.SIM2.getName());

                    smsService.updateSIMPref(smsPreferencesEntity.getSimChoice(), smsPreferencesEntity.getId());
                    savedPreferences = smsPreferencesEntity;
                    Toast.makeText(getApplicationContext(), "Preferences saved.", Toast.LENGTH_SHORT).show();
                }catch (Exception e){
                    Toast.makeText(getApplicationContext(), "Error saving the SIM settings.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog alert = alertDialog.create();
        alert.show();

        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.90);
        alert.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void generateURIInfoDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogMessagePortal);
        alertDialog.setTitle(Html.fromHtml("<font color='#FFFFFF'>URL gateway format:</font>"));
        Drawable dialogDrawable = getResources().getDrawable(R.drawable.baseline_important_devices_24);
        dialogDrawable.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_ATOP));
        alertDialog.setIcon(dialogDrawable);
        LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = li.inflate(R.layout.uri_info_dialog, null);
        alertDialog.setView(dialogView);

        TextView authParam = (TextView)dialogView.findViewById(R.id.authUriParam);
        TextView creditParam = (TextView)dialogView.findViewById(R.id.creditURIParam);
        TextView sendParam = (TextView)dialogView.findViewById(R.id.sendURIParam);

        sendParam.setText(TextUtils.join(", ", Utils.SEND_PARAMS));
        creditParam.setText(TextUtils.join(", ", Utils.CREDIT_PARAMS));
        authParam.setText("No parameters needed.");

        alertDialog.setPositiveButton("Close", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog alert = alertDialog.create();
        alert.show();

        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.90);
        alert.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /*Suppress needed to use setOptionalIconsVisible() until Google fixes the existing bug.*/
    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MaterialMenuInflater
                .with(this)
                .setDefaultColor(R.color.dialog_primary)
                .inflate(R.menu.menu, menu);

        for(int i = 0; i < menu.size(); i++){
            Drawable drawable = menu.getItem(i).getIcon();
            if(drawable != null) {
                drawable.mutate();
                drawable.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.app_primary), PorterDuff.Mode.SRC_ATOP));
            }
        }

        if(menu instanceof MenuBuilder){
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_settings:
                generateSettingsDialog();
                return true;
            case R.id.action_uri_info:
                generateURIInfoDialog();
                return true;
            case R.id.action_session_info:
                generateSessionInfoDialog();
                return true;
            case R.id.action_info:
                generateInfoDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(smsService != null) {
            smsList = smsService.getAllSMSList();
            if (customRecyclerViewAdapter != null && smsList != null)
                customRecyclerViewAdapter.refresh(smsList);
        }
    }

    /*OnDestroy will stop the service and the background service will try to restart it or it will end with the app.*/
    @Override
    protected void onDestroy() {
        // onDestroy gets called and the Broadcast Receiver will revive it, else it will die with the app
        Intent serviceIntent = new Intent(getApplicationContext(), backgroundService.getClass());
        stopService(serviceIntent);
        stopServer();
        if(receivedSMSBroadcastReceiver != null && sentSMSBroadcastReceiver != null) {
            unregisterReceiver(sentSMSBroadcastReceiver);
            unregisterReceiver(receivedSMSBroadcastReceiver);
        }

        if(preferenceChangeListener != null){
            messagePortalPref.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //don't do anything, only the main window available
    }
}