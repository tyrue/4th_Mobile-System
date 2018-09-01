package kr.koreatech.cse.ex18_hslocationtracking;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
{
    private static final String LOGTAG = "HS_Location_Tracking";
    private static final String BROADCAST_ACTION_ACTIVITY = "kr.ac.koreatech.msp.hslocationtracking";
    private TextView mAccelX;
    private TextView mAccelY;
    private TextView mAccelZ;
    private TextView rmsText;
    private TextView movingText;
    private TextView locationText;

    boolean isPermitted = false;
    final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private BroadcastReceiver MyStepReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(intent.getAction().equals(BROADCAST_ACTION_ACTIVITY))
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
                double lon = intent.getDoubleExtra("longitude", 0.0);
                double lat = intent.getDoubleExtra("latitude", 0.0);
                locationText.setText("Location: longitude " + lon + " latitude " + lat);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestRuntimePermission();

        //mAccelX = (TextView)findViewById(R.id.accelX);
        //mAccelY = (TextView)findViewById(R.id.accelY);
        //mAccelZ = (TextView)findViewById(R.id.accelZ);

        movingText = (TextView)findViewById(R.id.moving);
        locationText = (TextView)findViewById(R.id.location);

        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION_ACTIVITY);
        registerReceiver(MyStepReceiver, intentFilter);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(MyStepReceiver);
    }

    // Start/Stop 버튼을 눌렀을 때 호출되는 콜백 메소드
    // monitoring을 수행하는 service 시작/종료
    public void onClick(View v)
    {
        if(v.getId() == R.id.startMonitor)
        {
            Intent intent = new Intent(this, HSMonitorService.class);
            startService(intent);
        }
        else if(v.getId() == R.id.stopMonitor)
        {
            stopService(new Intent(this, HSMonitorService.class));
        }
    }

    private void requestRuntimePermission()
    {
        //*******************************************************************
        // Runtime permission check
        //*******************************************************************
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION))
            {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            }
            else
            {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        }
        else
        {
            // ACCESS_FINE_LOCATION 권한이 있는 것
            isPermitted = true;
        }
        //*********************************************************************
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {

                    // permission was granted, yay! Do the
                    // read_external_storage-related task you need to do.

                    // ACCESS_FINE_LOCATION 권한을 얻음
                    isPermitted = true;

                }
                else
                {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    // 권한을 얻지 못 하였으므로 location 요청 작업을 수행할 수 없다
                    // 적절히 대처한다
                    isPermitted = false;

                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
