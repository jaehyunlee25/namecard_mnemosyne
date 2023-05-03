package com.mnemosyne.namecard;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_WRITE_CONTACTS = 100;

    private static final int PERMISSIONS_REQUEST_READ_MEDIA_IMAGES = 201;
    private static final int REQUEST_CODE_PICK_IMAGE = 300;
    private ActivityResultLauncher<String> imagePickerActivityResultLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toast.makeText(this, "onCreate", Toast.LENGTH_SHORT).show();

        // Check if permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CONTACTS}, PERMISSIONS_REQUEST_WRITE_CONTACTS);
        }

        Toast.makeText(this, "READ_MEDIA_IMAGES", Toast.LENGTH_SHORT).show();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Read Image permission", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSIONS_REQUEST_READ_MEDIA_IMAGES);
        } else {
            Toast.makeText(this, "READ_MEDIA_IMAGES ELSE", Toast.LENGTH_SHORT).show();
            pickImageFromGallery();
        }

        // Initialize the ActivityResultLauncher for the image picker
        imagePickerActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::handleImagePicked
        );

        // Find the TextView element in the layout
        TextView textView = findViewById(R.id.textView);

        // Set the text of the TextView element
        textView.setText("Name Card");

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_CONTACTS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, you can insert contact now
                    // insertContact("John Doe", "1234567890");
                } else {
                    // Permission denied
                    Toast.makeText(this, "Write Contacts permission denied", Toast.LENGTH_SHORT).show();
                }
            }
            case PERMISSIONS_REQUEST_READ_MEDIA_IMAGES: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, you can select an image now
                    Toast.makeText(this, "Read Image permission accepted", Toast.LENGTH_SHORT).show();
                    pickImageFromGallery();
                } else {
                    // Permission denied
                    Toast.makeText(this, "Read External Storage permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void pickImageFromGallery() {
        Toast.makeText(this, "pickImageFromGallery", Toast.LENGTH_SHORT).show();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSIONS_REQUEST_READ_MEDIA_IMAGES);
        } else {
            // Launch the image picker
            Toast.makeText(this, "pickImageFromGallery ELSE", Toast.LENGTH_SHORT).show();
            imagePickerActivityResultLauncher.launch("image/*");
        }
    }
    private void handleImagePicked(Uri selectedImageUri) {
        if (selectedImageUri != null) {
            String imagePath = getPathFromUri(selectedImageUri);
            if (imagePath != null) {
                uploadImageToServer(imagePath);
            }
        }
    }
    public interface ApiService {
        @Multipart
        @POST("api/upload")
        Call<ResponseBody> uploadImage(@Part MultipartBody.Part image);
    }
    private void uploadImageToServer(String imagePath) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://example.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        File imageFile = new File(imagePath);
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", imageFile.getName(), requestBody);

        Call<ResponseBody> call = apiService.uploadImage(imagePart);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("Upload", "Image uploaded successfully");
                } else {
                    Log.e("Upload", "Failed to upload image: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Upload", "Error uploading image", t);
            }
        });
    }
    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(columnIndex);
        cursor.close();
        return path;
    }
    private void insertContact(String name, String phoneNumber) {
        ContentValues contentValues = new ContentValues();

        // Add the contact to the raw_contacts table
        contentValues.put(ContactsContract.RawContacts.ACCOUNT_TYPE, (String) null);
        contentValues.put(ContactsContract.RawContacts.ACCOUNT_NAME, (String) null);

        Uri rawContactUri = getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, contentValues);
        if (rawContactUri == null) return;
        long rawContactId = ContentUris.parseId(rawContactUri);

        // Add the contact name
        contentValues.clear();
        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        contentValues.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name);

        getContentResolver().insert(ContactsContract.Data.CONTENT_URI, contentValues);

        // Add the contact phone number
        contentValues.clear();
        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        contentValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber);
        contentValues.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);

        getContentResolver().insert(ContactsContract.Data.CONTENT_URI, contentValues);
    }


}
