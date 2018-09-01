package kr.koreatech.cse.ex10_bluetooth;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{
    boolean isPermitted = false;
    final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;

    BluetoothAdapter mBTAdapter;
    BluetoothManager mBTManager;
    static final int REQUEST_ENABLE_BT = 1;
    static final int REQUEST_ENABLE_DISCOVER = 2;
    TextView list;

    // BT 검색과 관련한 broadcast를 받을 BroadcastReceiver 객체 정의
    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
            {
                // discovery 시작됨
                // 검색 진행 중이라는 dialog를 표시한다든지 다른 작업 수행 가능

                // 아래는 toast 메시지 표시하는 코드
                Toast.makeText(getApplicationContext(), "Bluetooth scan started..", Toast.LENGTH_LONG).show();
            }
            else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
            {
                // discovery 종료됨
                // 화면에 dialog를 표시했다면, dialog 종료(dismiss)

                // 아래는 toast 메시지 표시하는 코드
                Toast.makeText(getApplicationContext(), "Bluetooth scan finished..", Toast.LENGTH_LONG).show();
            }
            else if (action.equals(BluetoothDevice.ACTION_FOUND))
            {
                // Bluetooth device가 검색 됨
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                // device 이름과 mac 주소, rssi 값을 TextView에 표시
                list.append("\nBT device name: " + device.getName() + "  address: " + device.getAddress() +
                        " RSSI: " + rssi + "\n");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestRuntimePermission();

        list = (TextView) findViewById(R.id.btList);
        list.setText("");

        // Bluetooth Adapter 참조 객체 얻는 두 가지 방법 ========================//
        // 1. BluetoothManager 통해서
        mBTManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBTAdapter = mBTManager.getAdapter(); // project 생성시 Minimum SDK 설정에서 API level 18 이상으로 선택해야
        // 2. BluetoothAdapter 클래스의 static method, getDefaultAdapter() 통해서
        //mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        // BT adapter 확인 ===============================//
        // 장치가 블루투스를 지원하지 않는 경우 null 반환
        if (mBTAdapter == null)
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
            // 블루투스 이용 가능
            // 스캔하고, 연결하고 등 작업을 할 수 있음

            // 필요한 경우, 블루트스 활성화 ========================================//
            // 블루투스를 지원하지만 현재 비활성화 상태이면, 활성화 상태로 변경해야 함
            // 이는 사용자의 동의를 구하는 다이얼로그와 비슷한 activity 화면이 표시되어 사용자가 활성화 하게 됨
            if (!mBTAdapter.isEnabled())
            {
                // 비활성화 상태
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

    // "Bluetooth Scan" 버튼을 눌렀을 때 실행되는 메소드
    // 레이아웃 파일 activity_main.xml 파일에서 해당 버튼의 onClick 속성 값으로 지정되어 있는 상태
    public void onClick(View view)
    {
        if (isPermitted == false)
            Toast.makeText(getApplicationContext(),
                    "BT 검색 결과 제공을 위한 Location access 권한이 없습니다..", Toast.LENGTH_LONG).show();
        else
        {
            // 스캔 버튼을 누르면, 검색 시작
            // 안드로이드는 블루투스 스캔 결과를 Broadcast 형태로 전달한다
            // 따라서 이 Broadcast를 받을 BroadcastReceiver를 등록해야 한다

            // 어떤 종류의 broadcast를 받을 것인지 IntentFilter로 설정
            // BT 스캔 시 안드로이드 시스템은 3가지의 broadcast를 전송하게 됨
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            // BroadcastReceiver 등록
            registerReceiver(mReceiver, filter);

            // BT device 검색 시작
            mBTAdapter.startDiscovery();

            list.setText("");
        }
    }

    // "Enable Bluetooth Discoverable" 버튼을 눌렀을 때 실행되는 callback 메소드
    // 레이아웃 파일 activity_main.xml 파일에서 해당 버튼의 onClick 속성 값으로 지정되어 있는 상태
    public void onClickDiscover(View view)
    {
        // BT discoverable을 요청하기 위한 Intent action은 정의되어 있음
        // 이를 이용하여 intent 객체를 생성
        Intent discoverIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);

        // 검색 가능 시간 설정. 기본적으로는 120초 동안 검색 가능
        // 앱이 설정할 수 있는 최대 시간 3600초, 값이 0인 경우 항상 검색 가능
        // 0 미만, 3600 초과 값은 120초로 자동 설정
        discoverIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);

        // 검색 가능하게 허용할 것인지 사용자에게 묻는 activity를 실행, 화면에 다이얼로그 같은 형태로 표시됨
        startActivityForResult(discoverIntent, REQUEST_ENABLE_DISCOVER);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        unregisterReceiver(mReceiver);
    }

    protected void onActivityResult(int requestCode, int responseCode, Intent data)
    {
        // 요청 코드에 따라 처리할 루틴을 구분해줌
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (responseCode == RESULT_OK) {
                    // 사용자가 활성화 상태로 변경하는 것을 허용하였음
                } else if (responseCode == RESULT_CANCELED) {
                    // 사용자가 활성화 상태로 변경하는 것을 허용하지 않음
                    // 블루투스를 사용할 수 없으므로 애플리케이션 종료
                    finish();
                }
                break;
            case REQUEST_ENABLE_DISCOVER:
                if (responseCode == RESULT_CANCELED) {
                    // 사용자가 DISCOVERABLE 허용하지 않음 (다이얼로그 화면에서 거부를 선택한 경우)
                    Toast.makeText(this, "사용자가 discoverable을 허용하지 않았습니다.", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void requestRuntimePermission() {
        //*******************************************************************
        // Runtime permission check
        //*******************************************************************
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION))
            {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            }
            else
            {
                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            }
        }
        else
            isPermitted = true; // ACCESS_FINE_LOCATION 권한이 있는 것
        //*********************************************************************
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // read_external_storage-related task you need to do.

                    // ACCESS_FINE_LOCATION 권한을 얻음
                    isPermitted = true;

                } else {

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
