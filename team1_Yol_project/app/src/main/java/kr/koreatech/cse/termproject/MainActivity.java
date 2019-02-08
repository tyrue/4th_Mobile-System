package kr.koreatech.cse.termproject;


import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity"; // 메인 액티비티 로그
    private static final String BROADCAST_ACTION_ACTIVITY = "kr.ac.koreatech.msp.hslocationtracking";
    boolean isPermitted = false; // 현재 권한 상태
    boolean isFirst = true; // 처음 프로그램을 실행 시켰는지 확인하는 변수, 로그 파일을 새로 만들 수 있게 하기 위함

    final int MY_PERMISSIONS_REQUEST_LOCATION_AND_WRITE_EXTERNAL_STORAGE = 1;

    private TextView t_text;
    private TextView movingText;
    private TextView locationText;
    private TextView log_t; // 화면에 로그 기록을 띄울 텍스트 뷰
    private ListView listView;
    ArrayList<String> values = new ArrayList<>();
    ArrayAdapter<String> madapter;

    private int total_step = 0;
    // wake lock을 사용하는 경우 필요한 코드
    PowerManager pm;
    PowerManager.WakeLock wl;

    TextFileManager text_fm = new TextFileManager();  // 텍스트 파일 매니저

    // 브로드캐스트 리시버
    BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            // 사용자 액션, 스캔 기록을 받아 파일에 저장한 후, 파일을 불러 로그를 띄움
            if (action.equals("kr.koreatech.cse.assignment3.encounter"))
            {
                String str = intent.getStringExtra("encounter"); // 기록 받아옴
                Log.d("Activity", "str: " + str);
                text_fm.save_append(str); // 파일 이어쓰기 모드
                log_t.setText(text_fm.load()); // 화면에 기록
                madapter.add(str);
            }
            else if(intent.getAction().equals("kr.ac.koreatech.msp.adcstepmonitor.step")) // 총 걸음 수 보이기
            {
                total_step += intent.getIntExtra("step_t", 0);
                t_text.setText("t_step: " + total_step);
            }
            else if(intent.getAction().equals("kr.ac.koreatech.msp.adcstepmonitor.moving")) // 움직임 여부 확인
            {
                boolean moving = intent.getBooleanExtra("moving", false);
                if(moving)
                {
                    movingText.setText("Moving");
                }
                else
                {
                    movingText.setText("NOT Moving");
                }
                double lon = intent.getDoubleExtra("longitude", 0.0); // 위도
                double lat = intent.getDoubleExtra("latitude", 0.0);  // 경도
                locationText.setText("Location: longitude " + lon + " latitude " + lat);
            }
            else if(intent.getAction().equals(BROADCAST_ACTION_ACTIVITY)) //
            {
                locationText.setText("location : " + intent.getStringExtra("location"));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestRuntimePermission();     // 권한 요청

        log_t = (TextView) findViewById(R.id.log_t);        // 로그 기록을 보여주는 텍스트 뷰
        log_t.setMovementMethod(new ScrollingMovementMethod()); // 텍스트뷰에 스크롤을 할 수 있도록 했다

        t_text = (TextView)findViewById(R.id.t_step);
        movingText = (TextView)findViewById(R.id.moving);
        locationText = (TextView)findViewById(R.id.location);
        listView = (ListView)findViewById(R.id.list_view);

        // wake lock을 사용하는 경우 필요한 코드
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Tag: partial wake lock");
        wl.acquire();

        // 결과 수신을 위한 BroadcastReceiver 및 사용자 필터 등록
        IntentFilter filter = new IntentFilter();
        filter.addAction("kr.koreatech.cse.assignment3.encounter");
        filter.addAction("kr.ac.koreatech.msp.adcstepmonitor.step");
        filter.addAction("kr.ac.koreatech.msp.adcstepmonitor.moving");
        filter.addAction(BROADCAST_ACTION_ACTIVITY);
        registerReceiver(mReceiver, filter);

        madapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, values);
        listView.setAdapter(madapter);  // 리스트 뷰 어댑터 설정
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() // 액티비티(프로그램)이 멈출 때
    {
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        unregisterReceiver(mReceiver); // 서비스 해제
        wl.release();   // wakelock 해제
    }

    public void onClick(View view)
    {
        if(view.getId() == R.id.startMonitor)
        {
            Intent intent = new Intent(this, HSMonitorService.class);
            if(isFirst == true)
            {
                text_fm.save_new(" "); // 파일 이어쓰기 모드
                isFirst = false;
            }
            startService(intent);
        }
        else if(view.getId() == R.id.stopMonitor)
        {
            stopService(new Intent(this, HSMonitorService.class));
        }

    }

    private void requestRuntimePermission() // 권한을 받는 함수
    {
        // 파일 쓰기 접근과 위치 확인 권한 요청함
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) // 사용자가 위치 권한요청을 거부한 경우
            {

            }
            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) // 사용자가 파일 권한요청을 거부한 경우
            {

            }
            else
            {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_LOCATION_AND_WRITE_EXTERNAL_STORAGE);
            }
        }
        else
        {
            // 권한이 있는 것
            isPermitted = true;
        }
    }

    // 권한요청에 따른 행동
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case MY_PERMISSIONS_REQUEST_LOCATION_AND_WRITE_EXTERNAL_STORAGE:
            {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    isPermitted = true; // 권한을 얻음
                }
                else
                {
                    // 권한을 얻지 못 하였으므로 요청 작업을 수행할 수 없다
                    isPermitted = false;
                }
                return;
            }
        }
    }
}
