package com.example.myclip.ui.Login;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.example.myclip.R;
import com.example.myclip.ui.TableLayout.MainActivity;


public class LoginActivity extends AppCompatActivity {

    public static final String CHANNEL_NAME = "channelName";
    public static final String USER_ID = "userId";

    private TextView channelName, userId;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        channelName = findViewById(R.id.login_channel_name_input);
        userId = findViewById(R.id.login_user_id_input);
        button = findViewById(R.id.login_button);

    }

    @Override
    protected void onStart() {
        super.onStart();

        button.setOnClickListener(v -> {
            Intent mainActivity = new Intent(this, MainActivity.class);
            mainActivity.putExtra(CHANNEL_NAME, channelName.getText().toString());
            mainActivity.putExtra(USER_ID, userId.getText().toString());
            startActivity(mainActivity);
        });
    }
}
