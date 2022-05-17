package com.android.classifierphotoapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.android.classifierphotoapp.databinding.FragmentFirstBinding;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;


public class FirstFragment extends Fragment {
    private FragmentFirstBinding binding;
    private LabelViewModel model;
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.INTERNET
    };
    private static final int PERMISSIONS_ALL = 9;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        model = new ViewModelProvider(requireActivity()).get(LabelViewModel.class);
        checkPermissions();

        // get labels
        if(model.labelList != null && model.labelList.size() > 0) {
            populateSpinner(model.labelList);
        } else {
            getLabelList();
        }

        binding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                model.selectedLabel = (String) adapterView.getItemAtPosition(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        binding.buttonFirst.setOnClickListener(view1 -> NavHostFragment.findNavController(FirstFragment.this)
                .navigate(R.id.action_FirstFragment_to_SecondFragment));

        binding.buttonRefresh.setOnClickListener(view1 -> getLabelList());
    }

    private void checkPermissions() {
        if(!(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.INTERNET) ==
                PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(
                    this.getActivity(),
                    PERMISSIONS,
                    PERMISSIONS_ALL
            );
        }
    }

    private void getLabelList() {
        Log.w("file", "getting label list");
        readLabelsFromJson();
    }

    private void readLabelsFromJson() {
        try {
            InputStream in = getContext().getAssets().open("labels.json");
            byte[] buf = new byte[in.available()];
            in.read(buf);
            in.close();
            String jsonString = new String(buf, StandardCharsets.UTF_8);
            List<String> list = jsonToArray(jsonString);
            populateSpinner(list);
            String msg = "read labels successfully";
            Log.w("network: ", msg);
            showSnackbar(msg);
        } catch (IOException | JSONException e) {
            String msg = "failed to read labels from file";
            Log.e("", msg);
            showSnackbar(msg);
            e.printStackTrace();
        }
    }

    private List<String> jsonToArray(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);
        List<String> list = new ArrayList<>();
        JSONArray arr = json.getJSONArray("labels");
        for(int i = 0; i < arr.length(); i++)
            list.add(arr.getString(i));
        return list;
    }


    private boolean labelReq(String host) { // returns true if successful, false otherwise
        try {
            URL url = new URL(host);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            StringBuilder buf = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                buf.append(line).append("\n");
            }
            // cleanup
            in.close();
            conn.disconnect();

            String msg = "got labels successfully";
            Log.w("network: ", msg);
            showSnackbar(msg);
            model.labelList = jsonToArray(buf.toString());
            getActivity().runOnUiThread(() -> populateSpinner(model.labelList));
            return true;

        } catch (Exception e) {
            String msg = "failed to reach host";
            Log.e("", msg);
            showSnackbar(msg);
            e.printStackTrace();
            return false;
        }
    }

    private void populateSpinner(List<String> list) {
        ArrayAdapter<String> aa = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, list);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinner.setAdapter(aa);

        if(model.selectedLabel != null) {
            int index = list.indexOf(model.selectedLabel);
            binding.spinner.setSelection(index);
        }
    }

    private void showSnackbar(String msg) {
        Snackbar.make(getActivity().findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}