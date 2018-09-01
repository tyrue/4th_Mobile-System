package kr.koreatech.cse.ex17_adcstepmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
{
    private TextView mAccelX;
    private TextView mAccelY;
    private TextView mAccelZ;
    private TextView rmsText;
    private TextView movingText;
    private double rms;

    private BroadcastReceiver MyStepReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(intent.getAction().equals("kr.ac.koreatech.msp.adcstepmonitor.rms"))
            {
                rms = intent.getDoubleExtra("rms", 0.0);
                rmsText.setText("rms: " + rms);
            }
            else if(intent.getAction().equals("kr.ac.koreatech.msp.adcstepmonitor.moving"))
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
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //mAccelX = (TextView)findViewById(R.id.accelX);
        //mAccelY = (TextView)findViewById(R.id.accelY);
        //mAccelZ = (TextView)findViewById(R.id.accelZ);

        rmsText = (TextView)findViewById(R.id.rms);
        movingText = (TextView)findViewById(R.id.moving);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("kr.ac.koreatech.msp.adcstepmonitor.rms");
        intentFilter.addAction("kr.ac.koreatech.msp.adcstepmonitor.moving");
        registerReceiver(MyStepReceiver, intentFilter);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(MyStepReceiver);
    }

    // Start/Stop 버튼을 눌렀을 때 호출되는 콜백 메소드
    // Activity monitoring을 수행하는 service 시작/종료
    public void onClick(View v)
    {
        if(v.getId() == R.id.startMonitor)
        {
            Intent intent = new Intent(this, ADCMonitorService.class);
            startService(intent);
        }
        else if(v.getId() == R.id.stopMonitor)
        {
            stopService(new Intent(this, ADCMonitorService.class));
        }
    }
}
