package com.example.coloranalyzer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Size;
import android.widget.Toast;
import com.google.android.material.textview.MaterialTextView;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    private PreviewView previewView;
    private MaterialTextView main_IMG_first,main_IMG_second,main_IMG_third,main_IMG_forth,main_IMG_fifth;

    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Objects.requireNonNull(getSupportActionBar()).hide();

        findViews();

        startImageTimer();

        // this piece of code(everything related to the if else statement) is taken from CameraX Documentation
        if(allPermissionsGranted()){
            startCamera();
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    /**
     * This function find all the views.
     */
    private void findViews() {
        previewView = findViewById(R.id.previewView);
        main_IMG_first = findViewById(R.id.main_IMG_first);
        main_IMG_second = findViewById(R.id.main_IMG_second);
        main_IMG_third = findViewById(R.id.main_IMG_third);
        main_IMG_forth = findViewById(R.id.main_IMG_forth);
        main_IMG_fifth = findViewById(R.id.main_IMG_fifth);
    }

    /**
     * This function extract the top 5 colors out of the current image bitmap value and init the views according to the results.
     * @param bitmap - the bitmap value of the current image.
     */
    private void findAndInitTopColors(Bitmap bitmap) {
        HashMap<Integer,Integer> allColors = new HashMap<Integer, Integer>();

        initiliazeColorsAndPixelsMap(allColors,bitmap);
        clearDetails();
        initDetails(sortMapByPopularity(allColors),bitmap.getWidth()*bitmap.getHeight());
    }

    /**
     * This function initiliaze given map according to the colors and their pixels amount on a given bitmap value.
     * @param allColors - The map that needed to be initiliazed.
     * @param bitmap - The bitmap value that the image details are taken from.
     */
    private void initiliazeColorsAndPixelsMap(HashMap<Integer, Integer> allColors, Bitmap bitmap) {
        int x = bitmap.getWidth(), y = bitmap.getHeight();
        for(int i = 0; i < x; i++) {
            for(int j = 0; j < y; j++) {
                Integer p = bitmap.getPixel(i,j);
                if(!allColors.containsKey(p)) {
                    allColors.put(p, 1);
                } else {
                    int n = allColors.get(p) + 1;
                    allColors.put(p, n);
                }
            }
        }
    }

    /**
     * This function clear all details on the views.
     */
    private void clearDetails() {
        main_IMG_first.setText("");
        main_IMG_first.setBackgroundColor(Color.TRANSPARENT);
        main_IMG_second.setText("");
        main_IMG_second.setBackgroundColor(Color.TRANSPARENT);
        main_IMG_third.setText("");
        main_IMG_third.setBackgroundColor(Color.TRANSPARENT);
        main_IMG_forth.setText("");
        main_IMG_forth.setBackgroundColor(Color.TRANSPARENT);
        main_IMG_fifth.setText("");
        main_IMG_fifth.setBackgroundColor(Color.TRANSPARENT);
    }

    /**
     * This function return a descending sorted map according to its value.
     * @param allColors - The map that needs to be sorted.
     * @return - sorted map.
     */
    private Map<Integer, Integer> sortMapByPopularity(HashMap<Integer, Integer> allColors) {
        return allColors.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> -e.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> { throw new AssertionError(); },
                        LinkedHashMap::new
                ));
    }

    /**
     * This function init the views according to the map values, it writes the top 5 most popular colors rgb values and its percent out of all the colors.
     * @param sortedMap - The map that holds all of the details.
     * @param size - The pixels amount of the current image - required in order to calculate the color percent out of the total pixels amount.
     */
    private void initDetails(Map<Integer, Integer> sortedMap, int size) {
        int c = 0;
        for(Integer p : sortedMap.keySet()) {
            String percent = String.format("%.2f", (sortedMap.get(p) * 100.0f) / size);
            String text = percent + "%\n" + "R:" + Color.red(p) + " G:" + Color.blue(p) + " B:" + Color.green(p);
            // if the background is bright then set text color to black else white.
            int textColor = (Color.red(p) > 200 && Color.green(p) > 200 && Color.blue(p) > 200) ? Color.BLACK : Color.WHITE;

            if(c == 0) {
                setViewData(main_IMG_first,text,textColor,p);
            } else if(c == 1) {
                setViewData(main_IMG_second,text,textColor,p);
            } else if(c == 2) {
                setViewData(main_IMG_third,text,textColor,p);
            } else if(c == 3) {
                setViewData(main_IMG_forth,text,textColor,p);
            } else if(c == 4) {
                setViewData(main_IMG_fifth,text,textColor,p);
            } else {
                break; // if top 5 has been initialized then break the loop.
            }
            c++;
        }
    }

    /**
     * This method set the data of material text views.
     * @param materialTextView - The material text view that needs to be initialized.
     * @param text - The material text view text.
     * @param textColor - The material text view text color.
     * @param colorRGB - The material text view background color.
     */
    private void setViewData(MaterialTextView materialTextView, String text, int textColor, int colorRGB) {
        materialTextView.setText(text);
        materialTextView.setTextColor(textColor);
        materialTextView.setBackgroundColor(colorRGB);
    }

    private boolean allPermissionsGranted(){
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    public void startCamera(){
        ListenableFuture cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = (ProcessCameraProvider) cameraProviderFuture.get();
                // Set up the view finder use case to display camera preview
                Preview preview = new Preview.Builder().build();
                // Choose the camera by requiring a lens facing
                CameraSelector cameraSelector = new CameraSelector.Builder()

                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
                //Images are processed by passing an executor in which the image analysis is run
                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                //set the resolution of the view
                                .setTargetResolution(new Size(1280, 720))
                                //the executor receives the last available frame from the camera at the time that the analyze() method is called
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                // Connect the preview use case to the previewView
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                // Attach use cases to the camera with the same lifecycle owner
                Camera camera = cameraProvider.bindToLifecycle(
                        ((LifecycleOwner)this),
                        cameraSelector,
                        preview,
                        imageAnalysis);


            } catch (InterruptedException | ExecutionException e) {
                // Currently no exceptions thrown. cameraProviderFuture.get() should
                // not block since the listener is being called, so no need to

                // handle InterruptedException.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(allPermissionsGranted()){
            startCamera();
        } else{
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // ---------- ---------- TIMER ---------- ----------

    private final int DELAY = 2000;
    private enum TIMER_STATUS {
        OFF,
        RUNNING,
        PAUSE
    }
    private TIMER_STATUS timerStatus = TIMER_STATUS.OFF;
    private Timer timer;

    private void startImageTimer() {
        if (timerStatus == TIMER_STATUS.RUNNING) {
            stopTimer();
            timerStatus = TIMER_STATUS.OFF;
        } else {
            startTimer();
        }
    }

    private void tick() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // if there is valid image then perform the calculation of the top 5 most popular colors
                if(previewView.getBitmap() != null) {
                    findAndInitTopColors(previewView.getBitmap());
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (timerStatus == TIMER_STATUS.RUNNING) {
            stopTimer();
            timerStatus = TIMER_STATUS.PAUSE;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (timerStatus == TIMER_STATUS.PAUSE)
            startTimer();
    }

    private void startTimer() {
        timerStatus = TIMER_STATUS.RUNNING;
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                tick();
            }
        }, 0, DELAY);
    }

    private void stopTimer() { timer.cancel(); }
}