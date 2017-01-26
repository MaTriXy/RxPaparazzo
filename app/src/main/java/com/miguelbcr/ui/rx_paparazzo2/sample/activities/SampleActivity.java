package com.miguelbcr.ui.rx_paparazzo2.sample.activities;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.miguelbcr.ui.rx_paparazzo2.RxPaparazzo;
import com.miguelbcr.ui.rx_paparazzo2.entities.FileData;
import com.miguelbcr.ui.rx_paparazzo2.entities.Response;
import com.miguelbcr.ui.rx_paparazzo2.entities.size.CustomMaxSize;
import com.miguelbcr.ui.rx_paparazzo2.entities.size.OriginalSize;
import com.miguelbcr.ui.rx_paparazzo2.entities.size.Size;
import com.miguelbcr.ui.rx_paparazzo2.entities.size.SmallSize;
import com.miguelbcr.ui.rx_paparazzo2.sample.R;
import com.miguelbcr.ui.rx_paparazzo2.sample.adapters.ImagesAdapter;
import com.squareup.picasso.Picasso;
import com.yalantis.ucrop.UCrop;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SampleActivity extends AppCompatActivity implements Testable {
    private static final String STATE_FILES = "FILES";

    private Toolbar toolbar;
    private ImageView imageView;
    private TextView filenameView;
    private RecyclerView recyclerView;
    private ArrayList<FileData> fileDataList;
    private Size size;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_layout);
        configureToolbar();

        fileDataList = new ArrayList<>();
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_FILES)) {
                List files = (List) savedInstanceState.getSerializable(STATE_FILES);
                fileDataList.addAll(files);
            }
        }

        initViews();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(STATE_FILES, fileDataList);
    }

    private void configureToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);
    }

    private void initViews() {
        imageView = (ImageView) findViewById(R.id.iv_image);
        filenameView = (TextView) findViewById(R.id.iv_filename);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView = (RecyclerView) findViewById(R.id.rv_images);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

        findViewById(R.id.fab_camera).setOnClickListener(v -> captureImage());
        findViewById(R.id.fab_camera_crop).setOnClickListener(v -> captureImageWithCrop());
        findViewById(R.id.fab_pickup_image).setOnClickListener(v -> pickupImage());
        findViewById(R.id.fab_pickup_images).setOnClickListener(v -> pickupImages());
        findViewById(R.id.fab_pickup_file).setOnClickListener(v -> pickupFile());
        findViewById(R.id.fab_pickup_files).setOnClickListener(v -> pickupFiles());

        loadImages();
    }

    private void captureImage() {
        Observable<Response<SampleActivity, FileData>> takeOnePhoto = RxPaparazzo.single(SampleActivity.this)
                .size(new CustomMaxSize(512))
                .usingCamera();

        processSingle(takeOnePhoto);
    }

    private void captureImageWithCrop() {
        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(ContextCompat.getColor(SampleActivity.this, R.color.colorAccent));
        options.setToolbarTitle("Cropping single photo");

        Observable<Response<SampleActivity, FileData>> takePhotoAndCrop = RxPaparazzo.single(SampleActivity.this)
                .size(new OriginalSize())
                .crop(options)
                .usingCamera();

        processSingle(takePhotoAndCrop);
    }

    private void pickupImage() {
        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(ContextCompat.getColor(SampleActivity.this, R.color.colorPrimaryDark));
        options.setToolbarTitle("Cropping single image");

        Observable<Response<SampleActivity, FileData>> pickUsingGallery = pickSingle(options, new CustomMaxSize(500))
                .usingGallery();

        processSingle(pickUsingGallery);
    }

    private void pickupImages() {
        Observable<Response<SampleActivity, List<FileData>>> pickMultiple = pickMultiple(new SmallSize())
                .usingGallery();

        processMultiple(pickMultiple);
    }

    private void pickupFile() {
        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(ContextCompat.getColor(SampleActivity.this, R.color.colorPrimaryDark));
        options.setToolbarTitle("Cropping single file");

        Observable<Response<SampleActivity, FileData>> pickUsingGallery = pickSingle(options, new CustomMaxSize(500))
                .usingFiles();

        processSingle(pickUsingGallery);
    }

    private void pickupFiles() {
        Size size = new SmallSize();

        Observable<Response<SampleActivity, List<FileData>>> pickMultiple = pickMultiple(size)
                .usingFiles();

        processMultiple(pickMultiple);
    }

    private void processSingle(Observable<Response<SampleActivity, FileData>> pickUsingGallery) {
        pickUsingGallery
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (PickerUtil.checkResultCode(SampleActivity.this, response.resultCode())) {
                        response.targetUI().loadImage(response.data());
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    Toast.makeText(getApplicationContext(), "ERROR " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private RxPaparazzo.SingleSelectionBuilder<SampleActivity> pickSingle(UCrop.Options options, Size size) {
        this.size = size;

        return RxPaparazzo.single(this)
                .useInternalStorage()
                .crop(options)
                .size(size);
    }

    private Disposable processMultiple(Observable<Response<SampleActivity, List<FileData>>> pickMultiple) {
        return pickMultiple
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (PickerUtil.checkResultCode(SampleActivity.this, response.resultCode())) {
                        if (response.data().size() == 1) {
                            response.targetUI().loadImage(response.data().get(0));
                        } else {
                            response.targetUI().loadImages(response.data());
                        }
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    Toast.makeText(getApplicationContext(), "ERROR " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private RxPaparazzo.MultipleSelectionBuilder<SampleActivity> pickMultiple(Size size) {
        this.size = size;

        return RxPaparazzo.multiple(this)
                .useInternalStorage()
                .crop()
                .size(size);
    }

    private void loadImage(FileData fileData) {
        this.fileDataList = new ArrayList<>(fileDataList);
        fileDataList.add(fileData);

        imageView.setVisibility(View.VISIBLE);
        imageView.setImageDrawable(null);

        recyclerView.setVisibility(View.GONE);
        recyclerView.setAdapter(null);

        filenameView.setVisibility(View.VISIBLE);
        filenameView.setText(fileData.describe());

        Picasso.with(getApplicationContext()).setLoggingEnabled(true);
        Picasso.with(getApplicationContext()).invalidate(fileData.getFile());
        Picasso.with(getApplicationContext()).load(fileData.getFile())
                .error(R.drawable.ic_description_black_48px)
                .into(imageView);
    }

    private void loadImages(List<FileData> fileDataList) {
        this.fileDataList = new ArrayList<>(fileDataList);

        loadImages();
    }

    private void loadImages() {
        imageView.setVisibility(View.GONE);
        imageView.setImageDrawable(null);

        filenameView.setVisibility(View.GONE);

        if (fileDataList == null || fileDataList.isEmpty()) {
            return;
        }

        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setAdapter(new ImagesAdapter(fileDataList));
    }

    @Override
    public List<String> getFilePaths() {
        List<String> filesPaths = new ArrayList<>();
        for (FileData fileData : fileDataList) {
            filesPaths.add(fileData.getFile().getAbsolutePath());
        }
        return filesPaths;
    }

    @Override
    public Size getSize() {
        return size;
    }
}
