package kr.koreatech.cse.ex_4;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener
{
    private SensorManager mSensorManager;
    private Sensor mOrientation;
    TextView mText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mText = (TextView)findViewById(R.id.text);
    }

    protected void onResume()
    {
        super.onResume();
        mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_UI);
    }

    protected void onPause()
    {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuacy)
    {

    }

    public void onSensorChanged(SensorEvent event)
    {
        if(event.sensor.getType() == Sensor.TYPE_ORIENTATION)
        {
            mText.setText("방향 센서 값\n\n 방위각 : " + event.values[0]
                    + "\n피치 : " + event.values[1] + "\n롤 : " + event.values[2]);
        }
    }
}
