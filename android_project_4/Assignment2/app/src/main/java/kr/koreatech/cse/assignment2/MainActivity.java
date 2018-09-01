package kr.koreatech.cse.assignment2;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
public class MainActivity extends AppCompatActivity
{
    boolean isPermitted = false; // 권한을 받았는지 확인
    final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION_AND_WRITE_EXTERNAL_STORAGE = 1; // 권한 요청 코드

    // 블루투스 관련
    BluetoothAdapter mBTAdapter;
    BluetoothManager mBTManager;

    static final int REQUEST_ENABLE_BT = 1;         // 블루투스 장치 코드
    static final int REQUEST_ENABLE_DISCOVER = 2;  // 블루투스 검색 코드

    TextView logText;       // 화면에 기록을 표시할 텍스트 뷰
    EditText bt;             // 블루투스 장치 이름 입력 받음
    EditText user;          // 사용자 이름 입력 받음
    IntentFilter filter;    // 브로드캐스트 필터
    boolean isfirst;       // 장치 검색을 처음 시작했는지 확인

    ArrayList<String> bt_name = new ArrayList<>();    // BT 디바이스 이름 리스트
    ArrayList<String> user_name = new ArrayList<>();  // 사용자 이름 리스트

    TextFileManager text_fm = new TextFileManager();  // 텍스트 파일 매니저

    // 브로드캐스트 리시버, 검색 목록을 받자마자 화면에 띄우기 위함
    BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // 아래와 같은 액션을 받았을 때, 파일에 기록을 저장하고, 화면에 기록을 표시한다.
            String action = intent.getAction();
            if (action.equals("kr.koreatech.cse.assignment2.encounter"))
            {
                String str = intent.getStringExtra("encounter");
                text_fm.save_append(str); // 파일 이어쓰기 모드
                logText.setText(text_fm.load()); // 화면에 기록
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestRuntimePermission();

        logText = findViewById(R.id.logText);                    // 인카운터 결과 화면을 출력함
        logText.setMovementMethod(new ScrollingMovementMethod()); // 텍스트뷰에 스크롤을 할 수 있도록 했다

        bt = findViewById(R.id.btName);
        user = findViewById(R.id.userName);

        // 리스트를 클리어 함
        bt_name.clear();
        user_name.clear();
        isfirst = true; // 처음 시작하는 것으로 함

        // 브로드캐스트 필터 설정
        filter = new IntentFilter();
        filter.addAction("kr.koreatech.cse.assignment2.encounter");
        registerReceiver(receiver, filter);

        // Bluetooth Adapter 얻기 ========================//
        mBTManager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        mBTAdapter = mBTManager.getAdapter(); // project 생성시 Minimum SDK 설정에서 API level 18 이상으로 선택해야함

        // BT adapter 확인 ===============================//
        if(mBTAdapter == null) // 장치가 블루투스를 지원하지 않는 경우 null 반환
        {
            // 블루투스 지원하지 않기 때문에 블루투스를 이용할 수 없음
            // alert 메세지를 표시하고 사용자 확인 후 종료하도록 함
            // AlertDialog.Builder 이용, set method에 대한 chaining call 가능
            new AlertDialog.Builder(this)
                    .setTitle("Not compatible")
                    .setMessage("Your device does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
        }
        else
        {
            // 블루투스 이용 가능 - 스캔하고, 연결하고 등 작업을 할 수 있음
            // 블루투스를 지원하지만 현재 비활성화 상태이면, 활성화 상태로 변경해야 함
            // 이는 사용자의 동의를 구하는 다이얼로그가 화면에 표시되어 사용자가 활성화 하게 됨
            if(!mBTAdapter.isEnabled()) // 비활성화 상태
            {
                // 사용자에게 요청 받아 활성화 함
                Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
            }
            else
            {
                // 활성화 상태
                // 스캔을 하거나 연결을 할 수 있음
            }
        }
    }

    // 브로드캐스트 리시버를 중지함
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(receiver);
    }

    // 브로드캐스트 리시버를 재개함
    protected void onResume()
    {
        super.onResume();
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent data)
    {
        // 요청 코드에 따라 처리할 루틴을 구분해줌
        switch(requestCode)
        {
            case REQUEST_ENABLE_BT:
                if(responseCode == RESULT_OK)
                {
                    // 사용자가 활성화 상태로 변경하는 것을 허용하였음
                }
                else if(responseCode == RESULT_CANCELED)
                {
                    // 사용자가 활성화 상태로 변경하는 것을 허용하지 않음
                    // 블루투스를 사용할 수 없으므로 애플리케이션 종료
                    finish();
                }
                break;

            case REQUEST_ENABLE_DISCOVER:
                if(responseCode == RESULT_CANCELED)
                {
                    // 사용자가 DISCOVERABLE 허용하지 않음 (다이얼로그 화면에서 거부를 선택한 경우)
                    Toast.makeText(this, "사용자가 discoverable을 허용하지 않았습니다.", Toast.LENGTH_SHORT).show();
                }
        }
    }

    // 버튼을 눌렀을 때의 이벤트 처리 함수
    public void onClick(View view)
    {
        if(view.getId() == R.id.regiBtn) // 디바이스와 사용자 이름 등록 버튼
        {
            // EditText에 입력된 디바이스와 사용자 이름을 String 변수에 담음
            String btName = bt.getText().toString();
            String userName = user.getText().toString();

            // 입력 칸이 비어있거나, 등록이 꽉 차면 등록을 하지 않음
            if(btName.isEmpty())
                Toast.makeText(this, "BT 디바이스 이름을 입력하세요", Toast.LENGTH_LONG).show();
            else if(userName.isEmpty())
                Toast.makeText(this, "사용자 이름을 입력하세요", Toast.LENGTH_LONG).show();
            else if(bt_name.size() >= 3 && user_name.size() >= 3)
                Toast.makeText(this, "더 이상 등록할 수 없습니다", Toast.LENGTH_LONG).show();
            // 등록가능함
            else
            {
                // 각 리스트에 문자열을 저장하고 입력칸을 빈칸으로 돌려놓음
                bt_name.add(btName);
                user_name.add(userName);
                bt.setText("");
                user.setText("");
                Toast.makeText(this, "BT 디바이스와 사용자 이름 등록!!", Toast.LENGTH_LONG).show();
            }
        }
        else if(view.getId() == R.id.checkBtn) // 등록된 디바이스와 사용자 쌍을 확인함
        {
            if(bt_name.size() > 0 && user_name.size() > 0)
            {
                String str = new String();
                for(int i = 0; i < bt_name.size(); i++) // 각 리스트 크기는 같으므로 디바이스 리스트 크기로 봐도 상관없다.
                    str += (bt_name.get(i) + " : " + user_name.get(i) + "\n");

                Toast.makeText(this, str, Toast.LENGTH_LONG).show();
            }
            else // 등록되어 있지 않으면 확인 불가
                Toast.makeText(this, "등록된 장치가 없습니다", Toast.LENGTH_LONG).show();
        }
        else if(view.getId() == R.id.startMonitorBtn) // 블루투스 모니터링 시작
        {
            if(bt_name.size() <= 0 || user_name.size() <= 0) // 등록이 되지 않으면 실행 불가
                Toast.makeText(this, "아직 등록을 하지 않으셨습니다.", Toast.LENGTH_LONG).show();
            else    // 등록이 되어있음
            {
                if(isfirst == true) // 처음 시작하는 경우 파일을 초기화하여 처음부터 쓰도록 함
                {
                    text_fm.save_new(" "); // 새로 쓰기 모드
                    isfirst = false;
                }

                // 서비스에 디바이스 이름과 사용자 이름 리스트를 보내고 서비스 시작.
                Intent intent = new Intent(this, EncounterMonitor.class);
                intent.putStringArrayListExtra("bt_name", bt_name);
                intent.putStringArrayListExtra("user_name", user_name);
                startService(intent);
            }
        }
        else if(view.getId() == R.id.stopMonitorBtn) // 서비스 중지 버튼, 서비스를 중지한다.
        {
            stopService(new Intent(this, EncounterMonitor.class));
        }
    }

    // "Enable Bluetooth Discoverable" 버튼을 눌렀을 때 실행되는 callback 메소드
    public void onClickDiscover(View view)
    {
        // BT discoverable을 요청하기 위한 Intent action은 정의되어 있음 이를 이용하여 intent 객체를 생성
        Intent discoverIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);

        // 검색 가능 시간 설정. 기본적으로는 120초 동안 검색 가능
        discoverIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);

        // 검색 가능하게 허용할 것인지 사용자에게 묻는 activity를 실행, 화면에 다이얼로그 같은 형태로 표시됨
        startActivityForResult(discoverIntent, REQUEST_ENABLE_DISCOVER);
    }

    // 위치와 파일 접근의 권한을 요청하는 메소드
    private void requestRuntimePermission()
    {
        // Runtime permission check
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)   // 두 권한을 받지 않았을 경우
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) // 사용자가 위치 권한요청을 거부한 경우
            {

            }
            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) // 사용자가 파일 권한요청을 거부한 경우
            {

            }
            else
            {
                // No explanation needed, we can request the permission.
                // 두 권한 받기를 요청함
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION_AND_WRITE_EXTERNAL_STORAGE);
            }
        }
        else
        {
            isPermitted = true; // 권한을 받은 상태
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION_AND_WRITE_EXTERNAL_STORAGE:
            {
                // 권한을 얻지 못 하였으므로 요청 작업을 수행할 수 없다
                isPermitted = grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                return;
            }
        }
    }
}

