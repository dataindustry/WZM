package cn.whut.edu.wzm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private final List<OverlayOptions> overlayOptionsList = new ArrayList<>();

    private final Map<String, LatLng> latLngMap = new LinkedHashMap<>();

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private String currentFolder = "";

    private final DecimalFormat df = new DecimalFormat("000");
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm");

    // 左下角经纬度
    private static final LatLng leftBottomPosition = new LatLng(29.476833, 106.423548);
    // 右上角角经纬度
    private static final LatLng rightTopPosition = new LatLng(29.682774, 106.664221);

    private final Timer timer = new Timer();

    private void saveBitmap(Bitmap bitmap, String path) {
        File file = new File(path);
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                out.flush();
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateMapStatus(BaiduMap baiduMap, LatLng position, float zoom) {
        MapStatus mapStatus = new MapStatus.Builder()
                .target(position)
                .zoom(zoom)
                .build();

        MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mapStatus);
        baiduMap.setMapStatus(mapStatusUpdate);
    }

    private void execute(BaiduMap baiduMap) {

        baiduMap.setBaiduHeatMapEnabled(true);

        for (Map.Entry<String, LatLng> entry : latLngMap.entrySet()) {

            String currentKey = entry.getKey();
            LatLng currentValue = entry.getValue();

            System.out.println("update " + currentKey);
            updateMapStatus(baiduMap, currentValue, 16);

            try {
                Thread.sleep(9000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            baiduMap.snapshot(bitmap -> {
                System.out.println("snapshot " + currentKey);
                saveBitmap(bitmap, currentFolder + currentKey + ".png");
            });

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        baiduMap.setBaiduHeatMapEnabled(false);

    }

    @SuppressLint("SetWorldWritable")
    private void schedule(Thread thread, int hourOfDay, int minute) {

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        timer.schedule(new TimerTask() {
            public void run() {

                Date date = new Date();
                String heatMapFolder = getDataDir() + "/heat_map/" + sdf.format(date) + "/";

                File file = new File(heatMapFolder);
                if (!file.exists()) {
                    if (file.mkdir()) {
                        file.setWritable(true, false);
                    }
                }

                System.out.println(sdf.format(date) + " starting ..");
                currentFolder = heatMapFolder;
                thread.start();

            }
        }, calendar.getTime());

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SDKInitializer.initialize(getApplicationContext());
        SDKInitializer.setCoordType(CoordType.BD09LL);

        setContentView(R.layout.activity_main);

        int permission = this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }

        int stepCount = 5;
        double latitudeMargin = rightTopPosition.latitude - leftBottomPosition.latitude;
        double longitudeMargin = rightTopPosition.longitude - leftBottomPosition.longitude;
        double latitudeStep = latitudeMargin / stepCount;
        double longitudeStep = longitudeMargin / stepCount;

        for (int i = 0; i <= stepCount; i++) {
            for (int j = 0; j <= stepCount; j++) {

                LatLng position = new LatLng((leftBottomPosition.latitude + latitudeStep * i),
                        (leftBottomPosition.longitude + longitudeStep * j));
                latLngMap.put(df.format(i) + "_" + df.format(j), position);

                OverlayOptions option = new MarkerOptions()
                        .position(position)
                        .icon(BitmapDescriptorFactory
                                .fromResource(R.drawable.iconmark1));
                overlayOptionsList.add(option);

            }
        }

        MapView mapView = findViewById(R.id.bmapView);
        mapView.showZoomControls(false);
        mapView.showScaleControl(false);
        BaiduMap baiduMap = mapView.getMap();
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NONE);

        List<int[]> taskList = new ArrayList<>();

        taskList.add(new int[]{14, 32});
        taskList.add(new int[]{15, 32});
        taskList.add(new int[]{16, 32});
        taskList.add(new int[]{17, 32});

        for (int[] task : taskList) {

            new Thread(() -> this.schedule(new Thread(() -> execute(baiduMap)), task[0], task[1])).start();

        }

    }
}
