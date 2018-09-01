package kr.koreatech.cse.assignment1;

/**
 * Created by DooHyun on 2018-03-27.
 */
        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.Intent;
        import android.location.LocationManager;
        import android.widget.Toast;

public class AlertReceiver extends BroadcastReceiver // 근접 경보를 처리하는 리시버 클래스
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        // 접근 경보에 대한 상태
        boolean isEntering = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);
        String loc_name = intent.getStringExtra("loc_name"); // 현재 접근한 경보의 이름

        if(isEntering) // 접근했다면
            Toast.makeText(context, loc_name + "에 접근중입니다..", Toast.LENGTH_LONG).show();
        else // 벗어났다면
            Toast.makeText(context, loc_name + "에서 벗어납니다..", Toast.LENGTH_LONG).show();
    }
}