package com.example.chatapp;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;
import android.content.Intent;


public class PhoneLoginActivity extends AppCompatActivity {

    private Button SendVerificationCodeButton, VerifyButton;
    private EditText InputPhoneNumber, InputVerificationCode;

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;


    private FirebaseAuth mAuth;

    private ProgressDialog loadingBar;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        loadingBar = new ProgressDialog(this);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_phone_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        SendVerificationCodeButton =(Button)findViewById(R.id.send_ver_code_button);
        VerifyButton =(Button)findViewById(R.id.Verify_button);
        InputPhoneNumber =(EditText) findViewById(R.id.phone_number_input);
        InputVerificationCode =(EditText)findViewById(R.id.Verification_code_input);

        SendVerificationCodeButton.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                /*
                SendVerificationCodeButton.setVisibility(View.VISIBLE);
                InputPhoneNumber.setVisibility(View.VISIBLE);


                VerifyButton.setVisibility(View.VISIBLE);
                InputVerificationCode.setVisibility(View.VISIBLE); */

                /* nuevo a declarar paso 24 */
                loadingBar.setTitle("Phone Verification");
                loadingBar.setMessage("Porfavor espera, estamos autenticacion tu telefono");

                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                String phoneNumber = InputPhoneNumber.getText().toString().trim();

                if (TextUtils.isEmpty(phoneNumber)) {
                    Toast.makeText(PhoneLoginActivity.this, "Phone number is required...", Toast.LENGTH_SHORT).show();
                } else if (!phoneNumber.startsWith("+502") || phoneNumber.length() != 12) {
                    Toast.makeText(PhoneLoginActivity.this, "Número guatemalteco inválido. Usa el formato +502XXXXXXXX", Toast.LENGTH_LONG).show();
                    loadingBar.dismiss();
                    return;
                } else {
                    // Verificación por Firebase
                    PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                            .setPhoneNumber(phoneNumber)
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(PhoneLoginActivity.this)
                            .setCallbacks(callbacks)
                            .build();

                    PhoneAuthProvider.verifyPhoneNumber(options);
                }


            }
        }));


        /* */
        VerifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SendVerificationCodeButton.setVisibility(View.INVISIBLE);
                InputPhoneNumber.setVisibility(View.INVISIBLE);

                String verificationCode = InputVerificationCode.getText().toString();
                if (TextUtils.isEmpty(verificationCode)){
                    Toast.makeText(PhoneLoginActivity.this, "Porfavor escriba el código de verificación primero", Toast.LENGTH_SHORT).show();
                }
                else {
                    /* verificacion del codigo */

                    loadingBar.setTitle("Verification Code");
                    loadingBar.setMessage("Porfavor espera, estamos verificando el codigo de verificación");

                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();


                    /* 4. Phone auth Credential proyect */
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, verificationCode);
                    signInWithPhoneAuthCredential(credential);

                }
            }
        });

        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                loadingBar.dismiss();
                Log.e("PhoneLogin", "Verification failed: " + e.getMessage()); // <-- agrega esto
                Toast.makeText(PhoneLoginActivity.this, "Número de teléfono invalido: " + e.getMessage(), Toast.LENGTH_LONG).show();

                SendVerificationCodeButton.setVisibility(View.VISIBLE);
                InputPhoneNumber.setVisibility(View.VISIBLE);

                VerifyButton.setVisibility(View.INVISIBLE);
                InputVerificationCode.setVisibility(View.INVISIBLE);
            }



            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {



                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                loadingBar.dismiss();

                // nueva linea
                Toast.makeText(PhoneLoginActivity.this, "El código ha sido enviado, porfavor revise", Toast.LENGTH_SHORT).show();

                /* pegando codigo */
                SendVerificationCodeButton.setVisibility(View.INVISIBLE);
                InputPhoneNumber.setVisibility(View.INVISIBLE);


                VerifyButton.setVisibility(View.VISIBLE);
                InputVerificationCode.setVisibility(View.VISIBLE);

            }
        };

        }


    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        loadingBar.dismiss();
                        Toast.makeText(this, "Congratulations, you're logged is successfully...", Toast.LENGTH_SHORT).show();

                        SendUserToMainActivity();
                    } else {
                        String message = task.getException().toString();
                        Toast.makeText(this, "Error : " + message, Toast.LENGTH_SHORT).show();

                    }
                });
    }

    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent (PhoneLoginActivity.this, MainActivity.class);
        startActivity(mainIntent);
        finish();

    }


}