package kr.koreatech.cse.ex9_wifialert;

/**
 * Created by DooHyun on 2018-04-03.
 */

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AlertService extends Service {

    private static final String TAG = "AlertService";

    WifiManager wifiManager;
    List<ScanResult> scanList;

    String placeName;
    String top1APId;
    int top1rssi;

    Timer timer = new Timer();
    TimerTask timerTask = null;

    boolean isFirst = true;

    Vibrator vib;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                checkProximity();
        }
    };

    // 등록 장소 근접 여부를 판단하고 그에 따라 알림을 주기 위한 메소드
    private void checkProximity() {
        scanList = wifiManager.getScanResults();
        boolean isProximate = false;

        // 등록된 top1 AP가 스캔 결과에 있으며, 그 RSSI가 top1 rssi 값보다 10이하로 작을 때
        // 등록된 장소 근처에 있는 것으로 판단
        for (int i = 1; i < scanList.size(); i++) { // ?
            ScanResult result = scanList.get(i);
            if (top1APId.equals(result.SSID + result.BSSID) && result.level > top1rssi - 10)
                isProximate = true;
        }

        if (isProximate)
        {
            // 진동 패턴
            // 0초 후에 시작 => 바로 시작, 200ms 동안 진동, 100ms 동안 쉼, 200ms 동안 진동, 100ms 동안 쉼, 200ms 동안 진동
            long[] pattern = {0, 200, 100, 200, 100, 200};
            // pattern 변수로 지정된 방식으로 진동한다, -1: 반복 없음. 한번의 진동 패턴 수행 후 완료
            vib.vibrate(pattern, -1);

            Toast.makeText(this, "** " + placeName + "에 있거나 그 근처에 있습니다 **", Toast.LENGTH_SHORT).show();
        }
        else if(!isProximate)
        {
            // 동작 확인용
            vib.vibrate(200);
            Toast.makeText(this, "** " + placeName + " 근처에 있지 않습니다 **", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");

        vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // intent: startService() 호출 시 넘기는 intent 객체
        // flags: service start 요청에 대한 부가 정보. 0, START_FLAG_REDELIVERY, START_FLAG_RETRY
        // startId: start 요청을 나타내는 unique integer id

        Toast.makeText(this, "AlertService 시작", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onStartCommand()");

        // 넘어온 intent에서 등록 장소 및 AP 정보 추출
        placeName = intent.getStringExtra("place");
        top1APId = intent.getStringExtra("apid");
        top1rssi = intent.getIntExtra("rssi", -100);

        // 주기적으로 wifi scan 수행하기 위한 timer 가동
        startTimerTask();

        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        Toast.makeText(this, "AlertService 중지", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onDestroy()");

        stopTimerTask();
        unregisterReceiver(mReceiver);
    }

    private void startTimerTask()
    {
        // TimerTask 생성한다
        timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                wifiManager.startScan();
            }
        };

        // TimerTask를 Timer를 통해 실행시킨다
        timer.schedule(timerTask, 1000, 2000); // 1초 후에 타이머를 구동하고 2초마다 반복한다
        //*** Timer 클래스 메소드 이용법 참고 ***//
        // 	schedule(TimerTask task, long delay, long period)
        // http://developer.android.com/intl/ko/reference/java/util/Timer.html
        //***********************************//
    }

    private void stopTimerTask() {
        // 1. 모든 태스크를 중단한다
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }
}