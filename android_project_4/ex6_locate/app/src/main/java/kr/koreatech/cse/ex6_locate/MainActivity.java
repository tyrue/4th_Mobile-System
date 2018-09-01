package kr.koreatech.cse.ex6_locate;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements LocationListener
{
    final static String TAG = "MSP03";
    final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    TextView logView;
    TextView gps;
    TextView network;
    LocationManager lm;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logView = (TextView)findViewById(R.id.location);
        gps = (TextView)findViewById(R.id.gps);
        network = (TextView)findViewById(R.id.network);

        // LocationManager 참조 객체
        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        // GPS 프로바이더 사용 가능 여부
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gps.setText("GPS Provider: Available");
        }

        // 네트워크 프로바이더 사용 가능 여부
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            network.setText("Network Provider: Available");
        }
    }

    @Override
    protected void onResume() // 앱이 다시 켜질때 마다
    {
        super.onResume();

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
            // ACCESS_FINE_LOCATION 권한이 있는 것이므로
            // location updates 요청을 할 수 있다.

            // GPS provider를 이용
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
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

                    // ACCESS_FINE_LOCATION 권한을 얻었으므로
                    // 관련 작업을 수행할 수 있다
                    try {
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                    }
                    catch(SecurityException e) {
                        Log.d(TAG, "SecurityException: permission required");
                    }

                }
                else
                    {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    // 권한을 얻지 못 하였으므로 location 요청 작업을 수행할 수 없다
                    // 적절히 대처한다
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    protected void onPause() {
        super.onPause();

        lm.removeUpdates(this);
    }

    // LocationListener 구현을 위한 메소드
    // onLocationChanged, onStatusChanged, onProviderEnabled, onProviderDisabled

    public void onLocationChanged(Location location)
    {
        double lat = location.getLatitude();
        double lng = location.getLongitude();

        logView.setText("위도: "+ lat +", 경도: "+ lng);
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    public void onProviderEnabled(String provider) {

    }

    public void onProviderDisabled(String provider) {

    }
}
