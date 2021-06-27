package com.example.myclip.ui.TableLayout;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.myclip.R;
import com.example.myclip.model.User;

public class UploadFragment extends Fragment {

    private static final int PICK_VIDEO = 1;
    private final int VIDEO_WITH_CAMERA = 1001;

    private Button buttonRecord, buttonSelect;
    private String videoPath, channelName, dateCreated;
    private float length;
    private int framerate, frameWidth, frameHeight;


    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_upload, container, false);

        buttonRecord = rootView.findViewById(R.id.upload_fragment_record);
        buttonSelect = rootView.findViewById(R.id.upload_fragment_select);

        buttonRecord.setOnClickListener(v -> {
            Intent cameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 20);
            startActivityForResult(cameraIntent, VIDEO_WITH_CAMERA);

        });

        buttonSelect.setOnClickListener(v -> {
            Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
            getIntent.setType("video/*");

            Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            pickIntent.setType("video/*");

            Intent chooserIntent = Intent.createChooser(getIntent, "Select Video");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

            startActivityForResult(chooserIntent, PICK_VIDEO);
        });

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK &&  (requestCode == VIDEO_WITH_CAMERA || requestCode == PICK_VIDEO)) {
            //Get mp4 info
            Uri uri = data.getData();
            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            metaRetriever.setDataSource(getContext(), uri);

            Cursor cursor = null;
            try {
                String[] proj = {MediaStore.Images.Media.DATA};
                cursor = getContext().getContentResolver().query(data.getData(), proj, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                videoPath = cursor.getString(column_index);
            } catch (Exception e) {
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            channelName = new User().getChannel().getChannelName();
            dateCreated = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
            length = Float.parseFloat(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            framerate = Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT));
            frameWidth = Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            frameHeight = Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            metaRetriever.release();

            ((MainActivity)getContext()).onVideoInformation(channelName, dateCreated, length, framerate, frameWidth, frameHeight, videoPath);
        }
    }
}
