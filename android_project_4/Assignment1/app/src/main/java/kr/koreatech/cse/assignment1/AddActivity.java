package kr.koreatech.cse.assignment1;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by DooHyun on 2018-03-27.
 */

public class AddActivity extends AppCompatActivity implements LocationListener
{
    TextView latitude_t, longitude_t; // 위도, 경도 텍스트 뷰
    EditText name_e, latitude_e, longitude_e, range_e; // 각 접근경보의 데이터를 입력 받을 EditText 뷰
    Button add_b;       // 등록 버튼
    int RESULT_OK = 3; // 요청 결과 코드

    LocationManager lm;
    final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        latitude_t = (TextView)findViewById(R.id.latitude_t);    // 현재위치의 위도
        longitude_t = (TextView)findViewById(R.id.longitude_t); // 현재위치의 경도
        name_e = (EditText)findViewById(R.id.name_a);             // 사용자가 입력한 위치 이름
        latitude_e = (EditText)findViewById(R.id.latitude_a);    // 사용자가 입력한 위도 - 현재 위치 기준으로 자동으로 입력
        longitude_e = (EditText)findViewById(R.id.longitude_a); // 사용자가 입력한 경도 - 현재 위치 기준으로 자동으로 입력
        range_e = (EditText)findViewById(R.id.range_a);          // 사용자가 입력한 범위
        add_b = (Button)findViewById(R.id.add_b_a);              // 추가 버튼

        // LocationManager 참조 객체
        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    }

    protected void onResume() // 앱이 다시 켜질때 마다
    {
        super.onResume();

        if (ContextCompat.checkSelfPermission(AddActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) // 하직 권한을 받지 못함
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(AddActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION))
            {
            }
            else
            {
                ActivityCompat.requestPermissions(AddActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION); // 권한을 받음
            }
        }
        else
        {
            // ACCESS_FINE_LOCATION 권한이 있는 것이므로
            // location updates 요청을 할 수 있다.
            // GPS provider를 이용
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
        }
    }

    public void onClick(View view) // add_b의 클릭 이벤트 처리
    {
        // 이름과 범위가 입력 받지 않았다면 등록 못하게 함
        if(name_e.getText().toString() == "" || name_e.getText().toString().isEmpty())
            Toast.makeText(this, "이름이 입력되지 않았습니다.", Toast.LENGTH_LONG).show();
        else if(range_e.getText().toString() == "" || range_e.getText().toString().isEmpty())
            Toast.makeText(this, "범위가 입력되지 않았습니다.", Toast.LENGTH_LONG).show();
        else // 데이터가 모두 입력 됨
        {
            Intent in = new Intent(this, MainActivity.class); // 메인액티비티로 돌아감

            // 각 입력받은 값을 전달함
            in.putExtra("name_a", name_e.getText().toString());
            in.putExtra("latitude_a", latitude_e.getText().toString());
            in.putExtra("longitude_a", longitude_e.getText().toString());
            in.putExtra("range_a", range_e.getText().toString());

            setResult(RESULT_OK, in); // 요청 결과 전달
            finish(); // add 액티비티 창을 닫는다.
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        lm.removeUpdates(this); // 위치 정보 요청 해제
    }

    public void onLocationChanged(Location location) // 현재 위치가 변할 때 마다 실행하는 메소드
    {
        double lat = location.getLatitude(); // 현재 위도
        double lng = location.getLongitude(); // 현재 경도

        // 현재의 위도와 경도로 텍스트 뷰와 EditText뷰를 설정
        latitude_t.setText(String.valueOf(lat));
        latitude_e.setText(String.valueOf(lat));
        longitude_t.setText(String.valueOf(lng));
        longitude_e.setText(String.valueOf(lng));
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    public void onProviderEnabled(String provider) {

    }

    public void onProviderDisabled(String provider) {

    }
}
