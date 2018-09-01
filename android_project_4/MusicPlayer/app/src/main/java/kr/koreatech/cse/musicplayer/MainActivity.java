package kr.koreatech.cse.musicplayer;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
{
    static final String TAG = "MobileProgramming";
    File musicDir; // 파일 객체

    private ListView m_ListView;                     // 리스트 뷰 객체
    private ArrayAdapter<String> m_Adapter;          // 어댑터
    ArrayList<String> values = new ArrayList<>();     // 음악 이름 리스트
    ArrayList<String> file_addr = new ArrayList<>();  // 음악 파일 주소 리스트

    // 권환 플래그
    public final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 런타임 권한 얻기
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) // 권한이 없는 상태면
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) // 사용자가 권한 요청을 거절했을 때
            {
            }
            else // 거절 하지 않았을 때
            {
                // 권한을 요청함
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }
        else // 권한이 있는 상태
        {
            prepareAccessToMusicDirectory(); // 파일을 읽는다.
        }
    }

    public void prepareAccessToMusicDirectory() // 공용 폴더의 파일을 읽는 함수
    {
        // Public Directory 중에 Music 디렉토리에 대한 File 객체를 반환한다
        musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File files[] = {}; // 파일 배열
        int num = 0;      // 파일 갯수
        try
        {
            // listFiles(): 디렉토리에 있는 파일(디렉토리 포함)들을 나타내는 File 객체들의 배열을 반환한다
            files = musicDir.listFiles();
            if(files == null) // 폴더가 없을 때
            {
                Log.i(TAG, "this is not be a directory or IOError was occurred");
            }
            else // music 폴더가 있을 때
            {
                num = files.length; // 파일의 개수 받음
                if (num == 0) // 음악 파일이 없을 때
                {
                    Log.i(TAG, "there is no files in the Music directory");
                    Toast.makeText(getApplicationContext(),"음악 파일이 없습니다.",Toast.LENGTH_SHORT);
                }
                else // 음악 파일이 있을 때
                {
                    m_Adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, values);
                    m_ListView = (ListView) findViewById(R.id.m_list);       // Xml에서 추가한 ListView의 객체
                    m_ListView.setAdapter(m_Adapter);                     // ListView에 어댑터 연결
                    m_ListView.setOnItemClickListener(onClickListItem);  // ListView 아이템 터치 시 이벤트를 처리할 리스너 설정

                    // File 객체 배열의 길이만큼 for 루프를 돌면서 파일의 이름과 주소를 저장한다.
                    for (int i = 0; i < num; i++)
                    {
                        Log.i(TAG, "music directory file " + (i + 1) + " : " + files[i].getName());
                        m_Adapter.add(files[i].getName());         // 파일 이름을 추가함
                        file_addr.add(files[i].getAbsolutePath()); // 파일의 절대 경로를 저장
                    }
                }
            }
        }
        catch(SecurityException e) { e.printStackTrace(); }
    }

    @Override
    // 요청 결과를 받는 함수
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
            {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // READ_EXTERNAL_STORAGE 권한을 얻었다
                    prepareAccessToMusicDirectory();
                }
                else
                {
                    // 권한을 얻지 못하였으므로 파일 읽기를 할 수 없다
                    Toast.makeText(getApplicationContext(),"권한을 얻지 못하여 파일을 불러올 수 없습니다.",Toast.LENGTH_SHORT);
                }
                return;
            }
        }
    }

    // 리스트 아이템 터치 이벤트 리스너 구현
    private AdapterView.OnItemClickListener onClickListItem = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            // 음악 재생 액티비티에 음악 파일 이름과 주소 리스트를 보내고 액티비티를 실행한다.
            Intent in = new Intent(MainActivity.this, PlayActivity.class);
            in.putStringArrayListExtra("file_addr",file_addr);
            in.putStringArrayListExtra("file_name",values);
            in.putExtra("pos",position); // 리스트 위치, 음악의 인덱스
            startActivity(in);
        }
    };
}
