package com.android.classifierphotoapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.classifierphotoapp.databinding.FragmentSecondBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

public class SecondFragment extends Fragment {
    private FragmentSecondBinding binding;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private Camera camera;
    private LabelViewModel viewModel;

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int PERMISSIONS_ALL = 10;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        previewView = binding.viewFinder;
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(hasCameraPermission() & hasWritePermission())
            enableCamera();
        else
            requestPermissions();


        viewModel = new ViewModelProvider(requireActivity()).get(LabelViewModel.class);
        binding.textView.setText(viewModel.selectedLabel);

        binding.buttonSecond.setOnClickListener(view1 -> NavHostFragment.findNavController(SecondFragment.this)
                .navigate(R.id.action_SecondFragment_to_FirstFragment));

        binding.imageCaptureButton.setOnClickListener(view1 -> takePicture());
    }

    private boolean hasCameraPermission() {
        boolean r = ContextCompat.checkSelfPermission(this.getActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        Log.println(Log.WARN, "", "perm: " + r);
        return r;
    }

    private boolean hasWritePermission() {
        boolean r = ContextCompat.checkSelfPermission(this.getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        Log.println(Log.WARN, "", "perm: " + r);
        return r;
    }

    private void requestPermissions() {
        Log.println(Log.WARN, "", "requesting perms");
        ActivityCompat.requestPermissions(
                this.getActivity(),
                PERMISSIONS,
                PERMISSIONS_ALL
        );
    }

    private void enableCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this.getActivity());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (Exception e) {
                Log.e("", e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this.getActivity()));
    }

    private void bindPreview(@NonNull ProcessCameraProvider processCameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        imageCapture = new ImageCapture.Builder()
                .setTargetRotation(this.getActivity().getWindowManager().getDefaultDisplay().getRotation())
                .build();
        camera = processCameraProvider.bindToLifecycle(this, cameraSelector, imageCapture, preview);
    }

    private void takePicture() {
        ContentValues fcv = new ContentValues();
        fcv.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + viewModel.selectedLabel);
        fcv.put(MediaStore.Images.Media.IS_PENDING, true);
        Uri folder = getContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, fcv);
        if(folder == null) {
            Log.e("FOLDER: ", "failed to create folder");
            return;
        }
        else {
            Log.w("FOLDER: ", "created folder: " + folder);
        }


        String filename = viewModel.selectedLabel + "_" + (System.currentTimeMillis() / 1000);

        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        cv.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        cv.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + viewModel.selectedLabel);
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                this.getActivity().getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                cv
        ).build();
        Log.println(Log.INFO, "","path: " + outputFileOptions);
        imageCapture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(this.getContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        String msg = "saved as " + filename;
                        Log.println(Log.INFO, "", msg);
                        Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        String msg = "err: " + exception.getMessage();
                        Log.e("", msg);
                        Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}