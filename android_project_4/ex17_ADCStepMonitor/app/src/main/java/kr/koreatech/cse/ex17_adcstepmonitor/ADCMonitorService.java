package kr.koreatech.cse.ex17_adcstepmonitor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class ADCMonitorService extends Service
{
    private static final String LOGTAG = "ADC_Step_Monitor";
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

    // Alarm 시간이 되었을 때 안드로이드 시스템이 전송해주는 broadcast를 받을 receiver 정의
    // 움직임 여부에 따라 기준에 맞게 다음 alarm이 발생하도록 설정한다.
    private BroadcastReceiver AlarmReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(intent.getAction().equals("kr.ac.koreatech.msp.adcalarm"))
            {
                Log.d(LOGTAG, "Alarm fired!!!!");
                //-----------------
                // Alarm receiver에서는 장시간에 걸친 연산을 수행하지 않도록 한다
                // Alarm을 발생할 때 안드로이드 시스템에서 wakelock을 잡기 때문에 CPU를 사용할 수 있지만
                // 그 시간은 제한적이기 때문에 애플리케이션에서 필요하면 wakelock을 잡아서 연산을 수행해야 함
                //-----------------

                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AdaptiveDutyCyclingStepMonitor_Wakelock");
                // ACQUIRE a wakelock here to collect and process accelerometer data
                wakeLock.acquire();

                accelMonitor = new StepMonitor(context);
                accelMonitor.onStart();

                // 1초 카운트다운
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

                        // 움직임 여부에 따라 다음 alarm 설정
                        boolean moving = accelMonitor.isMoving();
                        setNextAlarm(moving);

                        // 화면에 움지임 여부를 표시할 수 있도록 브로드캐스트 전송
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

    private void setNextAlarm(boolean moving)
    {
        // 움직임이면 5초 period로 등록
        // 움직임이 아니면 5초 증가, max 30초로 제한
        if(moving)
        {
            Log.d(LOGTAG, "MOVING!!");
            period = periodForMoving; // 5초로 고정
        }
        else
        {
            Log.d(LOGTAG, "NOT MOVING!!");
            period = period + periodIncrement; // 5초씩 증가
            if(period >= periodMax)
            {
                period = periodMax;
            }
        }
        Log.d(LOGTAG, "Next alarm: " + period);

        // 다음 alarm 등록
        Intent in = new Intent("kr.ac.koreatech.msp.adcalarm");
        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, in, 0);
        // 정확하게 부팅 된 이후 시간으로 다음 알람 설정, 주기가 5초면, 활동하는 시간 1초 + 쉬는 시간 4초 이렇게 구성되는 거임
        am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + period - activeTime, pendingIntent);
    }

    private void sendDataToActivity(boolean moving)
    {
        // 화면에 정보 표시를 위해 activity의 broadcast receiver가 받을 수 있도록 broadcast 전송
        Intent intent = new Intent("kr.ac.koreatech.msp.adcstepmonitor.moving");
        intent.putExtra("moving", moving);
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
        IntentFilter intentFilter = new IntentFilter("kr.ac.koreatech.msp.adcalarm");
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
        Intent in = new Intent("kr.ac.koreatech.msp.adcalarm");
        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, in, 0);

        // Alarm이 발생할 시간 및 alarm 발생시 이용할 pending intent 설정
        // 설정한 시간 (period=10000 (10초)) 후 alarm 발생
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