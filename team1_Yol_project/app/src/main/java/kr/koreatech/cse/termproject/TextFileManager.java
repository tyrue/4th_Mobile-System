package kr.koreatech.cse.termproject;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class TextFileManager // 텍스트 파일 저장을 도와주는 클래스
{
    private static final String FILE_NAME = "Location tracking.txt"; // 저장될 파일 이름
    private File folder;        // 파일 폴더
    private String folderPath;  // 폴더의 절대 경로
    private String filePath;    // 파일을 저장할 절대 경로

    public TextFileManager() // 생성자
    {
        // 외부 공용 디렉토리 중 Download 디렉토리에 대한 File 객체 얻음
        folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        // 절대 경로 String 값을 얻음
        folderPath = folder.getAbsolutePath();
        // 로그 파일 절대 경로 생성 (String 값)
        filePath = folderPath + "/" + FILE_NAME;
    }

    // 파일에 문자열 데이터를 쓰는 메소드, 처음부터 쓰기 모드이다.
    public void save_new(String data)
    {
        // 문자열이 비어있으면 저장하지 않음
        if (data == null || data.isEmpty() == true)
        {
            return;
        }

        FileOutputStream fos; // 파일 출력 객체
        try
        {
            fos = new FileOutputStream(filePath, false); // 처음쓰기 모드로 쓰기
            fos.write(data.getBytes());
            fos.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // 파일에 문자열 데이터를 쓰는 메소드, 이어쓰기 모드이다.
    public void save_append(String data)
    {
        // 문자열이 비어있으면 저장하지 않음
        if (data == null || data.isEmpty() == true)
        {
            return;
        }

        FileOutputStream fos; // 파일 출력 객체
        try
        {
            fos = new FileOutputStream(filePath, true); // 두번째 매개변수가 true이면 이어쓰기 모드로 쓰기
            fos.write(data.getBytes());
            fos.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // 파일에서 데이터를 읽고 문자열 데이터로 반환하는 메소드
    public String load()
    {
        try
        {
            // 로그 파일의 절대 경로를 이용하여 File 객체 생성
            File log = new File(filePath);
            // File 객체를 이용하여 해당 파일이 실제로 존재하는지 검사
            if(log.exists())
            {
                // 파일이 존재하는 경우 읽기 수행
                FileInputStream fis = new FileInputStream(filePath);
                byte[] data = new byte[fis.available()];
                fis.read(data);

                fis.close();
                return new String(data);
            }
            else
            {
                Log.i("FileManager", log.getName() + " file does not exist");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return "";
    }

    // 파일 삭제 메소드
    public boolean delete()
    {
        // 로그 파일의 절대 경로를 이용하여 File 객체 생성
        File log = new File(filePath);
        try
        {
            // File 객체를 이용하여 해당 파일 삭제
            boolean result = log.delete();

            if (result)
            {
                // file is successfully deleted
                Log.i("FileManager", log.getName() + " successfully deleted");
                return true;
            }
            else
            {
                Log.i("FileManager", "delete failed");
                return false;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }
}