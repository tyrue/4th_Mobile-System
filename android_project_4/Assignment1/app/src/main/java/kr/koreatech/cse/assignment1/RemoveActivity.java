package kr.koreatech.cse.assignment1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by DooHyun on 2018-03-27.
 */

public class RemoveActivity extends AppCompatActivity
{
    int RESULT_OK = 3; // 요청 결과 코드
    EditText name; // 해제할 경보의 이름
    Button remove; // 해제 버튼
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remove);

        // 뷰 초기화
        name = (EditText)findViewById(R.id.name_ed);
        remove = (Button)findViewById(R.id.remove_b_r);
    }

    public void onClick(View view)
    {
        if(name.getText().toString() == "" || name.getText().toString().isEmpty()) // 이름이 입력되지 않으면 해제 못함
            Toast.makeText(this, "이름이 입력되지 않았습니다.", Toast.LENGTH_LONG).show();
        else
        {
            Intent in = new Intent(this, MainActivity.class); // 메인 액티비티로 전환
            in.putExtra("name_r", name.getText().toString()); // 입력받은 이름을 전달
            setResult(RESULT_OK, in);
            finish(); // add 액티비티 창을 닫는다.
        }
    }
}
