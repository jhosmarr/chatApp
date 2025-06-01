package com.example.chatapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class SettingsActivity extends AppCompatActivity {

    private Button UpdateAccountSettings;
    private EditText userName, userStatus;
    private CircleImageView userProfileImage;

    private String currentUserID;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<CropImageContractOptions> cropImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();

        InitializeFields();
        initializeLaunchers();

        UpdateAccountSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateSettings();
            }
        });

        RetrieveUserInfo();

        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                galleryLauncher.launch(galleryIntent);
            }
        });
    }

    private void initializeLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();

                        CropImageOptions cropOptions = new CropImageOptions();
                        cropOptions.guidelines = CropImageView.Guidelines.ON;
                        cropOptions.aspectRatioX = 1;
                        cropOptions.aspectRatioY = 1;
                        cropOptions.fixAspectRatio = true;
                        cropOptions.allowRotation = true;
                        cropOptions.allowFlipping = true;
                        cropOptions.showCropOverlay = true;
                        cropOptions.activityMenuIconColor = ContextCompat.getColor(this, R.color.black);

                        cropImageLauncher.launch(new CropImageContractOptions(imageUri, cropOptions));
                    }
                });

        /* reemplazando codigo croopImageLauncher */
        cropImageLauncher = registerForActivityResult(
                new CropImageContract(),
                result -> {
                    if (result.isSuccessful()) {
                        Uri uriContent = result.getUriContent();
                        userProfileImage.setImageURI(uriContent);

                        // Subir a Firebase Storage
                        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");
                        StorageReference filePath = storageRef.child(currentUserID + ".jpg");

                        filePath.putFile(uriContent).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                filePath.getDownloadUrl().addOnSuccessListener(uri -> {
                                    String downloadUrl = uri.toString();

                                    RootRef.child("Users").child(currentUserID).child("image").setValue(downloadUrl)
                                            .addOnCompleteListener(saveTask -> {
                                                if (saveTask.isSuccessful()) {
                                                    Toast.makeText(SettingsActivity.this, "Imagen actualizada correctamente", Toast.LENGTH_SHORT).show();
                                                    RetrieveUserInfo(); // Recarga la imagen desde la base de datos
                                                } else {
                                                    Toast.makeText(SettingsActivity.this, "Error al guardar URL en DB", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                });
                            } else {
                                Toast.makeText(SettingsActivity.this, "Error al subir imagen", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        Exception error = result.getError();
                        Toast.makeText(SettingsActivity.this, "Error al recortar imagen: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void RetrieveUserInfo() {
        RootRef.child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if ((snapshot.exists()) && (snapshot.hasChild("name") && (snapshot.hasChild("image")))) {
                    String retrieveUserName = snapshot.child("name").getValue().toString();
                    String retrieveStatus = snapshot.child("status").getValue().toString();
                    String retrieveProfileImage = snapshot.child("image").getValue().toString();

                    userName.setText(retrieveUserName);
                    userStatus.setText(retrieveStatus);

                    // Mostrar la imagen con Glide
                    Glide.with(SettingsActivity.this)
                            .load(retrieveProfileImage)
                            .placeholder(R.drawable.profile_image)
                            .into(userProfileImage);

                } else if ((snapshot.exists()) && (snapshot.hasChild("name"))) {
                    String retrieveUserName = snapshot.child("name").getValue().toString();
                    String retrieveStatus = snapshot.child("status").getValue().toString();

                    userName.setText(retrieveUserName);
                    userStatus.setText(retrieveStatus);
                } else {
                    Toast.makeText(SettingsActivity.this, "Please set & update your profile information...", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void UpdateSettings() {
        String setUserName = userName.getText().toString();
        String setStatus = userStatus.getText().toString();

        if (TextUtils.isEmpty(setUserName)) {
            Toast.makeText(this, "Please write your user name first...", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(setStatus)) {
            Toast.makeText(this, "Please write your status...", Toast.LENGTH_SHORT).show();
        } else {
            HashMap<String, String> profileMap = new HashMap<>();
            profileMap.put("uid", currentUserID);
            profileMap.put("name", setUserName);
            profileMap.put("status", setStatus);

            RootRef.child("Users").child(currentUserID).setValue(profileMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(SettingsActivity.this, "Perfil actualizado correctamente.", Toast.LENGTH_SHORT).show();
                                RetrieveUserInfo(); // Refresca los datos visibles
                            } else {
                                String message = task.getException().toString();
                                Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }


    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(SettingsActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    private void InitializeFields() {
        UpdateAccountSettings = findViewById(R.id.update_settings_button);
        userName = findViewById(R.id.set_user_name);
        userStatus = findViewById(R.id.set_profile_status);
        userProfileImage = findViewById(R.id.set_profile_image);
    }
}

