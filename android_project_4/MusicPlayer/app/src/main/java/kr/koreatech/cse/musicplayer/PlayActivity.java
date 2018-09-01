package kr.koreatech.cse.musicplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

public class PlayActivity extends AppCompatActivity
{
    static final String TAG = "MobileProgramming";

    MusicService mService;    // 바인딩 서비스 객체
    boolean mBound = false; // 바인딩 됐는지 확인

    ArrayList<String> file_addr; // 음악 주소 리스트
    ArrayList<String> file_name; // 음악 이름 리스트
    int pos;                      // 재생할 음악 인덱스

    Intent input, ouput; // 받을 인텐트, 보낼 인텐트
    TextView m_name;      // 텍스트 뷰
    ImageButton b_play, b_pause; // 재생, 일시정지 뷰

    // ServiceConnection 인터페이스를 구현한 ServiceConnection 객체 생성
    private ServiceConnection mConnection = new ServiceConnection()
    {
        // Service의 onBind() 메소드에서 반환한 IBinder 객체를 받음 (두번째 인자)
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            Log.d("PlayActivity", "onServiceConnected()");
            // 두번째 인자로 넘어온 IBinder 객체를 MusicService 클래스에 정의된 LocalBinder 클래스 객체로 캐스팅
            MusicService.LocalBinder binder = (MusicService.LocalBinder)service;
            // MusicService 객체를 참조하기 위해 LocalBinder 객체의 getService() 메소드 호출
            mService = binder.getService(); // 서비스 받음
            mBound = true; // 바인딩 됨
        }
        // Service 연결 해제되었을 때 호출되는 callback 메소드
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            Log.d("PlayActivity", "onServiceDisconnected()");
            mBound = false; // 바인딩 종료
        }
    };

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        input = getIntent(); // 서비스 및 메인 액티비티로부터 값 얻어옴
        file_addr = input.getStringArrayListExtra("file_addr"); // 음악 주소 리스트 얻어옴
        file_name = input.getStringArrayListExtra("file_name"); // 음악 이름 리스트 얻어옴
        pos = input.getIntExtra("pos",0); // 현재 음악 인덱스 얻어옴
        int isplay = input.getIntExtra("isplay",0); // 음악이 재생 중인지 확인

        ouput = new Intent(this, MusicService.class); // 서비스에게 값을 보낸다.
        ouput.putStringArrayListExtra("file_addr",file_addr); // 서비스에게 해당 음악 주소 리스트 보냄
        ouput.putStringArrayListExtra("file_name",file_name); // 서비스에게 해당 음악 이름 리스트 보냄

        b_play = (ImageButton)findViewById(R.id.play);    // 재생 버튼 뷰
        b_pause = (ImageButton)findViewById(R.id.pause); // 일시정지 버튼 뷰

        if(isplay == 1) // 재생되는 중이라면
        {
            b_play.setVisibility(View.GONE);      // 재생 버튼 안보이게 함
            b_pause.setVisibility(View.VISIBLE); // 일시정지 버튼 보이게 함
            bindService(ouput, mConnection, Context.BIND_ADJUST_WITH_ACTIVITY); // 바인딩 시작
        }
        else if(isplay == 2) // 일시정지 상태라면
        {
            b_pause.setVisibility(View.GONE); // 일시정지 버튼 안보이게 함
            bindService(ouput, mConnection, Context.BIND_ADJUST_WITH_ACTIVITY); // 바인딩 시작
        }
        else // 새로 액티비티 켠 상태라면
        {
            b_pause.setVisibility(View.GONE); // 일시정지 버튼 안보이게 함
        }
        m_name = (TextView)findViewById(R.id.name); // 음악파일 이름 뷰
        m_name.setText(file_name.get(pos));        // 텍스트뷰를 음악 이름으로 설정함

        // 텍스트뷰 배경 이미지를 반투명하게 한다.
        Drawable alpha = ((TextView)findViewById(R.id.name)).getBackground();
        alpha.setAlpha(50);
    }

    public void onClick(View view) // 버튼 클릭 함수 설정
    {
        switch (view.getId())
        {
            case R.id.play: // 음악 재생
                Log.d(TAG, "onClick() start");
                b_play.setVisibility(View.GONE); // 재생 버튼 안보이게 함
                b_pause.setVisibility(View.VISIBLE); // 일시정지 버튼 보이게 함

                if(mBound) // 바인딩 상태라면
                {
                    mService.music_play(); // 음악 재생
                }
                else // 바인딩 되지 않은 상태
                {
                    ouput.putExtra("pos",pos); // 현재 음악 인덱스 보냄
                    startService(ouput); // 서비스 시작
                    bindService(ouput, mConnection, Context.BIND_ADJUST_WITH_ACTIVITY); // 바인딩 시작
                }
                break;

            case R.id.stop: // 중지
                if(mBound) // 바인딩 상태라면
                {
                    Log.d(TAG, "onClick() stop");
                    unbindService(mConnection); // 바인드 종료
                    stopService(new Intent(this, MusicService.class)); // 서비스 종료
                    mBound = false; // 바인드 해제
                }
                // 메인 액티비티로 돌아오고 현재 액티비티를 종료한다.
                ouput = new Intent(this, MainActivity.class);
                startActivity(ouput);
                finish(); // 액티비티 종료
                break;

            case R.id.pause: // 일시 정지
                if(mBound) // 바인딩 되었다면
                {
                    Log.d(TAG, "onClick() pause");
                    b_play.setVisibility(View.VISIBLE); // 재생 버튼 보이게 함
                    b_pause.setVisibility(View.GONE); // 일시정지 버튼 안보이게 함
                    mService.music_pause(); // 서비스의 음악 일시정지 함수 호출
                }
                break;

            case R.id.prev: // 이전 곡 재생
                Log.d(TAG, "onClick() prev");
                pos = (file_addr.size() + pos - 1) % file_addr.size(); // 인덱스 1감소
                m_name.setText(file_name.get(pos));  // 텍스트를 음악 이름으로 설정
                b_play.setVisibility(View.GONE);      // 재생 버튼 안보이게 함
                b_pause.setVisibility(View.VISIBLE); // 일시정지 버튼 보이게 함

                if(mBound) // 바인딩 되었다면
                {
                    mService.music_prev(); // 음악 이전 곡 재생
                }
                else // 바인딩 되지 않은 상태
                {
                    ouput.putExtra("pos",pos); // 현재 인덱스 보냄
                    startService(ouput); // 서비스 시작
                    bindService(ouput, mConnection, Context.BIND_ADJUST_WITH_ACTIVITY); // 바인딩 시작
                }
                break;

            case R.id.next: // 다음 곡 재생을 누르면
                Log.d(TAG, "onClick() next");
                pos = (file_addr.size() + pos + 1) % file_addr.size(); // 인덱스 1증가
                m_name.setText(file_name.get(pos));  // 텍스트를 음악 이름으로 설정
                b_play.setVisibility(View.GONE);      // 재생 버튼 안보이게 함
                b_pause.setVisibility(View.VISIBLE); // 일시정지 버튼 보이게 함

                if(mBound) // 바인딩 되었다면
                {
                    mService.music_next(); // 음악 다음 곡 재생
                }
                else // 바인딩 되지 않았다면
                {
                    ouput.putExtra("pos",pos); // 현재 인덱스 보냄
                    startService(ouput); // 서비스 시작
                    bindService(ouput, mConnection, Context.BIND_ADJUST_WITH_ACTIVITY); // 바인딩 시작
                }
                break;
        }
    }
}
