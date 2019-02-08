package kr.koreatech.cse.termproject;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static kr.koreatech.cse.termproject.CalcTime.timegap;

public class HSMonitorService extends Service
{
    private static final String LOGTAG = "HS_Location_Tracking";
    private static final String BROADCAST_ACTION_ACTIVITY = "kr.ac.koreatech.msp.hslocationtracking";

    private PowerManager.WakeLock wakeLock;          // 파워 매니저
    private CountDownTimer timer;                    // 타이머

    private StepMonitor accelMonitor;               // 스텝 모니터
    private long period = 7000;                    // 주기
    private static final long activeTime = 2000;        // 활동 시간
    private static final long periodForMoving = 5000;   // 움직일 때
    private static final long periodIncrement = 1000;   // 증가하는 주기 시간
    private static final long periodMax = 30000;         // 최대 주기 시간

    private String startMovingTime = "";        // 움직이기 시작할 때의 시간
    private String startStopTime = "";          // 정지하기 시작할 때의 시간

    private LocationManager mLocationManager = null;       // 위치 매니저
    private boolean isRequestRegistered = false;        // 위치 매니저 사용 여부
    private boolean isGpsEnable = false;                  // gps 사용 여부

    private class Location_data // 등록된 좌표 경보의 데이터 클래스
    {
        public   String name;         // 등록된 경보의 이름
        public   double latitude;   // 등록된 경보의 위도
        public   double longitude;  // 등록된 경보의 경도
        public   int range;          // 등록된 경보의 범위
        public Location_data(String name, double latitude, double longitude, int range)
        {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.range = range;
        }
    }
    ArrayList<PendingIntent> proximityIntent_l = new ArrayList<>(); // 각각 다른 위치의 경보를 전해주기 위해 팬딩인텐트를 리스트로 함
    ArrayList<Location_data> location_data_list = new ArrayList<>();    // 데이터 클래스를 리스트 구조로 저장한다.

    private double lon = 0.0;   // 위도
    private double lat = 0.0;   // 경도

    WifiManager wifiManager;  // 와이파이 매니저
    List<ScanResult> scanList; // 와이파이 스캔 리스트

    ArrayList<String> place_1_B, place_2_B; // 2공학관 401호, 다산정보관 1층로비 BSSID
    ArrayList<Integer> place_1_L, place_2_L; // 2공학관 401호, 다산정보관 1층로비 level

    String placeName1 = "실내", placeName2 = "실외"; // 현재 장소 이름
    // 각 장소마다 저장한 와이파이 정보와 스캔 결과가 몇개가 같은지 저장하는 카운트 배열
    int[] counter;
    int totalStep = 0;

    Vibrator vib;       // 진동
    Notification.Builder noti; // 서비스를 포그라운드로 실행시키기 위한 Notification 객체

    AlarmManager am;                // 알람 매니저
    PendingIntent pendingIntent;

    // Alarm 시간이 되었을 때 안드로이드 시스템이 전송해주는 broadcast를 받을 receiver 정의
    // 움직임 여부에 따라 다음 alarm이 발생하도록 설정한다.
    // 와이파이 스캔 결과도 받는다.
    private BroadcastReceiver MyReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(intent.getAction().equals("kr.ac.koreatech.msp.locationAlert")) // 등록한 경보에 접근 할 때
            {
                Log.d(LOGTAG, "접근 경보!!!!");
                Toast.makeText(context, "접근 경보!", Toast.LENGTH_LONG).show();
                // 접근 경보에 대한 상태
                boolean isEntering = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);
                placeName2 = intent.getStringExtra("loc_name"); // 현재 접근한 경보의 이름
                if(isEntering) // 접근했다면
                {
                    Toast.makeText(context, placeName2 + "에 접근중입니다..", Toast.LENGTH_LONG).show();
                }
                else // 벗어났다면 그냥 실외라고 칭함
                {
                    Toast.makeText(context, placeName2 + "에서 벗어납니다..", Toast.LENGTH_LONG).show();
                    placeName2 = "실외";
                }
            }
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) // 와이파이 스캔했을 때
            {
                if(!startStopTime.equals(""))
                {
                    whatIsPlace();   // 스캔정보를 분석하여 등록된 장소인지 판단한다.
                }
            }
            if(intent.getAction().equals("kr.ac.koreatech.msp.hsalarm")) // 알람이 울릴 때
            {
                Log.d(LOGTAG, "알람이 되었다!!!!");
                //-----------------
                // Alarm receiver에서는 장시간에 걸친 연산을 수행하지 않도록 한다
                // Alarm을 발생할 때 안드로이드 시스템에서 wakelock을 잡기 때문에 CPU를 사용할 수 있지만
                // 그 시간은 제한적이기 때문에 애플리케이션에서 필요하면 wakelock을 잡아서 연산을 수행해야 함
                //-----------------

                // WakeLock을 잡는다.
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HS_Wakelock");
                wakeLock.acquire();

                // 스텝모니터 초기화
                accelMonitor = new StepMonitor(context);
                accelMonitor.onStart();

                // 2초간 카운트 다운
                timer = new CountDownTimer(activeTime, 1000)
                {
                    @Override
                    public void onTick(long millisUntilFinished)
                    {
                    }
                    @Override
                    public void onFinish() // 카운트가 끝날 때
                    {
                        Log.d(LOGTAG, "2초간 센서 작동 완료!!");
                        accelMonitor.onStop();  // 스텝모니터 정지

                        boolean moving = accelMonitor.isMoving();
                        // 움직임 여부에 따라 GPS location update 요청 처리
                        if(moving)  // 움직이면 처음 시간 저장함, StepMonitor로 부터 카운트 횟수 받아옴
                        {
                            Intent in = new Intent("kr.ac.koreatech.msp.adcstepmonitor.moving"); // 액티비티한테 움직였다고 보냄
                            in.putExtra("moving", true);
                            sendBroadcast(in);

                            if(!isRequestRegistered)    // 위치 요청
                            {
                                Log.d(LOGTAG, "위치 요청 전");
                                requestLocation(); // 권한 요청
                                Log.d(LOGTAG, "위치 요청 후");
                            }

                            totalStep += accelMonitor.getMovementCount(); // 총 걸음 수 누적중..
                            String h = CalcTime.getCurrentHour();   // 현재시간 받아옴
                            if(startMovingTime.equals("")) startMovingTime = h; // 움직인 시간 저장
                            Log.d(LOGTAG, "h : " + h);

                            if(!startStopTime.equals("") && startStopTime != null)   // 정지 시간이 있다면
                            {
                                int t = CalcTime.timegap(startStopTime, h); // 지금까지 정지한 시간 계산
                                Log.d(LOGTAG, "timegap : " + t);
                                Log.d(LOGTAG, "stopTime : " + startStopTime + "currenTime : " + h);
                                if(t >= 5) // 정지시간이 5분 이상이면 파일에 저장, 리스트 뷰 띄움
                                {
                                    if(!isGpsEnable)    // gps 사용 불가
                                    {
                                        wifiManager.startScan();   // 실내라면 와이파이 스캔
                                        LogData d = new LogData(startStopTime, h, false, placeName1, 0);
                                        sendStringIntent(d.toString()); // 엑티비티에 내용 보냄
                                    }
                                    else    // gps 사용 가능
                                    {
                                        setLocationAlert(); // gps 접근 경보 등록
                                        LogData d = new LogData(startStopTime, h, false, placeName2, 0);
                                        sendStringIntent(d.toString());
                                        removeLocationAlert(); // 접근 경보 해제
                                    }
                                }
                                startStopTime = "";          // 정지 시간을 초기화
                            }
                        }
                        else // 멈춰있다면
                        {
                            Intent in = new Intent("kr.ac.koreatech.msp.adcstepmonitor.moving");
                            in.putExtra("moving", false); // 정지했다고 알림
                            sendBroadcast(in);

                            if(isRequestRegistered) // 위치 취소
                            {
                                Log.d(LOGTAG, "위치 취소 요청 전");
                                cancelLocationRequest(); // 권한 취소
                                Log.d(LOGTAG, "위치 취소 요청 후");
                            }

                            String h = CalcTime.getCurrentHour(); // 현재 시간 받아옴
                            Log.d(LOGTAG, "h : " + h);
                            if(startStopTime.equals("")) startStopTime = h; // 정지하기 시작한 시간
                            if(!startMovingTime.equals("") && startMovingTime != null) // 움직인 시간이 있다면
                            {
                                int t = CalcTime.timegap(startMovingTime, h); // 움직인 시간 계산
                                Log.d(LOGTAG, "timegap : " + t);
                                Log.d(LOGTAG, "stopTime : " + startStopTime + "currenTime : " + h);
                                if(t >= 1) // 이동 시간이 1분 이상이면 파일에 저장, 리스트 뷰 띄움
                                {
                                    LogData d = new LogData(startMovingTime, h, true, "", totalStep);
                                    sendStringIntent(d.toString()); // 움직인 내용 보냄

                                    // 여기서 현재 걸음수 출력
                                    in.setAction("kr.ac.koreatech.msp.adcstepmonitor.step");
                                    in.putExtra("step_t", totalStep);
                                    sendBroadcast(in);
                                    totalStep = 0; // 현재 걸음 수 초기화
                                }
                                startMovingTime = "";          // 이동 시간을 초기화
                            }
                        }
                        // 움직임 여부에 따라 다음 alarm 설정
                        setNextAlarm(moving);

                        // 화면에 위치 데이터를 표시할 수 있도록 브로드캐스트 전송
                        sendDataToActivity(moving);

                        // When you finish your job, RELEASE the wakelock
                        wakeLock.release();
                        wakeLock = null;
                    }
                };
                timer.start();  // 타이머 시작
            }
        }
    };

    // 스캔 결과를 받아 등록된 장소인지 분석하는 메소드
    private void whatIsPlace()
    {
        scanList = wifiManager.getScanResults(); // 와이파이 스캔 리스트를 저장
        Collections.sort(scanList, descend);    // 스캔 리스트를 level값에 따라 내림차순 정렬한다.

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
                    if(counter[0] >= 2)
                    {
                        placeName1 = "2공학관 401";
                        return;
                    }
                }

                else if(result.BSSID.equals(place_2_B.get(j)) && result.level >= place_2_L.get(j) - 5)
                {
                    counter[1]++;
                    if(counter[1] >= 2)
                    {
                        placeName1 = "다산 1층 로비";
                        return;
                    }
                }
            }
        }
        placeName1 = "실내";
    }

    @Override
    public void onCreate()
    {
        Log.d(LOGTAG, "onCreate");
        // 정보를 저장할 리스트 생성
        // BSSID
        place_1_B = new ArrayList<String>();
        place_2_B = new ArrayList<String>();
        // level
        place_1_L = new ArrayList<Integer>();
        place_2_L = new ArrayList<Integer>();

        // 각 장소에 등록되어 있는 와이파이 정보(BSSID, RSSI)를 저장함
        // 미리 해당 장소에 가서 와이파이 스캔한 정보를 바탕으로 작성함
        // 2공학관 401
        place_1_B.add("18:80:90:c6:7b:20"); place_1_L.add(-48);
        place_1_B.add("18:80:90:c6:7b:21"); place_1_L.add(-48);
        place_1_B.add("18:80:90:c6:7b:22"); place_1_L.add(-48);
        // 다산 정보관 1층 로비
        place_2_B.add("20:3a:07:9e:a6:c5"); place_2_L.add(-60);
        place_2_B.add("20:3a:07:9e:a6:c1"); place_2_L.add(-61);
        place_2_B.add("20:3a:07:9e:a6:c0"); place_2_L.add(-62);

        // gps 좌표
        Location_data data1 = new Location_data("운동장", 36.762581, 127.284527, 80);
        Location_data data2 = new Location_data("잔디광장 벤치", 36.764215, 127.282173, 50);
        location_data_list.add(data1);
        location_data_list.add(data2);

        try
        {
            wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE); // 와이파이 매니저
            mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 0, locationListener);
        }
        catch (SecurityException se)
        {
            se.printStackTrace();
            Log.e(LOGTAG, "PERMISSION_NOT_GRANTED");
        }

        counter = new int[2];   // 카운터
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

        // broadcast를 수신할 receiver 등록
        IntentFilter intentFilter = new IntentFilter("kr.ac.koreatech.msp.hsalarm"); // 알람
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);                   // wifi 스캔
        intentFilter.addAction("kr.ac.koreatech.msp.locationAlert");                        // gps 좌표
        registerReceiver(MyReceiver, intentFilter);

        // AlarmManager 객체 얻기
        am = (AlarmManager)getSystemService(ALARM_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // intent: startService() 호출 시 넘기는 intent 객체
        // flags: service start 요청에 대한 부가 정보. 0, START_FLAG_REDELIVERY, START_FLAG_RETRY
        // startId: start 요청을 나타내는 unique integer id
        Log.d(LOGTAG, "onStartCommand");
        Toast.makeText(this, "Activity Monitor 시작", Toast.LENGTH_SHORT).show();

        // Alarm이 발생할 시간이 되었을 때, 안드로이드 시스템에 전송을 요청할 broadcast를 지정
        Intent in = new Intent("kr.ac.koreatech.msp.hsalarm");
        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, in, 0);

        // Alarm이 발생할 시간 및 alarm 발생시 이용할 pending intent 설정
        am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + period, pendingIntent);

        sendStringIntent(CalcTime.getCurrentTime() + " 모니터링 시작\n");

        return super.onStartCommand(intent, flags, startId);
    }

    private void requestLocation() // 위치 요청
    {
        try
        {
            if(mLocationManager == null)    // 위치 매니저가 초기화 안됐을 때
            {
                Log.d(LOGTAG, "로케이션 매니저 초기화");
                mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 0, locationListener);
            }
            isRequestRegistered = true; // 요청 됨
        }
        catch (SecurityException se)
        {
            se.printStackTrace();
            Log.e(LOGTAG, "PERMISSION_NOT_GRANTED");
        }
    }

    private void cancelLocationRequest() // 위치 받기 취소
    {
        Log.d(LOGTAG, "위치 업데이트 취소");
        if(mLocationManager != null)
        {
            try
            {
                removeLocationAlert();
                mLocationManager.removeUpdates(locationListener);
            }
            catch(SecurityException se)
            {
                se.printStackTrace();
                Log.e(LOGTAG, "PERMISSION_NOT_GRANTED");
            }
        }
        mLocationManager = null;
        isRequestRegistered = false;
        isGpsEnable = false;
    }

    LocationListener locationListener = new LocationListener() // 위치리스너
    {
        public void onLocationChanged(Location location) // 위치가 바뀔 때
        {
            String provider = location.getProvider();
            Log.d(LOGTAG, " Time : " + CalcTime.getCurrentTime() + " Longitude : " + location.getLongitude()
                    + " Latitude : " + location.getLatitude() + " Altitude: " + location.getAltitude()
                    + " Accuracy : " + location.getAccuracy() + "provider : " + provider);

            Toast.makeText(getApplicationContext(), "provider : " + provider, Toast.LENGTH_SHORT).show();
            if(provider.equals("gps")) isGpsEnable = true;
            else isGpsEnable = false;

            lon = location.getLongitude();
            lat = location.getLatitude();
        }

        public void onStatusChanged(String provider, int status, Bundle bundle) // 상태 변화
        {
            Log.d(LOGTAG, "GPS 상태가 변했습니다. status code: " + status);
            Log.d(LOGTAG, "status provider: " + provider);
            if(status == 2)
            {
                Log.d(LOGTAG, "status: Available");
                Toast.makeText(getApplicationContext(), "GPS status Available.", Toast.LENGTH_SHORT).show();
            }
            else if(status == 1)
            {
                Log.d(LOGTAG, "status: Temporarily unavailable");
                Toast.makeText(getApplicationContext(), "GPS status unavailable.", Toast.LENGTH_SHORT).show();
            }
            else if(status == 0)
            {
                Log.d(LOGTAG, "status: Out of service");
                Toast.makeText(getApplicationContext(), "GPS Out of service", Toast.LENGTH_SHORT).show();
            }
            Toast.makeText(getApplicationContext(), "provider : " + provider, Toast.LENGTH_SHORT).show();
        }

        public void onProviderEnabled(String provider)
        {
            Log.d(LOGTAG, "GPS onProviderEnabled: " + provider);
        }

        public void onProviderDisabled(String provider)
        {
            Log.d(LOGTAG, "GPS onProviderDisabled: " + provider);
            Toast.makeText(getApplicationContext(), "GPS is off, please turn on!", Toast.LENGTH_LONG).show();
        }
    };

    private void setLocationAlert() // gps 좌표에 따른 접근 경보 설정
    {
        try
        {
            for(int i = 0; i < location_data_list.size(); i++)
            {
                // ProximityAlert 등록을 위한 PendingIntent 객체 얻기
                Intent intent = new Intent("kr.ac.koreatech.msp.locationAlert");
                intent.putExtra("loc_name", location_data_list.get(i).name); // 접근 경보의 이름을 전달함
                PendingIntent proximityIntent = PendingIntent.getBroadcast(this, i, intent, 0); // PendingIntent를 설정한다.
                proximityIntent_l.add(proximityIntent); // PendingIntent리스트에 PendingIntent을 추가함

                // 데이터 리스트에 있는 데이터를 순서대로 추출
                double lat = location_data_list.get(i).latitude; // 위도
                double lon = location_data_list.get(i).longitude; // 경도
                int ra = location_data_list.get(i).range;         // 범위
                mLocationManager.addProximityAlert(lat, lon, ra, -1, proximityIntent); // 접근 경보을 등록하고 방송한다.
            }
        }
        catch (SecurityException e)
        { // 예외 처리
            e.printStackTrace();
        }
    }

    private void removeLocationAlert() // gps 좌표에 따른 접근 경보 해제
    {
        try
        {
            if(proximityIntent_l.size() != 0) // 현재 PendingIntent리스트에 있는 모든 PendingIntent에 대한 접근 경보를 해제한다.
            {
                for(int i = 0; i < proximityIntent_l.size(); i++)
                    mLocationManager.removeProximityAlert(proximityIntent_l.get(i));
                proximityIntent_l.clear(); // 리스트 초기화
            }
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void sendStringIntent(String str) // 엑티비티로 내용을 보냄
    {
        Intent in = new Intent("kr.koreatech.cse.assignment3.encounter");
        in.putExtra("encounter", str);
        // 메인액티비티가 받을 수 있도록 브로드캐스트를 보낸다.
        sendBroadcast(in);
    }

    private void setNextAlarm(boolean moving)
    {
        // 움직임이면 5초 period로 등록
        // 움직임이 아니면 5초 증가, max 30초로 제한
        if(moving)  // 움직일 때 주기를 5초로
        {
            Log.d(LOGTAG, "MOVING!!");
            period = periodForMoving;
        }
        else       // 멈춰있을 때 주기를 늘림
        {
            Log.d(LOGTAG, "NOT MOVING!!");
            period = period + periodIncrement;
            if(period >= periodMax)
            {
                period = periodMax;
            }
        }
        Log.d(LOGTAG, "Next alarm: " + period);

        // 다음 alarm 등록
        Intent in = new Intent("kr.ac.koreatech.msp.hsalarm");
        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, in, 0);
        // 정확하게 부팅 된 이후 시간으로 다음 알람 설정, 주기가 5초면, 활동하는 시간 1초 + 쉬는 시간 4초 이렇게 구성되는 거임
        am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + period - activeTime, pendingIntent);
    }

    private void sendDataToActivity(boolean moving) // 엑티비티로 내용을 보냄
    {
        // 화면에 정보 표시를 위해 activity의 broadcast receiver가 받을 수 있도록 broadcast 전송
        Intent intent = new Intent(BROADCAST_ACTION_ACTIVITY);
        intent.putExtra("location", "lon : " + lon + " lat : " + lat + "\n");
        // broadcast 전송
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    public void onDestroy() // 서비스 종료
    {
        Toast.makeText(this, "Activity Monitor 중지", Toast.LENGTH_SHORT).show();
        try
        {
            // Alarm 발생 시 전송되는 broadcast 수신 receiver를 해제
            unregisterReceiver(MyReceiver);
        }
        catch(IllegalArgumentException ex)
        {
            ex.printStackTrace();
        }
        // AlarmManager에 등록한 alarm 취소
        am.cancel(pendingIntent);

        // release all the resources you use
        if(timer != null)
            timer.cancel();
        if(wakeLock != null && wakeLock.isHeld())
        {
            wakeLock.release();
            wakeLock = null;
        }
        sendStringIntent(CalcTime.getCurrentTime() + " 모니터링 종료\n");
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