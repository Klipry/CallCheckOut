package com.example.callcheckout;
import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.CallLog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public  class CallLogService extends Service {
    private Bundle savedState;
    private FirebaseFirestore firestoreDB;
    private static final String PREFS_NAME = "CallLogPrefs";
    private SharedPreferences sharedPreferences;

    private static final int DATABASE_ACCESS_DELAY = 2000; // 2 seconds
    private static final String CALL_HISTORY_COLLECTION = "callhistory";
    private static final String CALL_HISTORY_DOCUMENT_ID = "call";
    private String login = "z";

    private ContentObserver callLogObserver;
    private List<Call> callsList;

    private Handler handler;


    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
        login = sharedPreferences.getString("login", null);
        handler = new Handler();
        firestoreDB = FirebaseFirestore.getInstance();
        callsList = new ArrayList<>();
        callLogObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                if (hasPermission()) {
                    handler.removeCallbacksAndMessages(null);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            getCallDetails();
                            if (!callsList.isEmpty()) {
                                addCallLogsToFirestore();
                            }
                        }
                    }, DATABASE_ACCESS_DELAY);
                }
            }
        };
    }

    private boolean hasPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getContentResolver().registerContentObserver(CallLog.Calls.CONTENT_URI, true, callLogObserver);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(callLogObserver);
    }

    private void getCallDetails() {
        Cursor cursor = null;
        try {
            // Получаем текущую дату и время
            Calendar calendar = Calendar.getInstance();

            cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC");
            if (cursor == null) {
                return;
            }
            int number = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            int type = cursor.getColumnIndex(CallLog.Calls.TYPE);
            int date = cursor.getColumnIndex(CallLog.Calls.DATE);

            // Получаем номера SIM-карт
            SubscriptionManager subscriptionManager = SubscriptionManager.from(this);
            List<SubscriptionInfo> subsInfoList = subscriptionManager.getActiveSubscriptionInfoList();
            String[] simSlotIds = new String[2];
            if (subsInfoList != null) {
                for (int i = 0; i < subsInfoList.size(); i++) {
                    SubscriptionInfo subsInfo = subsInfoList.get(i);
                    int slotIndex = subsInfo.getSimSlotIndex();
                    simSlotIds[slotIndex] = subsInfo.getIccId();
                }
            }

            // Сохраняем текущий месяц и год
            int currentMonth = calendar.get(Calendar.MONTH);
            int currentYear = calendar.get(Calendar.YEAR);

            while (cursor.moveToNext()) {
                String phoneNumber = cursor.getString(number);
                String callType = cursor.getString(type);
                String callDate = cursor.getString(date);
                String simSlotId = "";
                int dircode = Integer.parseInt(callType);
                Date callDateTime = new Date(Long.valueOf(callDate));
                String dateString = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(callDateTime);
                String dir;

                // Получаем месяц и год текущего вызова
                Calendar callCalendar = Calendar.getInstance();
                callCalendar.setTime(callDateTime);
                int callMonth = callCalendar.get(Calendar.MONTH);
                int callYear = callCalendar.get(Calendar.YEAR);

                // Проверяем, что вызов был сделан в текущем месяце
                if (callMonth == currentMonth && callYear == currentYear) {
                    switch (dircode) {
                        case CallLog.Calls.OUTGOING_TYPE:
                            dir = "OUTGOING";
                            break;
                        case CallLog.Calls.INCOMING_TYPE:
                            dir = "INCOMING";
                            break;
                        case CallLog.Calls.MISSED_TYPE:
                            dir = "MISSED";
                            break;
                        default:
                            dir = "UNKNOWN";
                            break;
                    }
                    // Определяем, с какой SIM-карты был сделан вызов
                    if (simSlotIds[0] != null && phoneNumber.contains(simSlotIds[0])) {
                        simSlotId = "SIM 1";
                    } else if (simSlotIds[1] != null && phoneNumber.contains(simSlotIds[1])) {
                        simSlotId = "SIM 2";
                    }
                    callsList.add(new Call(dateString, phoneNumber, simSlotId, dir));
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }





    private void addCallLogsToFirestore() {
        if (firestoreDB == null) {
            return;
        }

        for (Call call : callsList) {
            // Check if the call has already been added to Firestore
            if (!sharedPreferences.contains(call.getId())) {
                addCallToFirestore(call.getDate(), call.getNumber(), call.getSimSlotId(), call.getDirection());
                // Save the call ID to SharedPreferences to prevent it from being added again
                sharedPreferences.edit().putBoolean(call.getId(), true).apply();
            }
        }

        callsList.clear();
    }


    private void addCallToFirestore(String date, String number, String simSlotId, String direction) {
        Map<String, Object> callHistoryMap = new HashMap<>();
        callHistoryMap.put("date", date);
        callHistoryMap.put("number", number);
        callHistoryMap.put("simSlotId", simSlotId);
        callHistoryMap.put("direction", direction);

        firestoreDB.collection("callhistory")
                .document(login)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // if a document with the login value exists, add the call log to that document
                        firestoreDB.collection("callhistory")
                                .document(login)
                                .collection("callhistory")
                                .add(callHistoryMap)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d(TAG, "Call log added to Firestore: " + documentReference.getId());
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Failed to add call log to Firestore", e);
                                });
                    } else {
                        // if no document with the login value exists, create a new document with the login value
                        firestoreDB.collection("callhistory")
                                .document(login)
                                .set(Collections.singletonMap("name", login))
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "New document created in Firestore for login: ");
                                    firestoreDB.collection("callhistory")
                                            .document(login)
                                            .collection("callhistory")
                                            .add(callHistoryMap)
                                            .addOnSuccessListener(documentReference -> {
                                                Log.d(TAG, "Call log added to Firestore: " + documentReference.getId());
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.w(TAG, "Failed to add call log to Firestore", e);
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Failed to create new document in Firestore for login: " + e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to query Firestore for login: " + e);
                });

    }

    private String getSimSerialNumber() {
        TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
            return manager.getSimOperator();
        }
        return null;
    }

    private class Call {
        private final String id;

        private String date;
        private String number;
        private String simSlotId;
        private String direction;

        public Call(String date, String number, String simSlotId, String direction) {
            this.date = date;
            this.number = number;
            this.simSlotId = simSlotId;
            this.direction = direction;
            this.id = generateId();
        }

        public String getId() {
            return id;
        }

        private String generateId() {
            return date + number + simSlotId + direction; // Change this to generate a unique ID for your use case
        }

        public String getDate() {
            return date;
        }

        public String getNumber() {
            return number;
        }

        public String getSimSlotId() {
            return simSlotId;
        }

        public String getDirection() {
            return direction;
        }
    }

}
