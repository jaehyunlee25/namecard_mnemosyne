package com.mnemosyne.namecard;

import android.Manifest;
/*import android.content.ContentUris;
import android.content.ContentValues;*/
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
/*import android.provider.ContactsContract;*/
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

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
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 200;
    private static final int PERMISSIONS_REQUEST_READ_MEDIA_IMAGES = 201;
    private ActivityResultLauncher<String> imagePickerActivityResultLauncher;
    private String baseurl = "https://dev.mnemosyne.co.kr/namecard/";
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView textView = findViewById(R.id.textView);
        // Set the text of the TextView element
        String title = "";
        textView.setText(title);

        // Initialize the ActivityResultLauncher for the image picker
        imagePickerActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::handleImagePicked
        );

        // Check if permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CONTACTS}, PERMISSIONS_REQUEST_WRITE_CONTACTS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Upload", "READ_MEDIA_IMAGES request");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSIONS_REQUEST_READ_MEDIA_IMAGES);
        } else  {
            imagePickerActivityResultLauncher.launch("image/*");
        }

        /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            Log.d("Upload", "READ_EXTERNAL_STORAGE request");
            ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
            );
        }*/
    }
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_CONTACTS: {
                // Permission granted, you can insert contact now
                // insertContact("John Doe", "1234567890");
                // Permission denied
                // Toast.makeText(this, "Write Contacts permission denied", Toast.LENGTH_SHORT).show();
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                }
            }
            case PERMISSIONS_REQUEST_READ_MEDIA_IMAGES: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, you can select an image now
                    // Toast.makeText(this, "Read Image permission accepted", Toast.LENGTH_SHORT).show();
                    // pickImageFromGallery();
                    //Log.d("Upload", "PERMISSIONS_REQUEST_READ_MEDIA_IMAGES");
                } else {
                    // Permission denied
                    //Toast.makeText(this, "PERMISSIONS_REQUEST_READ_MEDIA_IMAGES denied", Toast.LENGTH_SHORT).show();
                }
            }
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, you can select an image now
                    //Toast.makeText(this, "Read External Storage permission granted", Toast.LENGTH_SHORT).show();
                    // pickImageFromGallery();
                } else {
                    // Permission denied
                    //Toast.makeText(this, "Read External Storage permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void handleImagePicked(@NonNull Uri selectedImageUri) {
        Log.d("Upload", "selectedImageUri:: " + selectedImageUri.toString());
        byte[] imageByte = getImageBytesFromUri(selectedImageUri);
        Map<String, Object> imageFile = getFileInformationFromUri(selectedImageUri);
        uploadImageToServer(imageByte, imageFile);
    }
    public interface ApiService {
        @Multipart
        @POST("dummy")
        Call<ResponseBody> uploadImage(@Part MultipartBody.Part image, @Part MultipartBody.Part data);
    }
    private void uploadImageToServer(byte[] imageBytes, Map<String, Object> imageFile) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseurl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        MediaType mediaType = MediaType.parse((String) imageFile.get("mimeType"));
        RequestBody paramImage = RequestBody.create(imageBytes, mediaType);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("file", (String) imageFile.get("fileName"), paramImage);
        MultipartBody.Part dataPart = MultipartBody.Part.createFormData("data", "1");

        Call<ResponseBody> call = apiService.uploadImage(imagePart, dataPart);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("Upload", "Image uploaded successfully");
                } else {
                    Log.e("Upload", "Failed to upload image: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e("Upload", "Error uploading image", t);
            }
        });
    }
    public Map<String, Object> getFileInformationFromUri(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        Map<String, Object> fileInfo = new HashMap<>();

        try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int cn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int cs = cursor.getColumnIndex(OpenableColumns.SIZE);
                String fileName = cursor.getString(cn);
                long fileSize = cursor.getLong(cs);

                fileInfo.put("fileName", fileName);
                fileInfo.put("fileSize", fileSize);

                int mimeTypeColumn = cursor.getColumnIndex("mime_type");
                if (mimeTypeColumn != -1) {
                    String mimeType = cursor.getString(mimeTypeColumn);
                    fileInfo.put("mimeType", mimeType);
                }
            }
        }

        return fileInfo;
    }
    public byte[] getImageBytesFromUri(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        byte[] imageData = null;

        try (InputStream inputStream = contentResolver.openInputStream(uri)) {
            if (inputStream != null) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }

                imageData = byteArrayOutputStream.toByteArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageData;
    }
    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Log.d("Upload", projection.toString());
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) {
            Log.d("Upload", "cursor null");
            return null;
        }
        if(cursor.moveToFirst()){
            while(cursor.moveToNext()){
                Log.d("Upload", String.valueOf(cursor.getInt(Integer.parseInt(MediaStore.Images.ImageColumns._ID))));
            }
            /*int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            Log.d("Upload", "columnIndex " + String.valueOf(columnIndex));
            String path = cursor.getString(columnIndex);
            Log.d("Upload", "path " + path);
            cursor.close();*/
            //String path = "";
            return null;
        }

        return null;
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
