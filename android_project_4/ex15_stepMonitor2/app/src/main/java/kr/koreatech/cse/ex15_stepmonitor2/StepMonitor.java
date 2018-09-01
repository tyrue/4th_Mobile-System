package kr.koreatech.cse.ex15_stepmonitor2;

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

public class StepMonitor extends Service implements SensorEventListener
{
    private static final String LOGTAG = "StepMonitor";

    private SensorManager mSensorManager;
    private Sensor mLinear;
    private long prevT, currT;
    private double[] rmsArray;
    private int rmsCount;
    private double steps;

    // SENSOR_DELAY_NORMAL로 가속도 데이터 수집 시 데이터 업데이트 주기는 약 196ms (Google Pixel2 기준)
    // 따라서 초당 데이터 샘플 수는 5.1개
    // 이 값은 폰마다 다를 수 있으므로 확인 필요
    private static final int NUMBER_OF_SAMPLES = 5;

    // 3축 가속도 데이터의 RMS 값의 1초간 평균값을 이용하여 걸음이 있었는지 판단하기 위한 기준 문턱값
    private static final double AVG_RMS_THRESHOLD = 2.5;

    // 1분당 걸음수를 90으로 가정
    // 1초 간 걸음수를 계산하면 1.5가 나옴.
    // 1초간 rms 평균값이 기준 문턱값을 넘었을 때, steps를 1.5 씩 증가
    private static final double NUMBER_OF_STEPS_PER_SEC = 1.5;

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onCreate()
    {
        Log.d(LOGTAG, "onCreate()");

        prevT = currT = 0;
        rmsCount = 0;
        rmsArray = new double[NUMBER_OF_SAMPLES];

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLinear = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        // SensorEventListener 등록
        mSensorManager.registerListener(this, mLinear, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // intent: startService() 호출 시 넘기는 intent 객체
        // flags: service start 요청에 대한 부가 정보. 0, START_FLAG_REDELIVERY, START_FLAG_RETRY
        // startId: start 요청을 나타내는 unique integer id

        Toast.makeText(this, "Activity Monitor 시작", Toast.LENGTH_SHORT).show();
        Log.d(LOGTAG, "onStartCommand()");


        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy()
    {
        Toast.makeText(this, "Activity Monitor 중지", Toast.LENGTH_SHORT).show();
        Log.d(LOGTAG, "onDestroy()");

        // SensorEventListener 해제
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    // 센서 데이터가 업데이트 되면 호출
    public void onSensorChanged(SensorEvent event)
    {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
        {

            //*** 데이터 업데이트 주기 확인 용 코드 ***//
            // SENSOR_DELAY_NORMAL, SENSOR_DELAY_UI, SENSOR_DELAY_GAME, SENSOR_DELAY_FASTEST로
            // 변경해가면서 로그(logcat)를 확인해 볼 것
            currT = event.timestamp;
            double dt = (currT - prevT)/1000000;
            // logcat에 로그를 출력하려면 아래 code line의 주석을 해제
            Log.d(LOGTAG, "time difference=" + dt);
            prevT = currT;
            //***************************************

            //***** sensor data collection *****//
            // event.values 배열의 사본을 만들어서 values 배열에 저장
            float[] values = event.values.clone();

            // simple step calculation
            computeSteps(values);
        }
    }

    // a simple inference for step count
    private void computeSteps(float[] values)
    {
        double avgRms = 0;

        //***** feature extraction *****//
        // calculate feature data:
        // 여기서는 3축 가속도 데이터의 RMS 값의 1초 간의 평균값을 이용

        // 1. 현재 업데이트 된 accelerometer x, y, z 축 값의 Root Mean Square 값 계산
        double rms = Math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2]);

        // 2. 위에서 계산한 RMS 값을 rms 값을 저장해 놓는 배열에 넣음
        // 배열 크기는 1초에 발생하는 가속도 데이터 개수 (여기서는 5)
        if(rmsCount < NUMBER_OF_SAMPLES)
        {
            rmsArray[rmsCount] = rms;
            rmsCount++;
        }
        else if(rmsCount == NUMBER_OF_SAMPLES)
        {
            // 3. 1초간 rms 값이 모였으면 평균 rms 값을 계산
            double sum = 0;
            // 3-1. rms 값들의 합을 구함
            for(int i = 0; i < NUMBER_OF_SAMPLES; i++)
            {
                sum += rmsArray[i];
            }
            // 3-2. 평균 rms 계산
            avgRms = sum / NUMBER_OF_SAMPLES;
            Log.d(LOGTAG, "1sec avg rms: " + avgRms);

            // 4. rmsCount, rmsArray 초기화: 다시 1초간 rms sample을 모으기 위해
            rmsCount = 0;
            for(int i = 0; i < NUMBER_OF_SAMPLES; i++)
            {
                rmsArray[i] = 0;
            }

            // 5. 이번 업데이트로 계산된 rms를 배열 첫번째 원소로 저장하고 카운트 1증가
            rmsArray[0] = rms;
            rmsCount++;
        }

        //***** classification *****//
        // check if there is a step or not:
        // 1. 3축 가속도 데이터의 1초 평균 RMS 값이 기준 문턱값을 넘으면 step이 있었다고 판단함
        if(avgRms > AVG_RMS_THRESHOLD)
        {
            // 1-1. step 수는 1초 걸음 시 step 수가 일정하다고 가정하고, 그 값을 더해 줌
            steps += NUMBER_OF_STEPS_PER_SEC;
            Log.d(LOGTAG, "steps: " + steps);

            // if step counts increase, send steps data to MainActivity
            Intent intent = new Intent("kr.ac.koreatech.msp.stepmonitor");
            // 걸음수는 정수로 표시되는 것이 적합하므로 int로 형변환
            intent.putExtra("steps", (int)steps);
            // broadcast 전송
            sendBroadcast(intent);
        }
    }
}