package kr.koreatech.cse.ex13_powertimer;

import android.content.Context;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
{

    TextView m1000msCountTv = null;

    Timer mTimer = new Timer();
    TimerTask m1000msCountTimerTask = null;

    //***********************************
    // wake lock을 사용하는 경우 필요한 코드
    PowerManager pm;
    PowerManager.WakeLock wl;
    //***********************************

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m1000msCountTv = (TextView) findViewById(R.id.ms_1000_countdown_text);

        //**************************************************************
        // wake lock을 사용하는 경우 필요한 코드
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Tag: partial wake lock");
        wl.acquire();
        //**************************************************************
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        // 타이머는 작업 스레드이기 때문에 액티비티가 종료될 때
        // 반드시 중단하여 스레드를 제거시키도록 한다
        mTimer.cancel();

        //***********************************
        // wake lock을 사용하는 경우 필요한 코드
        wl.release();
        //***********************************
    }

    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.start:
            {
                startTimerTask();
                break;
            }
            case R.id.stop:
            {
                stopTimerTask();
                break;
            }
        }
    }

    private void startTimerTask()
    {
        // 1. TimerTask 실행 중이라면 중단한다
        stopTimerTask();

        // 2. TimerTask 객체를 생성한다
        // 1000 밀리초마다 카운팅 되는 태스크를 등록한다
        m1000msCountTimerTask = new TimerTask()
        {
            int mCount = 0;

            @Override
            public void run()
            {
                mCount++;
                m1000msCountTv.post(new Runnable()
                {
                    @Override
                    // 작업 스레드에서 작동하도록 함 왜냐하면 메인스레드에서 하도록 하면 메인이 너무 일을 많이 해서 작동 안 할 수도
                    public void run() {
                        m1000msCountTv.setText("Count: " + mCount);
                    }
                });
            }
        };

        // 3. TimerTask를 Timer를 통해 실행시킨다
        // 1초 후에 타이머를 구동하고 1000 밀리초 단위로 반복한다
        // 	schedule(TimerTask task, long delay, long period)
        mTimer.schedule(m1000msCountTimerTask, 1000, 1000);
        //*** Timer 클래스 메소드 이용법 참고 ***//
        // http://developer.android.com/intl/ko/reference/java/util/Timer.html
        //***********************************//
    }

    private void stopTimerTask()
    {
        // 1. 모든 태스크를 중단한다
        if (m1000msCountTimerTask != null)
        {
            m1000msCountTimerTask.cancel();
            m1000msCountTimerTask = null;
        }

        // 2. 카운팅 초기화값을 텍스트뷰에 표시
        m1000msCountTv.setText("Count: 0");
    }
}