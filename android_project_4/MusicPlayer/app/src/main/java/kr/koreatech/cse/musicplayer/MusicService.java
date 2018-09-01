package kr.koreatech.cse.musicplayer;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

import static android.content.Intent.getIntent;

public class MusicService extends Service
{
    private static final String TAG = "MusicService";
    ArrayList<String> file_addr; // 음악 리스트
    ArrayList<String> file_name; // 음악 리스트
    int pos;                 // 재생할 음악 위치

    Notification.Builder  noti; // 노티피케이션 객체
    MediaPlayer mediaPlayer;   // 미디어 객체
    Intent in;                  // 인텐트 객체
    PendingIntent pIntent;     // 팬딩인텐트 객체

    @Override
    public void onCreate()
    {
        Log.d(TAG, "onCreate()");
    }

    public class LocalBinder extends Binder // Binder 클래스를 상속 받는 클래스를 정의
    {
        // 클라이언트가 호출할 수 있는 공개 메소드가 있는 현재 Service 객체 반환
        MusicService getService() // getService() 메소드에서 현재 서비스 객체를 반환
        {
            return MusicService.this;
        }
    }
    private final IBinder mBinder = new LocalBinder(); // 위에서 정의한 Binder 클래스의 객체 생성
    @Override
    public IBinder onBind(Intent intent) // Service 연결이 되었을 때 호출되는 메소드
    {
        Log.d("LocalService", "onBind()");

        // 위에서 생성한 LocalBinder 객체를 반환
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) // 서비스 시작
    {
        Log.d(TAG, "onStartCommand()");
        Toast.makeText(this, "MusicService 시작", Toast.LENGTH_SHORT).show();
        try
        {
            file_addr = intent.getStringArrayListExtra("file_addr"); // 음악 주소 리스트 얻어옴
            file_name = intent.getStringArrayListExtra("file_name"); // 음악 이름 리스트 얻어옴
            pos = intent.getIntExtra("pos",0); // 클릭한 음악 번호 얻어옴

            mediaPlayer = new MediaPlayer(); // 미디어 플레이어 설정함
            mediaPlayer.setDataSource(file_addr.get(pos)); // 재생할 데이터 설정
            mediaPlayer.prepare(); // 준비
            mediaPlayer.start(); // 음악 재생

            // 1-1. PlayActivity 클래스를 실행하기 위한 Intent 객체
            in = new Intent(this, PlayActivity.class);
            // 1-2. Intent 객체를 이용하여 PendingIntent 객체를 생성 - Activity를 실행하기 위한 PendingIntent
            in.putStringArrayListExtra("file_addr",file_addr); // 음악 주소 리스트 보냄
            in.putStringArrayListExtra("file_name",file_name); // 음악 이름 리스트 보냄
            in.putExtra("pos", pos); // 현재 음악 인덱스 보냄
            in.putExtra("isplay", 1); // 재생 상태 보냄

            // PendingIntent객체 생성
            pIntent = PendingIntent.getActivity(this, 0, in, PendingIntent.FLAG_UPDATE_CURRENT);
            // 1-3. Notification 객체 생성
            noti = new Notification.Builder(this)
                    .setContentTitle("MusicPlayer") // 앱 이름 표시
                    .setContentText(file_name.get(pos)) // 음악 이름 표시
                    .setSmallIcon(R.mipmap.ic_launcher_round) // 아이콘 표시
                    .setContentIntent(pIntent); // 인텐트 설정
            // 2. foregound service 설정 - startForeground() 메소드 호출, 위에서 생성한 nofication 객체 넘겨줌
            startForeground(123, noti.build()); // 서비스가 kill 되지 않도록 함
        }
        catch (IOException e){ e.printStackTrace(); }
        return START_STICKY; // Kill 된 서비스를 다시 실행되도록 만들기
    }

    public void music_play() // 음악 재생
    {
        Toast.makeText(this, "MusicService 재생", Toast.LENGTH_SHORT).show();

        // 노티피케이션 클릭시 값을 액티비티로 보낸다
        in.putExtra("isplay", 1); // 재생 상태로 설정
        pIntent = PendingIntent.getActivity(this, 0, in, PendingIntent.FLAG_UPDATE_CURRENT);
        noti.setContentIntent(pIntent);
        startForeground(123, noti.build()); // 서비스가 kill 되지 않도록 함

        mediaPlayer.start(); // 음악 재생
    }

    public void music_pause() // 일시 정지
    {
        Toast.makeText(this, "MusicService 일시정지", Toast.LENGTH_SHORT).show();

        // 노티피케이션 클릭시 값을 액티비티로 보낸다
        in.putExtra("isplay", 2); // 일시정지 상태로 설정
        pIntent = PendingIntent.getActivity(this, 0, in, PendingIntent.FLAG_UPDATE_CURRENT);
        noti.setContentIntent(pIntent);
        startForeground(123, noti.build()); // 서비스가 kill 되지 않도록 함

        mediaPlayer.pause(); // 일시 정지
    }

    public void music_prev() // 이전 곡 실행
    {
        try
        {
            pos = (file_addr.size() + pos - 1) % file_addr.size(); // 인덱스 1감소
            mediaPlayer.reset(); // 재생 중지
            mediaPlayer.setDataSource(file_addr.get(pos)); // 파일 설정
            mediaPlayer.prepare(); // 준비
            mediaPlayer.start(); // 음악 재생

            // 노티피케이션 클릭시 값을 액티비티로 보낸다
            in.putExtra("pos",pos); // 현재 인덱스 보냄
            pIntent = PendingIntent.getActivity(this, 0, in, PendingIntent.FLAG_UPDATE_CURRENT);
            noti.setContentText(file_name.get(pos)); // 음악 이름 변경
            noti.setContentIntent(pIntent); // pIntent 변경
            startForeground(123, noti.build()); // 서비스가 kill 되지 않도록 함
            Toast.makeText(this, "MusicService 이전곡 재생", Toast.LENGTH_SHORT).show();
        }
        catch (IOException e){ e.printStackTrace(); }
    }

    public void music_next() // 다음 곡 실행
    {
        try
        {
            pos = (file_addr.size() + pos + 1) % file_addr.size(); // 인덱스 1 증가
            mediaPlayer.reset(); // 재생 중지
            mediaPlayer.setDataSource(file_addr.get(pos)); // 파일 변경
            mediaPlayer.prepare(); // 준비
            mediaPlayer.start(); // 음악 재생

            // 노티피케이션 클릭시 값을 액티비티로 보낸다.
            in.putExtra("pos",pos); // 현재 인덱스 보냄
            pIntent = PendingIntent.getActivity(this, 0, in, PendingIntent.FLAG_UPDATE_CURRENT);
            noti.setContentText(file_name.get(pos)); // 음악 이름 변경
            noti.setContentIntent(pIntent); // pIntent 변경
            startForeground(123, noti.build()); // 서비스가 kill 되지 않도록 함
            Toast.makeText(this, "MusicService 다음곡 재생", Toast.LENGTH_SHORT).show();
        }
        catch (IOException e){ e.printStackTrace(); }
    }

    @Override
    public void onDestroy() // 서비스 종료
    {
        super.onDestroy();
        mediaPlayer.stop();     // 재생 중지
        mediaPlayer.release();  // 음악 종료
        mediaPlayer = null;     // 연결 끊음
        Toast.makeText(this, "MusicService 종료", Toast.LENGTH_SHORT).show();
        Log.i("MobileProgramming", "onDestory()");
    }
}