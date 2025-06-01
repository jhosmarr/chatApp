package com.example.chatapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

public class RegisterActivity extends AppCompatActivity {

    private Button CreateAccountButton;
    private EditText UserEmail, UserPassword;
    private TextView AlreadyHaveAccountLink;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        RootRef = FirebaseDatabase.getInstance().getReference();

        InitializeFields();

        AlreadyHaveAccountLink.setOnClickListener(view -> SendUserToLoginActivity());

        CreateAccountButton.setOnClickListener(v -> CreateNewAccount());
    }

    private void CreateNewAccount() {
        String email = UserEmail.getText().toString();
        String password = UserPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter Email...", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter Password...", Toast.LENGTH_SHORT).show();
        } else {
            loadingBar.setTitle("Creating New Account");
            loadingBar.setMessage("Please wait while we are creating your account...");
            loadingBar.setCanceledOnTouchOutside(true);
            loadingBar.show();

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String currentUserID = mAuth.getCurrentUser().getUid();

                            // Guardar temporalmente nodo del usuario
                            RootRef.child("Users").child(currentUserID).setValue("")
                                    .addOnCompleteListener(saveTask -> {

                                        // Obtener y guardar device token
                                        FirebaseMessaging.getInstance().getToken()
                                                .addOnCompleteListener(tokenTask -> {
                                                    if (tokenTask.isSuccessful()) {
                                                        String deviceToken = tokenTask.getResult();
                                                        RootRef.child("Users").child(currentUserID).child("device_token")
                                                                .setValue(deviceToken)
                                                                .addOnCompleteListener(tokenSaveTask -> {
                                                                    if (tokenSaveTask.isSuccessful()) {
                                                                        SendUserToMainActivity();
                                                                        Toast.makeText(RegisterActivity.this, "Account Created Successfully...", Toast.LENGTH_SHORT).show();
                                                                    } else {
                                                                        Toast.makeText(RegisterActivity.this, "Failed to save device token", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                    loadingBar.dismiss();
                                                                });
                                                    } else {
                                                        Toast.makeText(RegisterActivity.this, "Failed to get device token", Toast.LENGTH_SHORT).show();
                                                        loadingBar.dismiss();
                                                    }
                                                });

                                    });

                        } else {
                            String message = task.getException().toString();
                            Toast.makeText(RegisterActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    });
        }
    }

    private void InitializeFields() {
        CreateAccountButton = findViewById(R.id.register_button);
        UserEmail = findViewById(R.id.register_email);
        UserPassword = findViewById(R.id.register_password);
        AlreadyHaveAccountLink = findViewById(R.id.already_have_account_link);
        loadingBar = new ProgressDialog(this);
    }

    private void SendUserToLoginActivity() {
        Intent loginIntent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(loginIntent);
    }

    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}
