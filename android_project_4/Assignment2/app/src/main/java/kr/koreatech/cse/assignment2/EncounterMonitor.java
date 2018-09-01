package kr.koreatech.cse.assignment2;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

// 15초 마다 보는데 2번이상 검색되면 인카운터 하도록 한다
// 블루투스는 스캔하고 피니쉬 되는데 약 13초 정도 걸린다.
public class EncounterMonitor extends Service
{
    private static final String TAG = "EncounterMonitor";

    BluetoothAdapter mBTAdapter; // 블루투스 어댑터
    ArrayList<String> btName;   // 디바이스 이름 리스트
    ArrayList<String> userName; // 사용자 이름 리스트
    int[] counter;              // 각 블루투스 장치의 검색 횟수

    Timer timer = new Timer();  // 블루투스 검색 실행 주기
    TimerTask timerTask = null; // 타이머에 맞춰서 일할 클래스

    Vibrator vib;       // 진동 변수

    long time;                  // 현재 시간을 저장할 변수
    // 밀리초 형식인 시간을 yyyy.mm.dd hh:mm 형식으로 포맷함
    SimpleDateFormat dayTime = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    @Override
    public void onCreate()
    {
        Log.d(TAG, "onCreate()");

        // 블루투스 어댑터, 진동 서비스 정의
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // 최대 등록될 수 있는 개수는 3개이고, 처음에 0으로 초기화함
        counter = new int[3];
        for(int i = 0; i < counter.length; i++)
            counter[i] = 0;

        // BT 디바이스 검색 관련하여 어떤 종류의 broadcast를 받을 것인지 IntentFilter로 설정
        // 검색 실행, 찾음, 검색 종료
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        // BroadcastReceiver 등록
        registerReceiver(mReceiver, filter);
    }

    // BT 검색과 관련한 broadcast를 받을 BroadcastReceiver 객체 정의
    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED))        // discovery 시작됨
                Toast.makeText(getApplicationContext(), "Bluetooth scan started..", Toast.LENGTH_SHORT).show();
            else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) // discovery 종료됨
                Toast.makeText(getApplicationContext(), "Bluetooth scan finished..", Toast.LENGTH_LONG).show();
            else if (action.equals(BluetoothDevice.ACTION_FOUND))    // Bluetooth device가 검색 됨
            {
                // 발견된 장치를 가져옴
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Intent in = new Intent(); // 브로트 캐스트 메시지를 보낼 인텐트
                for(int i = 0; i < btName.size(); i++) // 리스트 전체를 확인함
                {
                    // 검색된 디바이스 이름이 등록된 디바이스 이름과 같으면 진동과 Toast 메시지 표시
                    if (btName.get(i).equals(device.getName()))
                    {
                        counter[i]++; // 해당 디바이스의 검색 횟수 증가, 2번이상 만났을 경우 인카운터 판단
                        if(counter[i] >= 2)
                        {
                            vib.vibrate(200);
                            Toast.makeText(getApplicationContext(), "You encounter " + userName.get(i), Toast.LENGTH_LONG).show();

                            // 현재 시간을 문자열에 저장하고 이를 인텐트에 저장함
                            time = System.currentTimeMillis();
                            String d = dayTime.format(new Date(time));
                            in.putExtra("encounter", d + " " + userName.get(i) + "\n");
                        }
                    }
                }
                // 메인액티비티가 받을 수 있도록 브로드캐스트를 보낸다.
                in.setAction("kr.koreatech.cse.assignment2.encounter");
                sendBroadcast(in);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // intent: startService() 호출 시 넘기는 intent 객체
        // flags: service start 요청에 대한 부가 정보. 0, START_FLAG_REDELIVERY, START_FLAG_RETRY
        // startId: start 요청을 나타내는 unique integer id

        Toast.makeText(this, "EncounterMonitor 시작", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onStartCommand()");

        // MainActivity에서 Service를 시작할 때 사용한 intent에 담겨진 BT 디바이스와 사용자 이름 리스트 얻음
        btName = intent.getStringArrayListExtra("bt_name");
        userName = intent.getStringArrayListExtra("user_name");

        // 주기적으로 BT discovery 수행하기 위한 timer 가동
        startTimerTask();

        // 현재 시간을 문자열에 저장함
        time = System.currentTimeMillis();
        String str = dayTime.format(new Date(time));

        // 현재 시간을 포함한 시작을 알리는 문자열을 인텐트에 저장함
        // 그리고 메인액티비티가 받을 수 있도록 브로드캐스트를 보냄
        Intent in = new Intent();
        in.putExtra("encounter", "모니터링 시작 - " + str + "\n");
        in.setAction("kr.koreatech.cse.assignment2.encounter");
        sendBroadcast(in);

        return super.onStartCommand(intent, flags, startId);
    }

    // 서비스가 종료될 경우
    public void onDestroy()
    {
        Toast.makeText(this, "EncounterMonitor 중지", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onDestroy()");

        // 리스트를 비운다.
        btName.clear();
        userName.clear();

        // 현재시간을 문자열에 저장
        time = System.currentTimeMillis();
        String str = dayTime.format(new Date(time));

        // 현재 시간을 포함한 종료를 알리는 문자열을 인텐트에 저장함
        // 그리고 메인액티비티가 받을 수 있도록 브로드캐스트를 보냄
        Intent in = new Intent();
        in.putExtra("encounter", "모니터링 종료 - " + str + "\n");
        in.setAction("kr.koreatech.cse.assignment2.encounter");
        sendBroadcast(in);

        stopTimerTask(); // 타이머를 종료
        unregisterReceiver(mReceiver); // 리시버 종료
    }

    private void startTimerTask() // TimerTask 생성한다
    {
        timerTask = new TimerTask() // 블루투스 장치를 검색하는 일을 한다.
        {
            @Override
            public void run()
            {
                mBTAdapter.startDiscovery();
            }
        };

        // TimerTask를 Timer를 통해 실행시킨다
        // 1초 후에 타이머를 구동하고 15초마다 반복한다
        timer.schedule(timerTask, 1000, 15000);
    }

    private void stopTimerTask() // 모든 태스크를 중단한다
    {
        if (timerTask != null)
        {
            timerTask.cancel();
            timerTask = null;
        }
    }
}

