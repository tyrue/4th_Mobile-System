package kr.koreatech.cse.ex16_accelchart;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mLinear;

    GraphView graph;
    GraphView graphRMS;

    private LineGraphSeries<DataPoint> seriesX;
    private LineGraphSeries<DataPoint> seriesY;
    private LineGraphSeries<DataPoint> seriesZ;
    private LineGraphSeries<DataPoint> seriesRMS;

    private TextView mX;
    private TextView mY;
    private TextView mZ;
    private TextView rmsTV;

    private long count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        count = 0;

        mX = (TextView)findViewById(R.id.accelX);
        mY = (TextView)findViewById(R.id.accelY);
        mZ = (TextView)findViewById(R.id.accelZ);
        rmsTV = (TextView)findViewById(R.id.rms);

        //********** x, y, z 축 가속도를 표시하기 위한 GraphView 설정 **********//
        // UI 레이아웃에 정의된 GraphView 객체 참조 변수 생성
        graph = (GraphView)findViewById(R.id.graph);

        // line graph를 그리는데 사용되는 데이터를 저장하는 객체 생성
        seriesX = new LineGraphSeries<DataPoint>();
        seriesY = new LineGraphSeries<DataPoint>();
        seriesZ = new LineGraphSeries<DataPoint>();

        // graph 객체에 이 graph를 그리기 위해서 사용될 series 데이터를 추가
        graph.addSeries(seriesX);
        graph.addSeries(seriesY);
        graph.addSeries(seriesZ);

        // graph 상에서 화면에 보여지는 부분에 대한 설정
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-15.0);
        graph.getViewport().setMaxY(15.0);

        // horizontal zooming과 scrolling이 가능하도록 설정
        graph.getViewport().setScalable(true);

        // legend 설정
        seriesX.setTitle("accel X");
        seriesY.setTitle("accel Y");
        seriesZ.setTitle("accel Z");
        seriesX.setColor(Color.RED);
        seriesY.setColor(Color.BLUE);
        seriesZ.setColor(Color.GREEN);

        // graph에 legend를 표시하기 위한 과정
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        //****************************************************************************//

        //********** x, y, z 축 가속도 값의 RMS 값을 표시하기 위한 GraphView 설정 **********//
        // UI 레이아웃에 정의된 GraphView 객체 참조 변수 생성
        graphRMS = (GraphView)findViewById(R.id.graphLinear);


        // line graph를 그리는데 사용되는 데이터를 저장하는 객체 생성
        seriesRMS = new LineGraphSeries<DataPoint>();
        // graph 객체에 이 graph를 그리기 위해서 사용될 series 데이터를 추가
        graphRMS.addSeries(seriesRMS);

        // graph 상에서 화면에 보여지는 부분에 대한 설정
        graphRMS.getViewport().setXAxisBoundsManual(true);
        graphRMS.getViewport().setYAxisBoundsManual(true);
        graphRMS.getViewport().setMinY(0.0);
        graphRMS.getViewport().setMaxY(30.0);

        // horizontal zooming과 scrolling이 가능하도록 설정
        graphRMS.getViewport().setScalable(true);

        // legend 설정
        seriesRMS.setTitle("accel rms");
        seriesRMS.setColor(Color.GREEN);

        graphRMS.getLegendRenderer().setVisible(true);
        graphRMS.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        //******************************************************************************//

        //***** SensorManager를 이용하여 사용할 Sensor 지정 및 SensorEventListener 등록 *****//
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mLinear = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorManager.registerListener(this, mLinear, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //mSensorManager.registerListener(this, mLinear, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            // event.values 배열의 사본을 만들어서 values 배열에 저장
            float[] values = event.values.clone();

            count++;

            // graph에 가속도 값을 그리기 위해 series 객체에 데이터 추가
            // DataPoint 객체 생성: DataPoint(x, y)
            seriesX.appendData(new DataPoint(count, values[0]), true, 200);
            seriesY.appendData(new DataPoint(count, values[1]), true, 200);
            seriesZ.appendData(new DataPoint(count, values[2]), true, 200);

            // UI 레이아웃 상의 TextView 객체에 값 표시
            mX.setText("accel X: " + (values[0]));
            mY.setText("accel Y: " + (values[1]));
            mZ.setText("accel Z: " + (values[2]));

            // accelerometer x, y, z 축 값의 Root Mean Square 값 계산
            double rms = Math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2]);

            // graph에 값을 그리기 위해 series 객체에 계산된 rms 데이터 추가
            seriesRMS.appendData(new DataPoint(count, rms), true, 100);

            // UI 레이아웃 상의 TextView 객체에 값 표시
            rmsTV.setText("rms: " + rms);
        }
    }
}