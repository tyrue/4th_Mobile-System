package kr.koreatech.cse.ex18_hslocationtracking;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HSMonitorService extends Service
{
    private static final String LOGTAG = "HS_Location_Tracking";
    private static final String BROADCAST_ACTION_ACTIVITY = "kr.ac.koreatech.msp.hslocationtracking";
    AlarmManager am;
    PendingIntent pendingIntent;

    private PowerManager.WakeLock wakeLock;
    private CountDownTimer timer;

    private StepMonitor accelMonitor;
    private long period = 10000;
    private static final long activeTime = 1000;
    private static final long periodForMoving = 5000;
    private static final long periodIncrement = 5000;
    private static final long periodMax = 30000;

    private LocationManager mLocationManager = null;
    private final int MIN_TIME_UPDATES = 5000; // milliseconds
    private final int MIN_DISTANCE_CHANGE_FOR_UPDATES = 5; // meters
    private boolean isRequestRegistered = false;

    private double lon = 0.0;
    private double lat = 0.0;

    // Alarm 시간이 되었을 때 안드로이드 시스템이 전송해주는 broadcast를 받을 receiver 정의
    // 움직임 여부에 따라 다음 alarm이 발생하도록 설정한다.
    private BroadcastReceiver AlarmReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(intent.getAction().equals("kr.ac.koreatech.msp.hsalarm"))
            {
                Log.d(LOGTAG, "Alarm fired!!!!");
                //-----------------
                // Alarm receiver에서는 장시간에 걸친 연산을 수행하지 않도록 한다
                // Alarm을 발생할 때 안드로이드 시스템에서 wakelock을 잡기 때문에 CPU를 사용할 수 있지만
                // 그 시간은 제한적이기 때문에 애플리케이션에서 필요하면 wakelock을 잡아서 연산을 수행해야 함
                //-----------------

                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HS_Wakelock");
                // ACQUIRE a wakelock here to collect and process accelerometer data and control location updates
                wakeLock.acquire();

                accelMonitor = new StepMonitor(context);
                accelMonitor.onStart();

                timer = new CountDownTimer(activeTime, 1000)
                {
                    @Override
                    public void onTick(long millisUntilFinished)
                    {
                    }

                    @Override
                    public void onFinish()
                    {
                        Log.d(LOGTAG, "1-second accel data collected!!");
                        // stop the accel data update
                        accelMonitor.onStop();

                        boolean moving = accelMonitor.isMoving();
                        // 움직임 여부에 따라 GPS location update 요청 처리
                        if(moving)
                        {
                            Log.d(LOGTAG, "before calling requestLocation");
                            if(!isRequestRegistered)
                            {
                                requestLocation();
                                Log.d(LOGTAG, "after calling requestLocation");
                            }
                        }
                        else
                        {
                            Log.d(LOGTAG, "before calling cancelLocationRequest");
                            if(isRequestRegistered)
                            {
                                cancelLocationRequest();
                                Log.d(LOGTAG, "after calling cancelLocationRequest");
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
                timer.start();
            }
        }
    };

    private void requestLocation()
    {
        try
        {
            if(mLocationManager == null)
            {
                Log.d(LOGTAG, "mLocMan obtained");
                mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            }
            if(!isRequestRegistered)
            {
                // min time과 min distance를 0으로 설정했을 때와 아닌 경우 비교

                /*
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MIN_TIME_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        locationListener);
                */
                /*
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        0,
                        0,
                        locationListener);
                */
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        3000,
                        0,
                        locationListener);

                isRequestRegistered = true;
            }
        }
        catch (SecurityException se)
        {
            se.printStackTrace();
            Log.e(LOGTAG, "PERMISSION_NOT_GRANTED");
        }
    }

    private void cancelLocationRequest()
    {
        Log.d(LOGTAG, "Cancel the location update request");
        if(mLocationManager != null)
        {
            try
            {
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
    }

    LocationListener locationListener = new LocationListener()
    {
        public void onLocationChanged(Location location)
        {
            Log.d(LOGTAG, " Time : " + getCurrentTime() + " Longitude : " + location.getLongitude()
                    + " Latitude : " + location.getLatitude() + " Altitude: " + location.getAltitude()
                    + " Accuracy : " + location.getAccuracy());
            lon = location.getLongitude();
            lat = location.getLatitude();
        }

        public void onStatusChanged(String provider, int status, Bundle bundle)
        {
            Log.d(LOGTAG, "GPS status changed. status code: " + status);
            if(status == 2)
                Log.d(LOGTAG, "status: Available");
            else if(status == 1)
                Log.d(LOGTAG, "status: Temporarily unavailable");
            else if(status == 0)
                Log.d(LOGTAG, "status: Out of service");
            Toast.makeText(getApplicationContext(), "GPS status changed.", Toast.LENGTH_SHORT).show();
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

    public String getCurrentTime()
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.KOREA);
        Date currentTime = new Date();
        String dTime = formatter.format(currentTime);
        return dTime;
    }

    private void setNextAlarm(boolean moving)
    {
        // 움직임이면 5초 period로 등록
        // 움직임이 아니면 5초 증가, max 30초로 제한
        if(moving)
        {
            Log.d(LOGTAG, "MOVING!!");
            period = periodForMoving;
        }
        else
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
        am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + period - activeTime, pendingIntent);
    }

    private void sendDataToActivity(boolean moving)
    {
        // 화면에 정보 표시를 위해 activity의 broadcast receiver가 받을 수 있도록 broadcast 전송
        Intent intent = new Intent(BROADCAST_ACTION_ACTIVITY);
        intent.putExtra("moving", moving);
        intent.putExtra("longitude", lon);
        intent.putExtra("latitude", lat);
        // broadcast 전송
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onCreate()
    {

        Log.d(LOGTAG, "onCreate");

        // Alarm 발생 시 전송되는 broadcast를 수신할 receiver 등록
        IntentFilter intentFilter = new IntentFilter("kr.ac.koreatech.msp.hsalarm");
        registerReceiver(AlarmReceiver, intentFilter);

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
        // 설정한 시간 (5000-> 5초, 10000->10초) 후 alarm 발생
        am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + period, pendingIntent);

        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy()
    {
        Toast.makeText(this, "Activity Monitor 중지", Toast.LENGTH_SHORT).show();

        try
        {
            // Alarm 발생 시 전송되는 broadcast 수신 receiver를 해제
            unregisterReceiver(AlarmReceiver);
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
    }
}