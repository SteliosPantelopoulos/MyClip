package com.example.myclip.ui.TableLayout.Subscriptions;

import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.VideoView;

import com.example.myclip.databinding.FragmentSubscriptionBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import various.Video;
import various.VideoChunk;

public class MySubscriptionRecyclerViewAdapter extends RecyclerView.Adapter<MySubscriptionRecyclerViewAdapter.ViewHolder> {

    private final ArrayList<Video> mVideos;

    public MySubscriptionRecyclerViewAdapter(ArrayList<Video> items) {
        mVideos = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolder(FragmentSubscriptionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mVideos.get(position);
        holder.mChannelName.setText(mVideos.get(position).getChannelName());
        String hashtags = "";
        for(String hashtag : mVideos.get(position).getAssociatedHashTags()){
            hashtags += hashtag;
        }
        holder.mHashtags.setText(hashtags);


        ArrayList<VideoChunk> videoChunks = holder.mItem.getChunks();

        int numberOfBytes = 0;
        for(VideoChunk chunk: videoChunks){
            numberOfBytes += chunk.getData().length;
        }

        byte[] data = new byte[numberOfBytes];
        int counter = 0;
        for(VideoChunk chunk: videoChunks){
            for(int i=0; i<chunk.getData().length; i++){
                data[counter + i] = chunk.getData()[i];
            }
            counter += chunk.getData().length;
        }


        try {
            String filename="/rec"+ holder.mItem.getDateCreated().replace(".", "_") +".mp4";
            String root = Environment.getExternalStorageDirectory().toString();
            File myDir  = new File(root+"/Download");
            if (!myDir.exists())
                myDir.mkdir();
            File file = new File (myDir, filename);

            FileOutputStream out = new FileOutputStream(file);
            out.write(data);
            out.close();

            Uri video = Uri.parse(file.getPath());
            holder.mVideo.setVideoURI(video);
            holder.mVideo.start();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public int getItemCount() {
        return mVideos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public Video mItem;
        public TextView mChannelName, mHashtags;
        public VideoView mVideo;

        public ViewHolder(FragmentSubscriptionBinding binding) {
            super(binding.getRoot());
            mChannelName = binding.subscriptionFragmentChannelName;
            mHashtags = binding.subscriptionFragmentHashtags;
            mVideo = binding.subscriptionFragmentVideo;
        }

    }
}