package kr.koreatech.cse.termproject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CalcTime
{
    public static int timegap(String startTime, String endTime)  //시간차 구함
    {
        String[] startTime_hour_min = startTime.split(":");
        String[] endTime_hour_min = endTime.split(":");

        int hourGap = Integer.parseInt(endTime_hour_min[0]) - Integer.parseInt(startTime_hour_min[0]);
        int minGap =  Integer.parseInt(endTime_hour_min[1]) - Integer.parseInt(startTime_hour_min[1]);

        if(minGap < 0)
        {
            minGap+=60;
            hourGap-=1;
        }
        if(hourGap < 0)
        {
            hourGap += 24;
        }

        return hourGap*60 + minGap; //1시간에 60분이므로 시간*60 + 분
    }

    public static String getCurrentHour()
    {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.KOREA);
        Date currentTime = new Date();
        String dTime = formatter.format(currentTime);
        return dTime;
    }

    public static String getCurrentTime()  // 현재시간 출력하는 함수
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.KOREA);
        Date currentTime = new Date();
        String dTime = formatter.format(currentTime);
        return dTime;
    }
}