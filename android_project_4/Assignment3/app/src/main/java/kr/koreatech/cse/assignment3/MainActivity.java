package kr.koreatech.cse.assignment3;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
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
    private static final String TAG = "MainActivity"; // 메인 액티비티 로그
    boolean isPermitted = false; // 현재 권한 상태
    final int MY_PERMISSIONS_REQUEST_LOCATION_AND_WRITE_EXTERNAL_STORAGE = 1;

    TextView log_t; // 화면에 로그 기록을 띄울 텍스트 뷰

    // wake lock을 사용하는 경우 필요한 코드
    PowerManager pm;
    PowerManager.WakeLock wl;

    boolean isFirst = true; // 처음 프로그램을 실행 시켰는지 확인하는 변수, 로그 파일을 새로 만들 수 있게 하기 위함
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

        // wake lock을 사용하는 경우 필요한 코드
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Tag: partial wake lock");
        wl.acquire();

        // wifi scan 결과 수신을 위한 BroadcastReceiver 및 사용자 필터 등록
        IntentFilter filter = new IntentFilter();
        filter.addAction("kr.koreatech.cse.assignment3.encounter");
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d(TAG, "onResume");
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
        if (view.getId() == R.id.startAlert) // 서비스 시작 버튼
        {
            if(isFirst == true) // 처음 시작함
            {
                text_fm.save_new(" "); // 파일 새로 쓰기 모드, 파일에 빈칸을 넣어 초기화 함
                isFirst = false;
            }
            Intent intent = new Intent(this, AlertService.class);
            startService(intent);   // 서비스 시작
        }
        else if (view.getId() == R.id.stopAlert) // 서비스 중단 버튼
        {
            // AlertService 동작을 중단
            stopService(new Intent(this, AlertService.class));
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