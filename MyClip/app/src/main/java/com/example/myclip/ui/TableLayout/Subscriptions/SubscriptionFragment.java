package com.example.myclip.ui.TableLayout.Subscriptions;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.myclip.R;
import com.example.myclip.model.Consumer;
import com.example.myclip.model.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import utilities.Constants;
import utilities.HashFunction;
import utilities.Triple;
import various.Video;
import various.VideoChunk;

public class SubscriptionFragment extends Fragment {
    MySubscriptionRecyclerViewAdapter adapter;

    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;

    private static HashMap<String, Video> allVideosReceived = new HashMap<String, Video>();
    private ArrayList<Video> videosToProject = new ArrayList<>();     // all videos that contain topic that user is subscribed
    private Consumer consumer = new User().getConsumer();

    public SubscriptionFragment() {
    }

    @SuppressWarnings("unused")
    public static SubscriptionFragment newInstance(int columnCount) {
        SubscriptionFragment fragment = new SubscriptionFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subscription_list, container, false);

        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            findVideosSubscribed();
            adapter = new MySubscriptionRecyclerViewAdapter(videosToProject);
            recyclerView.setAdapter(adapter);
        }
        return view;
    }

    private void findVideosSubscribed() {
        /**
         * for every topic subscribed get its video(s)
         * called when user open subscriptions fragment
         */
        videosToProject.clear();
        for(String topic: consumer.getSubscriptions()){
            new getVideo().execute(topic);
        }
    }

    private class getVideo extends AsyncTask<String, Void, Void> {
        /**
         * 1. open connection with broker
         * 2. request video(s) about a topic
         * 3. receive videos and save them in videos arrayList
         */

        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        private int numberOfVideos;

        private String videoName;
        private String channelName;
        private String dateCreated;
        private float length;
        private int framerate;
        private int frameWidth;
        private int frameHeight;
        private int numberOfChunks;
        private ArrayList<String> hashtags = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... strings) {
            String topic = strings[0];
            int hashedTopic = HashFunction.hashFunction(topic);

            Triple correctBroker = consumer.findResponsibleBroker(hashedTopic);
            correctBroker.setIpAddress(Constants.SERVER_IP);                        //NOT REAL IP

            try {
                socket = new Socket(correctBroker.getIpAddress(), correctBroker.getPortNumber());
                in = new ObjectInputStream(socket.getInputStream());
                out = new ObjectOutputStream(socket.getOutputStream());

                out.writeUTF("consumer");
                out.flush();
                out.writeUTF("false");
                out.flush();
                out.writeUTF(new User().getConsumerId());
                out.flush();
                out.writeUTF("getAllVideos");
                out.flush();
                out.writeUTF(new User().getChannel().getChannelName());
                out.flush();
                out.writeUTF(topic);
                out.flush();

                numberOfVideos = in.readInt();
                for(int video = 0; video < numberOfVideos; video++){
                    videoName = in.readUTF();
                    channelName = in.readUTF();
                    dateCreated = in.readUTF();
                    length = in.readFloat();
                    framerate = in.readInt();
                    frameWidth = in.readInt();
                    frameHeight = in.readInt();
                    hashtags = (ArrayList<String>) in.readObject();

                    numberOfChunks = in.readInt();
                    ArrayList<VideoChunk> videoChunks = new ArrayList();
                    for(int i=0; i<numberOfChunks; i++){
                        videoChunks.add((VideoChunk) in.readObject());
                    }

                    if(!allVideosReceived.containsKey(videoName)){
                        Video newVideo = new Video(videoName, channelName, dateCreated, length, framerate, frameWidth, frameHeight, hashtags, consumer.mergeVideoChunks(videoChunks));
                        allVideosReceived.put(videoName, newVideo);
                        videosToProject.add(newVideo);
                    }else{
                        videosToProject.add(allVideosReceived.get(videoName));
                    }

                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            adapter.notifyDataSetChanged();
        }
    }
}