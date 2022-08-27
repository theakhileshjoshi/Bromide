package com.example.bromide;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bromide.databinding.ActivityProfileSetupBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileSetupActivity extends AppCompatActivity {

    ActivityProfileSetupBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;
    Uri selectedImage;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dialog = new ProgressDialog(this);
        dialog.setMessage("Updating Profile");

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        binding.uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 45);
            }
        });

        binding.btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = binding.editTextPersonName.getText().toString();

                if (name.isEmpty()) binding.editTextPersonName.setError("Enter User Name");
                else {
                    dialog.show();
                    if (selectedImage != null) {

                        StorageReference reference = storage.getReference().child("Profiles").child(auth.getUid());
                        reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                if (task.isSuccessful()) {
                                    reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            String uid = auth.getUid();
                                            String phone = auth.getCurrentUser().getPhoneNumber();
                                            String name = binding.editTextPersonName.getText().toString();
                                            String imageURL = uri.toString();
                                            User user = new User(uid, name, phone, imageURL);

                                            database.getReference()
                                                    .child("users")
                                                    .child(uid)
                                                    .setValue(user)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void unused) {
                                                            dialog.dismiss();
                                                            Intent intent = new Intent(ProfileSetupActivity.this, MainActivity.class);
                                                            startActivity(intent);
                                                            finish();
                                                        }
                                                    });

                                        }
                                    });
                                }
                            }
                        });
                    } else {
                        String uid = auth.getUid();
                        String phone = auth.getCurrentUser().getPhoneNumber();
                        String imageURL = "No image";
                        User user = new User(uid, name, phone, imageURL);

                        database.getReference()
                                .child("users")
                                .child(uid)
                                .setValue(user)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        dialog.dismiss();
                                        Intent intent = new Intent(ProfileSetupActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                });
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null) {
            if (data.getData() != null) {
                binding.profileImage.setImageURI(data.getData());
                selectedImage = data.getData();

                Bitmap bmp = null;
                try {
                    bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                // "RECREATE" THE NEW BITMAP
                int width = (int) (bmp.getWidth() - bmp.getWidth()*0.85);
                int height = (int) (bmp.getHeight()- bmp.getHeight()*0.85);

                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bmp,width,height,true);
                bmp.recycle();

                //here you can choose quality factor in third parameter(ex. i choosen 25)
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                String path = MediaStore.Images.Media.insertImage(getContentResolver(), resizedBitmap, "Title", null);
                selectedImage = Uri.parse(path);
            }
        }
    }
}