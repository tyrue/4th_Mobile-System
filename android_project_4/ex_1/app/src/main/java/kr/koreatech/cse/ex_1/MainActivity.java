package kr.koreatech.cse.ex_1;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity
{
    EditText[] editTexts = new EditText[5];
    String[] texts = new String[4];
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editTexts[0] = (EditText)findViewById(R.id.이름);
        editTexts[1] = (EditText)findViewById(R.id.학교);
        editTexts[2] = (EditText)findViewById(R.id.학부);
        editTexts[3] = (EditText)findViewById(R.id.학번);
        editTexts[4] = (EditText)findViewById(R.id.result);
    }

    public void click_save(View view)
    {
        for(int i = 0; i < 4; i++)
            texts[i] = editTexts[i].getText().toString();
        editTexts[4].setText("");
        editTexts[4].setText("이름:" + texts[0] + "\n" +
                               "학교:" + texts[1] + "\n" +
                                "학부:" + texts[2] + "\n" +
                                "학번:" + texts[3] + "\n");
    }
}
