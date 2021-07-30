package com.doc.verification;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class UploadActivity extends AppCompatActivity {
    private ImageView id_image;
    private MaterialButton uploadButton;
    private static final int REQUEST_IMAGE = 10;
    private DatabaseReference idRef;
    private StorageReference idStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        id_image = findViewById(R.id.id_image);
        uploadButton = findViewById(R.id.uploadButton);

        idRef = FirebaseDatabase.getInstance().getReference("Document");
        idStorage = FirebaseStorage.getInstance().getReference("Document");

        selectPhoto();
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE}, 20);
        }
    }

    private void selectPhoto() {
        id_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPermission();
                openGallery();
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            Picasso.get().load(uri).into(id_image);
        }
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadDocument(data.getData());
            }
        });
    }

    private void uploadDocument(Uri uri) {
        if (uri != null) {
            ProgressDialog pd = new ProgressDialog(this);
            pd.setCancelable(false);
            pd.setMessage("Uploading Photo");
            String docId = String.valueOf(System.currentTimeMillis());
            StorageReference ref = idStorage.child(docId + ".jpeg");
            ref.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    pd.dismiss();
                    Task<Uri> task = taskSnapshot.getStorage().getDownloadUrl();
                    while (!task.isComplete()) ;
                    Uri uri1 = task.getResult();
                    String url = uri1.toString();

                    idRef.child(docId).setValue(url);
                    Picasso.get().load(R.drawable.id_icon).into(id_image);
                    showSuccessDialog();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    pd.dismiss();
                    Toast.makeText(UploadActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                    double prog = (taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount()) * 100;
                    pd.setMessage((int) prog + "% Uploading");
                    pd.show();
                }
            });
        }
    }

    private void showSuccessDialog() {
        Dialog successDialog = new Dialog(this);
        successDialog.setContentView(R.layout.success_layout);
        successDialog.setCancelable(false);
        successDialog.show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                successDialog.dismiss();
                UploadActivity.super.finish();
            }
        }, 1500);
    }
}