package kr.koreatech.cse.ex11_bluetooth_encounter;

/**
 * Created by DooHyun on 2018-04-10.
 */
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class EncounterMonitor extends Service
{
    private static final String TAG = "EncounterMonitor";

    BluetoothAdapter mBTAdapter;
    String btName;
    String userName;
    boolean isEncountering = false;

    Timer timer = new Timer();
    TimerTask timerTask = null;

    Vibrator vib;

    // BT 검색과 관련한 broadcast를 받을 BroadcastReceiver 객체 정의
    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
            {
                // discovery 시작됨
                // 아래는 toast 메시지 표시하는 코드
                Toast.makeText(getApplicationContext(), "Bluetooth scan started..", Toast.LENGTH_SHORT).show();
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
            {
                // discovery 종료됨
                // 아래는 toast 메시지 표시하는 코드
                Toast.makeText(getApplicationContext(), "Bluetooth scan finished..", Toast.LENGTH_LONG).show();
            } else if (action.equals(BluetoothDevice.ACTION_FOUND))
            {
                // Bluetooth device가 검색 됨
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                if (btName.equals(device.getName()))
                {
                    // 검색된 디바이스 이름이 등록된 디바이스 이름과 같으면
                    // 진동과 Toast 메시지 표시
                    vib.vibrate(200);
                    Toast.makeText(getApplicationContext(), "You encounter " + userName,
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onCreate()
    {
        Log.d(TAG, "onCreate()");

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // BT 디바이스 검색 관련하여 어떤 종류의 broadcast를 받을 것인지 IntentFilter로 설정
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);


        // BroadcastReceiver 등록
        registerReceiver(mReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // intent: startService() 호출 시 넘기는 intent 객체
        // flags: service start 요청에 대한 부가 정보. 0, START_FLAG_REDELIVERY, START_FLAG_RETRY
        // startId: start 요청을 나타내는 unique integer id

        Toast.makeText(this, "EncounterMonitor 시작", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onStartCommand()");

        // MainActivity에서 Service를 시작할 때 사용한 intent에 담겨진 BT 디바이스와 사용자 이름 얻음
        btName = intent.getStringExtra("BTName");
        userName = intent.getStringExtra("UserName");

        // 주기적으로 BT discovery 수행하기 위한 timer 가동
        startTimerTask();

        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy()
    {
        Toast.makeText(this, "EncounterMonitor 중지", Toast.LENGTH_SHORT).show();
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
                mBTAdapter.startDiscovery();
            }
        };

        // TimerTask를 Timer를 통해 실행시킨다
        // 1초 후에 타이머를 구동하고 30초마다 반복한다
        timer.schedule(timerTask, 1000, 30000);
        //*** Timer 클래스 메소드 이용법 참고 ***//
        // 	schedule(TimerTask task, long delay, long period)
        // http://developer.android.com/intl/ko/reference/java/util/Timer.html
        //***********************************//
    }

    private void stopTimerTask()
    {
        // 1. 모든 태스크를 중단한다
        if (timerTask != null)
        {
            timerTask.cancel();
            timerTask = null;
        }
    }
}

