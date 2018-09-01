package kr.koreatech.cse.ex_5_accelerometer;

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
    private Sensor mAccel, mLinear;
    TextView mText;
    String[] strings = new String[2];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mLinear = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mText = (TextView)findViewById(R.id.text);
        strings[0] = ""; strings[1] = "";
    }

    protected void onResume()
    {
        super.onResume();
        mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mLinear, SensorManager.SENSOR_DELAY_UI);
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
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            strings[0] = "Accel 값\n\n" + "x:" + event.values[0]
                    + "\ny : " + event.values[1] + "\nz : " + event.values[2];
        }
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
        {
            strings[1] = "Linear 값\n\n " + "x:" + event.values[0]
                    + "\ny : " + event.values[1] + "\nz : " + event.values[2];
        }
        mText.setText(strings[0] + "\n" + strings[1]);
    }
}
