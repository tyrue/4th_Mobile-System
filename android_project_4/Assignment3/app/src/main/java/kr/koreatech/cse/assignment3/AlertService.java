package kr.koreatech.cse.assignment3;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class AlertService extends Service
{
    private static final String TAG = "AlertService";

    WifiManager wifiManager;  // 와이파이 매니저
    List<ScanResult> scanList; // 와이파이 스캔 리스트

    ArrayList<String> place_1_B, place_2_B, place_3_B; // 4층 엘레베이터, 408호 계단 앞, 401호 계단 앞 BSSID
    ArrayList<Integer> place_1_L, place_2_L, place_3_L; // 4층 엘레베이터, 408호 계단 앞, 401호 계단 앞 level

    String placeName; // 현재 장소 이름
    // 각 장소마다 저장한 와이파이 정보와 스캔 결과가 몇개가 같은지 저장하는 카운트 배열
    int[] counter;

    // 타이머 객체
    Timer timer = new Timer();
    TimerTask timerTask = null;

    Vibrator vib;       // 진동

    Intent in;          // 인텐트 객체
    long time;         // 현재 시간을 저장할 변수
    // 밀리초 형식인 시간을 yyyy.mm.dd HH:mm 형식으로 포맷함
    SimpleDateFormat dayTime = new SimpleDateFormat("yyyy.MM.dd HH:mm");

    Notification.Builder noti; // 서비스를 포그라운드로 실행시키기 위한 Notification 객체

    BroadcastReceiver mReceiver = new BroadcastReceiver() // 브로드캐스트 리시버
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // 와이파이를 성공적으로 스캔하면 정보를 분석함
            Log.d(TAG, "onReceive()");
            String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                checkProximity();   // 스캔정보를 분석하여 방송보낸다.
        }
    };

    // 등록 장소 근접 여부를 판단하고 그에 따라 알림을 주기 위한 메소드
    private void checkProximity()
    {
        scanList = wifiManager.getScanResults(); // 와이파이 스캔 리스트를 저장
        Collections.sort(scanList, descend);    // 스캔 리스트를 level값에 따라 내림차순 정렬한다.

        int result = whatIsPlace(scanList);     // 현재 위치한 장소 코드값을 받음
        if (result != -1)    // -1이 아니면 등록된 장소에 있다
        {
            // 검색 결과에 따른 장소 이름 저장
            if(result == 0) placeName = "4층 엘레베이터 앞";
            else if(result == 1) placeName = "408호 계단 앞";
            else if(result == 2) placeName = "401호 계단 앞";
            // 진동 패턴
            // 0초 후에 시작 => 바로 시작, 200ms 동안 진동, 100ms 동안 쉼, 200ms 동안 진동, 100ms 동안 쉼, 200ms 동안 진동
            long[] pattern = {0, 200, 100, 200, 100, 200};
            // pattern 변수로 지정된 방식으로 진동한다, -1: 반복 없음. 한번의 진동 패턴 수행 후 완료
            vib.vibrate(pattern, -1);

            Toast.makeText(this, "** " + placeName + "에 있거나 그 근처에 있습니다 **", Toast.LENGTH_SHORT).show();

            // 현재 시간을 문자열에 저장하고 이를 인텐트에 저장함
            time = System.currentTimeMillis();
            String d = dayTime.format(new Date(time));
            in.putExtra("encounter", d + " " + placeName + "\n");
            // 메인액티비티가 받을 수 있도록 브로드캐스트를 보낸다.
            in.setAction("kr.koreatech.cse.assignment3.encounter");
            sendBroadcast(in);
        }

        else if(result == -1)   // 등록된 장소가 아니다.
        {
            // 동작 확인용
            vib.vibrate(200);
            Toast.makeText(this, "** 등록된 장소 근처에 있지 않습니다 **", Toast.LENGTH_SHORT).show();
            // 현재 시간을 문자열에 저장하고 이를 인텐트에 저장함
            time = System.currentTimeMillis();
            String d = dayTime.format(new Date(time));
            in.putExtra("encounter", d + " unknown \n");
            // 메인액티비티가 받을 수 있도록 브로드캐스트를 보낸다.
            in.setAction("kr.koreatech.cse.assignment3.encounter");
            sendBroadcast(in);
        }
    }

    // 스캔 결과를 받아 등록된 장소인지 분석하는 메소드
    private int whatIsPlace(List<ScanResult> scanList)
    {
        for(int i = 0; i < counter.length; i++) // 카운터를 0으로 초기화
            counter[i] = 0;
        for (int i = 0; i < 3; i++) // 신호가 가장 센 3개의 와이파이 리스트 중, 2개 이상이 등록된 정보와 같은지 확인
        {
            ScanResult result = scanList.get(i);    // 리스트에 저장된 i번째 항목을 가져옴
            for(int j = 0; j < place_1_B.size(); j++) // 각 장소에 저장된 와이파이 정보의 개수는 같다
            {
                // result의 BSSID와 level을 각 미리 등록된 정보와 비교한다.
                // result의 level이 미리 등록된 level에 5를 뺀 값의 이상이면 카운트가 1증가함
                // 카운트가 2이상이면 장소 값을 반환함
                if(result.BSSID.equals(place_1_B.get(j)) && result.level >= place_1_L.get(j) - 5)
                {
                    counter[0]++;
                    if(counter[0] >= 2) return 0;
                }

                else if(result.BSSID.equals(place_2_B.get(j)) && result.level >= place_2_L.get(j) - 5)
                {
                    counter[1]++;
                    if(counter[1] >= 2) return 1;
                }

                else if(result.BSSID.equals(place_3_B.get(j)) && result.level >= place_3_L.get(j) - 5)
                {
                    counter[2]++;
                    if(counter[2] >= 2) return 2;
                }
            }
        }
        return -1; // 어디에도 등록되지 않은 장소를 의미함
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate()
    {
        Log.d(TAG, "onCreate()");
        // 정보를 저장할 리스트 생성
        // BSSID
        place_1_B = new ArrayList<String>();
        place_2_B = new ArrayList<String>();
        place_3_B = new ArrayList<String>();
        // level
        place_1_L = new ArrayList<Integer>();
        place_2_L = new ArrayList<Integer>();
        place_3_L = new ArrayList<Integer>();

        // 각 장소에 등록되어 있는 와이파이 정보(BSSID, RSSI)를 저장함
        // 미리 해당 장소에 가서 와이파이 스캔한 정보를 바탕으로 작성함
        // 4층 엘레베이터
        place_1_B.add("50:0f:80:b2:51:61"); place_1_L.add(-53);
        place_1_B.add("90:9f:33:39:c8:b2"); place_1_L.add(-53);
        place_1_B.add("50:0f:80:b2:51:60"); place_1_L.add(-54);
        // 408호 계단 앞
        place_2_B.add("40:01:7a:de:11:60"); place_2_L.add(-48);
        place_2_B.add("40:01:7a:de:11:65"); place_2_L.add(-49);
        place_2_B.add("40:01:7a:de:11:61"); place_2_L.add(-49);
        // 401호 계단 앞
        place_3_B.add("18:80:90:c6:7b:20"); place_3_L.add(-66);
        place_3_B.add("18:80:90:c6:7b:21"); place_3_L.add(-66);
        place_3_B.add("18:80:90:c6:7b:22"); place_3_L.add(-68);

        vib = (Vibrator) getSystemService(VIBRATOR_SERVICE); // 진동 설정
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE); // 와이파이 매니저

        // 와이파이 스캔 브로드캐스트 리시버 필터 작성
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);

        counter = new int[3];   // 카운터
        in = new Intent();       // 인텐트

        // Service를 Foreground로 실행하기 위한 과정
        // MainActivity 클래스를 실행하기 위한 Intent 객체
        Intent intent = new Intent(this, MainActivity.class);
        // Activity를 실행하기 위한 PendingIntent
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // Notification 객체 생성
        noti = new Notification.Builder(this)
                .setContentTitle("AlertService")
                .setContentText("Service is running... start an activity")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pIntent);

        // foregound service 설정 - startForeground() 메소드 호출, 위에서 생성한 nofication 객체 넘겨줌
        startForeground(123, noti.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Toast.makeText(this, "AlertService 시작", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onStartCommand()");

        // 주기적으로 wifi scan 수행하기 위한 timer 가동
        startTimerTask();

        // 현재 시간을 문자열에 저장함
        time = System.currentTimeMillis();
        String str = dayTime.format(new Date(time));

        // 현재 시간을 포함한 시작을 알리는 문자열을 인텐트에 저장함
        // 그리고 메인액티비티가 받을 수 있도록 브로드캐스트를 보냄
        in.putExtra("encounter", "모니터링 시작 - " + str + "\n");
        in.setAction("kr.koreatech.cse.assignment3.encounter");
        sendBroadcast(in);

        return START_STICKY; // 가용 메모리가 확보되면 다시 서비스를 실행
    }

    // 서비스가 종료될 때
    public void onDestroy()
    {
        Toast.makeText(this, "AlertService 중지", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onDestroy()");

        // 현재시간을 문자열에 저장
        time = System.currentTimeMillis();
        String str = dayTime.format(new Date(time));

        // 현재 시간을 포함한 종료를 알리는 문자열을 인텐트에 저장함
        // 그리고 메인액티비티가 받을 수 있도록 브로드캐스트를 보냄
        in.putExtra("encounter", "모니터링 종료 - " + str + "\n");
        in.setAction("kr.koreatech.cse.assignment3.encounter");
        sendBroadcast(in);

        stopTimerTask(); // 타이머 종료
        unregisterReceiver(mReceiver);  // 리시버 해제
    }

    // 타이머 시작 메소드
    private void startTimerTask()
    {
        // TimerTask 생성한다
        timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                wifiManager.startScan(); // 와이파이 스캔
            }
        };

        // TimerTask를 Timer를 통해 실행시킨다
        timer.schedule(timerTask, 1000, 60000); // 1초 후에 타이머를 구동하고 60초마다 반복한다
    }

    // 타이머 종료 메소드
    private void stopTimerTask()
    {
        // 모든 태스크를 중단한다
        if (timerTask != null)
        {
            timerTask.cancel();
            timerTask = null;
        }
    }

    // 와이파이 스캔 결과 리스트를 level값에 따라 내림차순 함
    private final static Comparator<ScanResult> descend = new Comparator<ScanResult>()
    {
        @Override
        public int compare(ScanResult o1, ScanResult o2)
        {
            if(o1.level > o2.level) return -1;
            else if(o1.level < o2.level) return 1;
            else return 0;
        }
    };
}