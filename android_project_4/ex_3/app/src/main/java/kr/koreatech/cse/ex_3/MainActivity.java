package kr.koreatech.cse.ex_3;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String list = "";
        SensorManager sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ALL);

        list += "전체 센서 수 : " + sensors.size() + "\n";
        int i = 0;
        for(Sensor s: sensors)
        {
            list += "" + i++ + "name : " + s.getName() + "\n" + "power : " + s.getPower() + "\n"
                    + "resolution : " + s.getResolution() + "\n" + "range : " + s.getMaximumRange() + "\n"
                    + "vendor : " + s.getVendor() + "\n" + "min delay : " +s.getMinDelay() + "\n\n";
        }

        TextView text = (TextView)findViewById(R.id.text);
        text.setMovementMethod(new ScrollingMovementMethod());
        text.setText(list);
    }
}
