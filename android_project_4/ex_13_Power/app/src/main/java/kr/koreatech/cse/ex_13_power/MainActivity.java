package kr.koreatech.cse.ex_13_power;

import android.Manifest;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.TextView;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity
{
    boolean isPermitted = false;
    final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    TextView ap1;
    TextView ap2;
    EditText placeName;
    String top1APId;
    int top1rssi = -100;


    WifiManager wifiManager;
    List<ScanResult> scanResultList;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                getWifiInfo();
        }
    };

    // RSSI 값이 가장 큰 AP 정보를 얻기 위한 메소드
    private void getWifiInfo()
    {
        scanResultList = wifiManager.getScanResults();
        String top1BSSID;
        String top1SSID;

        // 신호 세기가 가장 센 AP의 정보를 저장하기 위한 변수를 scanResultList의 첫번째 결과로 초기화
        // top1rssi: AP의 RSSI 값, top1BSSID: AP의 BSSID 값, top1SSID: AP의 SSID 값
        top1rssi = scanResultList.get(0).level;
        top1BSSID = scanResultList.get(0).BSSID;
        top1SSID = scanResultList.get(0).SSID;

        // RSSI 크기가 가장 큰 것의 BSSID, SSID, RSSI 값을 얻음
        for (int i = 1; i < scanResultList.size(); i++) {
            ScanResult result = scanResultList.get(i);
            if (top1rssi <= result.level) {
                top1rssi = result.level;
                top1BSSID = result.BSSID;
                top1SSID = result.SSID;
            }
        }
        // 화면의 TextView에 SSID와 BSSID를 이어붙여서 텍스트로 표시
        top1APId = top1SSID + top1BSSID;
        ap1.setText(top1APId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestRuntimePermission();

        ap1 = (TextView) findViewById(R.id.ap1);
        ap2 = (TextView) findViewById(R.id.ap2);
        placeName = (EditText) findViewById(R.id.placeName);



        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager.isWifiEnabled() == false)
            wifiManager.setWifiEnabled(true);
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 스크린을 계속 킴
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // wifi scan 결과 수신을 위한 BroadcastReceiver 등록
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // wifi scan 결과 수신용 BroadcastReceiver 등록 해제
        unregisterReceiver(mReceiver);
    }

    public void onClick(View view)
    {
        if (view.getId() == R.id.start)
        {
            // Start wifi scan 버튼이 눌렸을 때

            Toast.makeText(this, "WiFi scan start!!", Toast.LENGTH_LONG).show();

            if (isPermitted)
            {
                // wifi 스캔 시작
                wifiManager.startScan();
            }
            else
            {
                Toast.makeText(getApplicationContext(),
                        "Location access 권한이 없습니다..", Toast.LENGTH_LONG).show();
            }
            ap1.setText("");
        }
        else if (view.getId() == R.id.startAlert)
        {
            // Start proximity alert 버튼이 눌렸을 때
            // placeName이라는 변수로 참조하는 EditText에 쓰여진 장소 이름으로 proximity alert을 등록한다

            // proximity alert을 주는 것은 Service로 구현
            // Service를 AlertService라는 이름의 클래스로 구현하고 startService 메소드를 호출하여 이 Service를 시작

            if (placeName.getText().toString().equals(""))
                Toast.makeText(this, "Input your place name", Toast.LENGTH_LONG).show();
            else
            {
                String name = placeName.getText().toString();

                Intent intent = new Intent(this, AlertService.class);
                intent.putExtra("place", name);
                intent.putExtra("apid", top1APId);
                intent.putExtra("rssi", top1rssi);
                // 위에서 key 값으로 쓰인 String 값은 여러 곳에서 반복해서 사용된다면
                // String 상수로 정의해 놓고 사용하는 것이 좋음
                // 이 예제에서는 AlertService에서 쓰임

                startService(intent);
                placeName.setText("");

            }
        }
        else if (view.getId() == R.id.stopAlert)
        {
            // Stop proximity alert 버튼이 눌렸을 때
            // AlertService 동작을 중단
            stopService(new Intent(this, AlertService.class));
        }
    }

    private void requestRuntimePermission()
    {
        //*******************************************************************
        // Runtime permission check
        //*******************************************************************
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
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
                    // ACCESS_FINE_LOCATION 권한을 얻음
                    isPermitted = true;
                }
                else
                {
                    // 권한을 얻지 못 하였으므로 location 요청 작업을 수행할 수 없다
                    // 적절히 대처한다
                    isPermitted = false;
                }
                return;
            }

        }
    }
}