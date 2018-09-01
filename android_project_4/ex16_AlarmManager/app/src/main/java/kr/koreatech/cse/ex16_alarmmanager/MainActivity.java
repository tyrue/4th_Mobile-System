package kr.koreatech.cse.ex16_alarmmanager;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.List;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    TextView scanResultText;

    WifiManager wifiManager;
    List<ScanResult> scanResultList;

    boolean isPermitted = false;
    final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;

    AlarmManager am;
    PendingIntent pendingIntent;

    // Alarm 시간이 되었을 때 안드로이드 시스템이 전송해주는 broadcast를 받을 receiver 정의
    // 그리고 다시 동일 시간 후 alarm이 발생하도록 설정한다.
    private BroadcastReceiver AlarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("kr.ac.koreatech.msp.alarm")) {

                //*****************
                // Alarm이 발생하였을 때 wifi scan을 수행한다
                wifiManager.startScan();
                scanResultText.setText("");
                //-----------------
                // Alarm receiver에서는 장시간에 걸친 연산을 수행하지 않도록 한다
                // Alarm을 발생할 때 안드로이드 시스템에서 wakelock을 잡기 때문에 CPU를 사용할 수 있지만
                // 그 시간은 제한적이기 때문에 애플리케이션에서 필요하면 wakelock을 잡아서 연산을 수행해야 함
                //*****************

                Intent in = new Intent("kr.ac.koreatech.msp.alarm");
                pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, in, 0);
                am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + 5000, pendingIntent);
            }

        }
    };

    // Wifi scan 결과를 받는 용도로 사용하는 Broadcast Recevier
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                getWifiInfo();
        }
    };

    private void getWifiInfo() {
        scanResultList = wifiManager.getScanResults();
        scanResultText.setText("===================================\n");
        for(int i = 0; i < scanResultList.size(); i++) {
            ScanResult result = scanResultList.get(i);
            scanResultText.append((i+1) + "- SSID: " + result.SSID + "\t RSSI: " + result.level + " dBm\n");
        }
        scanResultText.append("===================================");
        for(int i = 0; i < scanResultList.size(); i++)
           Log.d("scan Result", scanResultList.get(i).BSSID + ", " + scanResultList.get(i).level + "\n");
        Log.d("scan Result", "끝\n");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestRuntimePermission();

        scanResultText = (TextView)findViewById(R.id.result);

        wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        if(wifiManager.isWifiEnabled() == false)
            wifiManager.setWifiEnabled(true);

        am = (AlarmManager)getSystemService(ALARM_SERVICE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mReceiver);

        try {
            // Alarm 발생 시 전송되는 broadcast 수신 receiver를 해제
            unregisterReceiver(AlarmReceiver);
        } catch(IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        // AlarmManager에 등록한 alarm 취소
        am.cancel(pendingIntent);
    }

    public void onClick(View view) {
        if(view.getId() == R.id.start) {
            //Toast.makeText(this, "WiFi scan start!!", Toast.LENGTH_LONG).show();
            startScan();
        }
    }

    private void startScan() {
        // wifi scan 결과 발생 시 전송되는 broadcast를 수신할 receiver 등록
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);

        // Alarm 발생 시 전송되는 broadcast를 수신할 receiver 등록
        IntentFilter intentFilter = new IntentFilter("kr.ac.koreatech.msp.alarm");
        registerReceiver(AlarmReceiver, intentFilter);

        // Alarm이 발생할 시간이 되었을 때, 안드로이드 시스템에 전송을 요청할 broadcast를 지정
        Intent intent = new Intent("kr.ac.koreatech.msp.alarm");
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        // Alarm이 발생할 시간 및 alarm 발생시 이용할 pending intent 설정
        // 10초 후 alarm 발생
        //
        am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 5000, pendingIntent);
    }

    private void requestRuntimePermission() {
        //*******************************************************************
        // Runtime permission check
        //*******************************************************************
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            }
        } else {
            // ACCESS_FINE_LOCATION 권한이 있는 것
            isPermitted = true;
        }
        //*********************************************************************
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // read_external_storage-related task you need to do.

                    // ACCESS_FINE_LOCATION 권한을 얻음
                    isPermitted = true;

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    // 권한을 얻지 못 하였으므로 location 요청 작업을 수행할 수 없다
                    // 적절히 대처한다
                    isPermitted = false;

                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}