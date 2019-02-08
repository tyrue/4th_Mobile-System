package kr.koreatech.cse.termproject;

// logData 선언 예제
//
//      LogData a = new LogData("21:50", "22:50", true, "", 450);
//      //(시작시간, 끝난시간, 이동여부, 장소, 걸음수);
//      //시간은 시:분 의 형태로 스트링으로 넣을 것. getCurrentHourAndMin()함수를 이용하면 됨.
public class LogData
{
    private String startTime;       // 시작 시간
    private String endTime;         // 종료 시간
    private boolean isMove = false; // 움직임 여부
    private String location;        // 위치, 정지한 상태일 때만
    private int step = 0;           // 걸음수, 이동한 상태일 때만

    public LogData(String startTime, String endTime, boolean isMove, String loc, int step) // 생성자
    {
        this.startTime = startTime;
        this.endTime = endTime;
        this.isMove = isMove;
        this.location = loc;
        this.step = step;
    }

    public String toString()
    {
        String res = "";
        res += startTime + "-" + endTime;
        //시작시간-끝나는시간
        res += " " + String.valueOf(CalcTime.timegap(startTime, endTime)) + "분";
        //시작시간부터 끝나는 시간까지의 시간
        if(isMove)
        {
            res+=" 이동 " + String.valueOf(step) + "걸음\n";
        }
        else
        {
            res+=" 정지 " + location + "\n";
        }
        return res;
    }
}
