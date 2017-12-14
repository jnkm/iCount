package com.example.jordan.icount;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.content.Intent;

import android.widget.EditText;
import android.widget.TextView;;
import android.widget.Toast;
import android.provider.MediaStore;
import android.database.Cursor;
import android.net.Uri;
import android.widget.ImageView;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;


public class HomeScreen extends AppCompatActivity {

    private Button mCameraButton;
    private Button mGalleryButton;
    private static int RESULT_LOAD_IMG = 1;
    private static int REQUEST_IMAGE_CAPTURE = 2;
    static final int REQUEST_TAKE_PHOTO = 3;
    String imgDecodableString;
    private TextView textout;
    private String mCurrentPhotoPath;
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private AlbumStorageDirFactory mAlbumStorageDirFactory = null;


    abstract class AlbumStorageDirFactory {
        public abstract File getAlbumStorageDir(String albumName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        mCameraButton = (Button) findViewById(R.id.home_button_1);
        mGalleryButton = (Button) findViewById(R.id.home_button_2);

        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureIMGWrapper();

            }
        });

        mGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, RESULT_LOAD_IMG);

            }
        });
    }

    private void captureIMG() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri imageUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),"fname_" +
                String.valueOf(System.currentTimeMillis()) + ".jpg"));
        Uri photoURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".my.package.name.provider", createImageFile());
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    private void captureIMGWrapper() {
        int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showMessageOKCancel("You need to allow access to Files",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                                        REQUEST_CODE_ASK_PERMISSIONS);
                            }
                        });
                return;
            }
            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }
        captureIMG();
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(HomeScreen.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    public static class App {
        public static File _file;
        public static File _dir;
        public static Bitmap bitmap;
    }






    private void capturePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            File photoFile = null;
            try {
                photoFile = setUpPhotoFile();
                mCurrentPhotoPath = photoFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
                photoFile = null;
                mCurrentPhotoPath = null;
            }
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMG) {
            try {
                // When an Image is picked
                if (resultCode == RESULT_OK && null != data) {
                    // Get the Image from data
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    // Get the cursor
                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    // Move to first row
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    imgDecodableString = cursor.getString(columnIndex);
                    cursor.close();

                    Intent start = new Intent(HomeScreen.this, ImagePreview.class);
                    start.putExtra("image_path", imgDecodableString);
                    startActivity(start);
                } else {
                    Toast.makeText(this, "You haven't picked Image", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
            }
        }
        else if (requestCode == REQUEST_IMAGE_CAPTURE) {



        }

    }





    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir();
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        return imageF;
    }

    private File setUpPhotoFile() throws IOException {

        File f = createImageFile();
        mCurrentPhotoPath = f.getAbsolutePath();

        return f;
    }

    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());

            if (storageDir != null) {
                if (! storageDir.mkdirs()) {
                    if (! storageDir.exists()){
                        Log.d("CameraSample", "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }
    private String getAlbumName() {
        return getString(R.string.album_name);
    }


    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

//    private void dispatchTakePictureIntentWrapper() {
//        int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
//            if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                showMessageOKCancel("You need to allow access to Photos",
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                                        REQUEST_CODE_ASK_PERMISSIONS);
//                            }
//                        });
//                return;
//            }
//            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    REQUEST_CODE_ASK_PERMISSIONS);
//            return;
//        }
//        dispatchTakePictureIntent();
//    }

//    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
//        new AlertDialog.Builder(HomeScreen.this)
//                .setMessage(message)
//                .setPositiveButton("OK", okListener)
//                .setNegativeButton("Cancel", null)
//                .create()
//                .show();
//    }

}
