package kr.koreatech.cse.ex12_publicfile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{
    EditText mEdit = null;
    TextFileManager mFileMgr;

    final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    boolean isPermitted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestRuntimePermission();

        mFileMgr = new TextFileManager();
        mEdit = (EditText)findViewById(R.id.edit);
    }

    public void onClick(View v)
    {
        if(!isPermitted)
        {
            Toast.makeText(getApplicationContext(),
                    "외부 파일 쓰기/읽기 권한이 없습니다..", Toast.LENGTH_LONG).show();
        }
        else
        {
            switch (v.getId())
            {
                case R.id.read:
                {
                    String data = mFileMgr.load();
                    mEdit.setText(data);

                    Toast.makeText(this, "불러오기 완료", Toast.LENGTH_SHORT).show();
                    break;
                }

                case R.id.write:
                {
                    String data = mEdit.getText().toString();
                    mFileMgr.save(data);
                    mEdit.setText("");

                    Toast.makeText(this, "저장 완료", Toast.LENGTH_SHORT).show();
                    break;
                }

                case R.id.delete:
                {
                    if(mFileMgr.delete())
                    {
                        Toast.makeText(this, "삭제 완료", Toast.LENGTH_SHORT).show();
                        mEdit.setText("");
                    }
                    else
                    {
                        Toast.makeText(this, "삭제할 파일이 존재하지 않음", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
            }
        }
    }

    private void requestRuntimePermission()
    {
        //*******************************************************************
        // Runtime permission check
        //*******************************************************************
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) // 파일 쓰기 읽기 권한 받기 // 런타임 체크할게 2개, 로케이션, 파일
                != PackageManager.PERMISSION_GRANTED) // 2개 이상 받을 때, &&해서 하나 더 권한 씀
        {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            }
            else
            {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE); // 배열이 2개인걸로 해서 2개 받음
            }
        }
        else
        {
            // WRITE_EXTERNAL_STORAGE 권한이 있는 것
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
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // permission was granted, yay! Do the
                    // read_external_storage-related task you need to do.

                    // WRITE_EXTERNAL_STORAGE 권한을 얻음
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