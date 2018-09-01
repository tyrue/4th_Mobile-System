package kr.koreatech.cse.assignment1;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements LocationListener
{
    TextView[] text;    // 등록된 경보의 이름을 나타내는 텍스트 뷰 집합
    TextView now_loc;   // 현재 위치를 나타냄
    Button add, remove; // 등록, 해제 버튼
    Intent in;           // 액티비티 전환을 위한 인텐트

    private final int  ADD = 1;         // 경보 등록 코드
    private final int REMOVE = 2;      // 경보 해제 코드
    private final int RESULT_OK = 3;   // 인텐트 결과 확인
    private final String FILENAME = "data.txt";

    LocationManager locManager;
    AlertReceiver receiver;
    ArrayList<PendingIntent> proximityIntent_l = new ArrayList<>(); // 각각 다른 위치의 경보를 전해주기 위해 팬딩인텐트를 리스트로 함

    boolean isPermitted = false;        // 위치 서비스가 허가 받았는지 확인함
    boolean isLocRequested = false;     // 위치데이터가 요청되었는지 확인
    boolean isAlertRegistered = false;  // 근접 경보가 등록되었는지 확인
    final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1; // 위치 요청 코드

    private class Location_data // 등록된 경보의 데이터 클래스
    {
        public String name;         // 등록된 경보의 이름
        public String latitude;    // 등록된 경보의 위도
        public String longitude;   // 등록된 경보의 경도
        public String range;        // 등록된 경보의 범위
    }

    ArrayList<Location_data> location_data_list = new ArrayList<>();    // 데이터 클래스를 리스트 구조로 저장한다.

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 텍스트 뷰를 초기화 한다.
        text = new TextView[3];
        text[0] = (TextView)findViewById(R.id.first_t);
        text[1] = (TextView)findViewById(R.id.second_t);
        text[2] = (TextView)findViewById(R.id.third_t);
        now_loc = (TextView)findViewById(R.id.now_t);

        // 버튼을 초기화 한다.
        add = (Button)findViewById(R.id.add_b_m);
        remove = (Button)findViewById(R.id.remove_b_m);

        // 리스트 초기화
        location_data_list.clear();
        proximityIntent_l.clear();

        load_list_from_file(); // 파일로부터 데이터를 읽어서 데이터 리스트에 저장
        list_init(); // UI에 보여지는 등록된 경보 리스트를 새로 고침함

        // 로케이션 매니저 초기화
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    public void onResume() // 액티비티가 다시 시작할 때 마다 실행
    {
        super.onResume();
        if(proximityIntent_l.size() != 0)
        {
            for(int i = 0; i < proximityIntent_l.size(); i++)
                locManager.removeProximityAlert(proximityIntent_l.get(i)); // 등록된 경보를 해제함
            proximityIntent_l.clear(); // 모든 리스트 삭제
        }
        requestRuntimePermission(); // 위치서비스 허가 받음

        // 근접 경보를 받을 브로드캐스트 리시버 객체 생성 및 등록
        // 액션이 kr.ac.koreatech.msp.locationAlert인 브로드캐스트 메시지를 받도록 설정
        receiver = new AlertReceiver();
        IntentFilter filter = new IntentFilter("kr.ac.koreatech.msp.locationAlert");
        registerReceiver(receiver, filter);

        try
        {
            for(int i = 0; i < location_data_list.size(); i++)
            {
                // ProximityAlert 등록을 위한 PendingIntent 객체 얻기
                Intent intent = new Intent("kr.ac.koreatech.msp.locationAlert");
                intent.putExtra("loc_name", location_data_list.get(i).name); // 접근 경보의 이름을 전달함
                PendingIntent proximityIntent = PendingIntent.getBroadcast(this, i, intent, 0); // PendingIntent를 설정한다.
                proximityIntent_l.add(proximityIntent); // PendingIntent리스트에 PendingIntent을 추가함

                // 데이터 리스트에 있는 데이터를 순서대로 추출
                double lat = Double.parseDouble(location_data_list.get(i).latitude); // 위도
                double lon = Double.parseDouble(location_data_list.get(i).longitude); // 경도
                int ra = Integer.parseInt(location_data_list.get(i).range);         // 범위
                locManager.addProximityAlert(lat, lon, ra, -1, proximityIntent); // 접근 경보을 등록하고 방송한다.
            }
            isAlertRegistered = true; // 접근 경보 등록된 상태
        }
        catch (SecurityException e) { // 예외 처리
            e.printStackTrace();
        }
    }

    public void onClick(View view) // 버튼을 눌렀을 때의 이벤트 처리
    {
        if(view.getId() == R.id.add_b_m) // 추가 버튼일 때
        {
            if(location_data_list.size() >= 3) // 현재 등록된 경보 리스트가 3개 이상이라면 더 등록 못하게 함
                Toast.makeText(this, "등록할 공간이 없습니다.", Toast.LENGTH_LONG).show();
            else
            {
                in = new Intent(this, AddActivity.class);
                startActivityForResult(in, ADD); // 요청 코드를 ADD로 하고 AddActivity를 연다.
            }
        }
        else if(view.getId() == R.id.remove_b_m)
        {
            if(location_data_list.size() == 0) // 현재 등록된 경보가 없을 때 해제 할 수 없도록 한다.
                Toast.makeText(this, "등록된 경보가 없습니다.", Toast.LENGTH_LONG).show();
            else
            {
                in = new Intent(this, RemoveActivity.class);
                startActivityForResult(in, REMOVE); // 요청 코드를 REMOVE로 하고 RemoveActivity를 연다.
            }
        }
    }

    // 외부 액티비티로 부터 전달 값을 받아 처리하는 메소드
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == ADD && resultCode == RESULT_OK) // 경보 등록을 요청한 결과일 때
        {
            if(location_data_list.size() < 3) // 현재 등록된 경보 리스트가 3개 미만이어야 함
            {
                String name_a = data.getStringExtra("name_a").toString(); // 등록할 경보의 이름을 받음
                boolean same = false; // 리스트에 이미 같은 이름으로 등록되어 있는지 확인

                for(int i = 0; i < location_data_list.size(); i++) // 전체 리스트를 확인하여 이미 존재하는 이름이라면 등록하지 않음
                {
                    if(name_a.equals(location_data_list.get(i).name))
                    {
                        Toast.makeText(this, "이미 같은 이름으로 등록되어 있습니다.", Toast.LENGTH_LONG).show();
                        same = true;
                        break;
                    }
                }

                if(same == false) // 리스트에 없는 이름이라면
                {
                    Location_data d = new Location_data(); // 새로운 경보 위치 데이터 객체 생성

                    // 입력받은 경보의 데이터 값을 저장한다.
                    d.name = name_a; // 이름
                    d.latitude = data.getStringExtra("latitude_a"); // 위도
                    d.longitude = data.getStringExtra("longitude_a"); // 경도
                    d.range = data.getStringExtra("range_a"); // 범위

                    location_data_list.add(d); // 데이터 리스트에 클래스 객체 저장

                    list_init(); // UI 리스트 초기화
                    Toast.makeText(this, "등록 되었습니다.", Toast.LENGTH_LONG).show();
                }
            }
            else // 현재 등록된 경보가 3개 이상이면 등록 못함
                Toast.makeText(this, "등록할 공간이 없습니다.", Toast.LENGTH_LONG).show();
        }
        else if(requestCode == REMOVE && resultCode == RESULT_OK) // 등록 해제를 요청한 결과
        {
            String s_r = data.getStringExtra("name_r"); // 해제할 접근 경보의 이름
            int l_size = location_data_list.size(); // 현재 등록된 경보 리스트의 크기

            for(int i = 0; i < location_data_list.size(); i++) // 전체 리스트를 확인
            {
                if(s_r.equals(location_data_list.get(i).name))
                    location_data_list.remove(i); // 해당 이름을 가진 객체를 삭제
            }

            if(l_size == location_data_list.size()) // 만약 크기가 같다면, 리스트에서 삭제를 하지 않았으므로 해당하는 이름이 없다고 판단
                Toast.makeText(this, "해당하는 이름이 없습니다.", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(this, "해제 되었습니다.", Toast.LENGTH_LONG).show();
            list_init(); // UI리스트 초기화
        }
    }

    public void list_init() // 등록된 데이터에 따라 보여지는 리스트를 다시 초기화함
    {
        for(int i = 0; i < 3; i++)
            text[i].setText((i + 1) + ". " ); // "1. ", "2. ", "3. "로 초기화
        for(int i = 0; i < location_data_list.size(); i++) // 각 등록된 접근 경보 이름을 뒤에 덧붙인다
            text[i].setText((i + 1) + ". " + location_data_list.get(i).name);
    }

    protected void onStop() // 프로그램을 종료할 때, 데이터를 파일에 저장한다.
    {
        super.onStop();
        save_list_to_file(); // 접근 경보 데이터 리스트를 파일에 저장
    }

    private void save_list_to_file() // 리스트를 파일에 저장하는 메소드
    {
        File file = new File(getFilesDir(), FILENAME); // 데이터를 저장할 객체 생성
        FileWriter fw = null;        // 파일쓰기 객체
        BufferedWriter bufwr = null; // 파일 쓰기용 버퍼
        try
        {
            // 파일 쓰기 초기화
            fw = new FileWriter(file);
            bufwr = new BufferedWriter(fw);

            for (int i = 0; i < location_data_list.size(); i++)
            {
                // 각 리스트에 있는 데이터 객체의 변수들을 ","로 구분하여 파일 한 줄에 쓴다.
                bufwr.write(location_data_list.get(i).name + "," + location_data_list.get(i).latitude + "," +
                        location_data_list.get(i).longitude + "," + location_data_list.get(i).range);
                bufwr.newLine(); // 파일 다음 줄로 넘어감
            }
            bufwr.flush(); // 버퍼 초기화
            Toast.makeText(this, "파일에 저장 되었습니다.", Toast.LENGTH_LONG).show();
        }
        catch (Exception e) {
            e.printStackTrace() ;
        }

        try
        {
            if (bufwr != null) // 파일 쓰기용 버퍼 닫기
                bufwr.close();

            if (fw != null) // 파일 쓰기 객체 닫기
                fw.close();
        }
        catch (Exception e) {
            e.printStackTrace() ;
        }
    }

    private void load_list_from_file() // 파일로부터 데이터 리스트에 저장하는 메소드
    {
        File file = new File(getFilesDir(), FILENAME); // 파일 객체 생성
        FileReader fr = null;           // 파일읽기 객체
        BufferedReader bufrd = null;    // 파일 읽기용 버퍼

        if (file.exists()) // 파일이 존재한다면
        {
            try
            {
                // 파일 읽기 객체 초기화
                fr = new FileReader(file);
                bufrd = new BufferedReader(fr);

                String str; // 파일을 한 줄씩 읽어 문자열로 씀
                while ((str = bufrd.readLine()) != null) // 더이상 읽을 줄이 없을 때 까지 반복
                {
                    String[] data = str.split(","); // ,로 구분하여 데이터를 분리함
                    Location_data d = new Location_data(); // 접근 경보 데이터 객체 초기화

                    // 순서대로 데이터 객체에 저장한다.
                    d.name = data[0];
                    d.latitude = data[1];
                    d.longitude = data[2];
                    d.range = data[3];

                    location_data_list.add(d); // 객체를 접근 경보 리스트에 추가한다.
                }

                bufrd.close(); // 버퍼 닫음
                fr.close();    // 파일 닫음
                Toast.makeText(this, "파일로부터 데이터를 읽습니다.", Toast.LENGTH_LONG).show();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void requestRuntimePermission() // 실행시간 권한 받는 메소드
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) // 아직 ACCESS_FINE_LOCATION권한을 받지 않았다면
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION))
            {
            }
            else // 권한을 받음
            {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        }
        else // 이미 ACCESS_FINE_LOCATION 권한이 있다면
        {
            isPermitted = true; // 권한이 있음
            // 현재 위치를 GPS를 통해 읽어온다.
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, this);
            isLocRequested = true; // 현재 위치 요청됨
        }
    }

    // 권한 요청 결과를 처리하는 메소드
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
            {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // ACCESS_FINE_LOCATION 권한을 얻음
                    isPermitted = true;
                }
                else
                {
                    // 권한을 얻지 못 하였으므로 location 요청 작업을 수행할 수 없다
                    // 적절히 대처한다
                    isPermitted = false;
                }
                return;
            }
        }
    }

    @Override
    protected void onPause() // 액티비티(프로그램)이 멈출 때
    {
        super.onPause();
        // 자원 사용 해제
        try
        {
            if(isLocRequested) // 현재 위치를 요청받고 있는 상태면
            {
                locManager.removeUpdates(this); // 현재 위치 받기 해제
                isLocRequested = false;
            }
            if(isAlertRegistered) // 접근 경보가 등록된 상태라면
            {
                if(proximityIntent_l.size() != 0) // 현재 PendingIntent리스트에 있는 모든 PendingIntent에 대한 접근 경보를 해제한다.
                {
                    for(int i = 0; i < proximityIntent_l.size(); i++)
                        locManager.removeProximityAlert(proximityIntent_l.get(i));
                    proximityIntent_l.clear(); // 리스트 초기화
                    unregisterReceiver(receiver); // 브로드캐스트 해제
                }
            }
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) // 현재 위치가 변하면
    {
        // 현재 위치 정보를 UI에 표시한다.
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        now_loc.setText("현재 위치 \n" + "위도 : " + lat + ", 경도 : " + lng +
                "\n등록된 근접경보 : " + proximityIntent_l.size());
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

}
