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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    private PreviewView previewView;
    private MaterialTextView main_IMG_first,main_IMG_second,main_IMG_third,main_IMG_forth,main_IMG_fifth;

    private final int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    private Bitmap bitmap;
    private HashMap<Integer,Integer> allColors = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Objects.requireNonNull(getSupportActionBar()).hide();

        findViews();

        startCameraWorker();

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
     * This function initialize given map according to the colors and their pixels amount on the current bitmap value.
     */
    private void initiliazeColorsAndPixelsMap() {
        if(bitmap == null)
            return;
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
     * @param hm - The map that needs to be sorted.
     * @return - sorted map.
     */
    private HashMap<Integer, Integer> sortMapByPopularity(HashMap<Integer, Integer> hm) {
        return hm.entrySet().stream()
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
     */
    private void initDetails(Map<Integer, Integer> sortedMap) {
        int c = 0, size = bitmap.getWidth()*bitmap.getHeight();
        for(Integer p : sortedMap.keySet()) {
            String percent = String.format("%.2f", (sortedMap.get(p) * 100.0f) / size);
            String text = percent + "%\n" + "R:" + Color.red(p) + " G:" + Color.green(p) + " B:" + Color.blue(p);
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

    // ----------- ----------- ----------- WORKER ----------- ----------- -----------
    private enum WORKER_STATUS {
        OFF,
        RUNNING,
        PAUSE
    }
    private WORKER_STATUS workerStatus = WORKER_STATUS.OFF;
    private ScheduledExecutorService worker;

    private void startCameraWorker() {
        if (workerStatus == WORKER_STATUS.RUNNING) {
            stopWorker();
            workerStatus = WORKER_STATUS.OFF;
        } else {
            startWorker();
        }
    }

    private void startWorker() {
        if (worker == null)
            worker = Executors.newSingleThreadScheduledExecutor();

        worker.execute(new Runnable() {
            @Override
            public void run() {
                performTasks();
                if (worker != null) {
                    worker.schedule(this, 50, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    /**
     * This method perform heavy task of calculations and sort on the thread we created,
     * and then call another function to update the ui according to the calculations performed on this task.
     */
    private void performTasks() {
        if(allColors == null)
            allColors = new HashMap<>();
        else
            allColors.clear();

        // perform heavy task on the thread we created
        initiliazeColorsAndPixelsMap();
        Map<Integer,Integer> sortedMap = sortMapByPopularity(allColors);

        // perform ui task on the main thread
        performUiTask(sortedMap);
    }

    /**
     * This method perform the ui tasks on the main thread according to the given map values.
     * @param sortedMap - The map that according to its details the ui needs to be updated.
     */
    private void performUiTask(Map<Integer, Integer> sortedMap) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(bitmap != null) {
                    clearDetails();
                    initDetails(sortedMap);
                }
                bitmap = previewView.getBitmap(); // get next image
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (workerStatus == WORKER_STATUS.RUNNING) {
            stopWorker();
            workerStatus = WORKER_STATUS.PAUSE;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (workerStatus == WORKER_STATUS.PAUSE) {
            startWorker();
        }
    }

    private void stopWorker() {
        worker.shutdown();
        worker = null;
    }
}