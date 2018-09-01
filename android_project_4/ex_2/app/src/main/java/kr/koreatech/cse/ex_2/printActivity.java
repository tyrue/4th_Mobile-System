package kr.koreatech.cse.ex_2;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

public class printActivity extends AppCompatActivity
{
    EditText result;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);
        result = (EditText)findViewById(R.id.result);
        Intent in = getIntent();
        String s = in.getStringExtra("print");

        result.setText(s);
    }

    public void click_re(View view)
    {
        Intent in = new Intent(this, MainActivity.class);
        startActivity(in);
        finish();
    }
}

