package kr.koreatech.cse.ex14_stepmonitor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class StepMonitor extends Service implements SensorEventListener {
    private static final String TAG = "StepMonitor";

    private SensorManager mSensorManger;
    private Sensor mAccel;
    private float previousY, currentY;
    private int threshold;
    private int steps;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        // step 여부를 판단하기 위한 y축 가속도 값의 차이의 문턱값
        threshold = 10;
        previousY = currentY = steps = 0;

        mSensorManger = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccel = mSensorManger.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // SensorEventListener 등록
        mSensorManger.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL); // 초당 몇십번
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // intent: startService() 호출 시 넘기는 intent 객체
        // flags: service start 요청에 대한 부가 정보. 0, START_FLAG_REDELIVERY, START_FLAG_RETRY
        // startId: start 요청을 나타내는 unique integer id

        Toast.makeText(this, "Step Monitor 시작", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onStartCommand()");


        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        Toast.makeText(this, "Step Monitor 중지", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onDestroy()");

        // SensorEventListener 해제
        mSensorManger.unregisterListener(this);

    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // 센서 데이터가 업데이트 되면 호출
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //***** sensor data collection *****//
            // 현재 y축 가속도 값
            currentY = event.values[1];

            // simple step calculation
            computeSteps(previousY, currentY);

            // 현재 y축 가속도 값을 이전 y축 가속도 값으로 기억해 둠
            previousY = event.values[1];
        }
    }

    // a simple inference for step count
    // 이전 y축 가속도 값과 현재 y축 가속도 값의 차가 threshold보다 크면 걸음 수 증가
    private void computeSteps(float prev, float curr) {
        //***** feature extraction *****//
        // calculate feature data:
        // 여기서는 이전 y축 가속도 값과 현재 y축 가속도 값의 차이를 이용
        float feature = Math.abs(curr - prev);

        //***** classification *****//
        // check if there is a step or not:
        // 여기서는 y축 가속도 값 차이가 일정 문턱값 이상이면 step으로 판단
        if(feature > threshold) {
            steps++;

            // if steps increased, send steps data to MainActivity
            Intent intent = new Intent("kr.ac.koreatech.msp.stepmonitor");
            intent.putExtra("steps", steps);
            sendBroadcast(intent);
        }
    }
}