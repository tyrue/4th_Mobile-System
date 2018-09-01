package kr.koreatech.cse.ex8_wifi;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity
{
    TextView scanResultText;
    WifiManager wifiManager;
    List<ScanResult> scanResultList;
    boolean isPermitted = false;
    final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    // BroadcastReceiver 정의
    // 여기서는 이전 예제에서처럼 별도의 Java class 파일로 만들지 않았는데, 어떻게 하든 상관 없음
    BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                getWifiInfo();
        }
    };

    // wifi scan 결과를 얻어서 UI의 TextView에 표시하는 기능 수행
    private void getWifiInfo()
    {
        scanResultList = wifiManager.getScanResults();
        scanResultText.setText("===================================\n");
        for(int i = 0; i < scanResultList.size(); i++)
        {
            ScanResult result = scanResultList.get(i);
            scanResultText.append((i+1) + "- SSID: " + result.SSID + "\t BSSID: " + result.BSSID + "\t RSSI: " + result.level + " dBm\n");
        }
        scanResultText.append("===================================");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestRuntimePermission();

        scanResultText = (TextView)findViewById(R.id.result);
        wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);

        // wifi가 활성화되어있는지 확인 후 꺼져 있으면 켠다
        if(wifiManager.isWifiEnabled() == false)
            wifiManager.setWifiEnabled(true);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // wifi scan 결과가 나왔을 때 전송되는 broadcast를 받기 위해
        // IntentFilter 객체를 생성하고 이를 이용하여 BroadcastReceiver 객체를 등록한다
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public void onClick(View view)
    {
        if(view.getId() == R.id.start)
        {
            Toast.makeText(this, "WiFi scan start!!", Toast.LENGTH_LONG).show();

            if(isPermitted) // 허가 받음
            {
                // wifi 스캔 시작
                wifiManager.startScan();

                scanResultText.setText(""); // 텍스트 초기화
            }
            else
                Toast.makeText(getApplicationContext(), "Location access 권한이 없습니다..", Toast.LENGTH_LONG).show();
        }
    }

    private void requestRuntimePermission() // 실행시간에 허가 받는 함수
    {
        //*******************************************************************
        // Runtime permission check
        //*******************************************************************
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) // ACCESS_FINE_LOCATION 허가를 받지 않았다면?
        {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION))
            {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            }
            else
            {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        }
        else
        {
            // ACCESS_FINE_LOCATION 권한이 있는 것
            isPermitted = true;
        }
        //*********************************************************************
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {

                    // permission was granted, yay! Do the
                    // read_external_storage-related task you need to do.

                    // ACCESS_FINE_LOCATION 권한을 얻음
                    isPermitted = true;

                }
                else
                {

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