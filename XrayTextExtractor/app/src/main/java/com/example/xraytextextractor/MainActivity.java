package com.example.xraytextextractor;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    ImageView imgView;
    Button Choose, Capture, Copy;
    TextView outputText;
    ActivityResultLauncher<String> requestPermissionLauncher;
    ActivityResultLauncher<Uri> takePictureLauncher;
    ActivityResultLauncher<Intent> choosePictureLauncher;
    String str, photoPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imgView = findViewById(R.id.imageView);
        Choose = findViewById(R.id.Choose);
        Capture = findViewById(R.id.Capture);
        Copy = findViewById(R.id.copy);
        outputText = findViewById(R.id.outputText);

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean isGranted) {
                if(isGranted){
                    captureImage();
                } else {
                    Toast.makeText(MainActivity.this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        });

        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean success) {
                if(success){
                    if(photoPath != null){
                        int rotationDegrees = 0;
                        Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
                        try {
                            rotationDegrees = getImageRotationDegrees(MainActivity.this, "0", false);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                        imgView.setImageBitmap(bitmap);
                        recognizeText(bitmap, rotationDegrees);
                    }
                }
            }
        });

        choosePictureLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode() == Activity.RESULT_OK){
                    Intent data = result.getData();
                    if(data != null){
                        Uri uri = data.getData();
                        imgView.setImageURI(uri);
                        try {
                            Bitmap image = MediaStore.Images.Media.getBitmap(MainActivity.this.getContentResolver(), uri);
                            recognizeText(image, 0);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        });

        Capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        Choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                choosePictureLauncher.launch(intent);
            }
        });

    }

    private File createimageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile("JPEG_"+timestamp+"_",".jpg",storageDir);
        photoPath = image.getAbsolutePath();
        return image;
    }

    private void captureImage(){
        File photoFile = null;
        try {
            photoFile = createimageFile();
        } catch (IOException e) {
            Toast.makeText(this, "Error occured while creating image file", Toast.LENGTH_SHORT).show();
        }
        if(photoFile != null){
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName()  + ".provider", photoFile);
            takePictureLauncher.launch(photoUri);
        }
    }

    private void recognizeText( Bitmap bitmap, int rotationDegree){
        InputImage image = InputImage.fromBitmap(bitmap, rotationDegree);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image).addOnSuccessListener(new OnSuccessListener<Text>() {
            @Override
            public void onSuccess(Text text) {
                str = "";
                for(Text.TextBlock block : text.getTextBlocks()){
                    for(Text.Line line : block.getLines()){
                        str = str + line.getText() + "\n";
                    }
                    str = str + "\n";
                }
                outputText.setText(str);
                Copy.setVisibility(View.VISIBLE);
                Copy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("extracted text", str);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(MainActivity.this, "Text Copied", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Failed to recognize text", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Below code is not written by me it is given by android ml-kit to calculate rotation angle
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static int getImageRotationDegrees(Activity activity, String cameraId, boolean isFrontFacing) throws CameraAccessException {
        SparseIntArray ORIENTATIONS = new SparseIntArray();
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);

        int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotationFromDisplay = ORIENTATIONS.get(deviceRotation);

        CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        int sensorOrientation = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_ORIENTATION);

        int rotationCompensation;
        if (isFrontFacing) {
            rotationCompensation = (sensorOrientation + rotationFromDisplay) % 360;
        } else {
            rotationCompensation = (sensorOrientation - rotationFromDisplay + 360) % 360;
        }
        return rotationCompensation;
    }
}