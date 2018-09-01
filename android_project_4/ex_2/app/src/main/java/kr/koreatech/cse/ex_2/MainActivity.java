package kr.koreatech.cse.ex_2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity
{
    EditText[] editTexts = new EditText[4];
    String[] texts = new String[4];
    String putText;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editTexts[0] = (EditText)findViewById(R.id.이름);
        editTexts[1] = (EditText)findViewById(R.id.학교);
        editTexts[2] = (EditText)findViewById(R.id.학부);
        editTexts[3] = (EditText)findViewById(R.id.학번);
    }

    public void click_save(View view)
    {
        Intent in = new Intent(MainActivity.this, printActivity.class);
        for(int i = 0; i < 4; i++)
            texts[i] = editTexts[i].getText().toString();
        putText = "이름:" + texts[0] + "\n" +
                    "학교:" + texts[1] + "\n" +
                    "학부:" + texts[2] + "\n" +
                    "학번:" + texts[3] + "\n";
        in.putExtra("print", putText);
        Log.d("test", putText);
        startActivity(in);
        finish();
    }
}
