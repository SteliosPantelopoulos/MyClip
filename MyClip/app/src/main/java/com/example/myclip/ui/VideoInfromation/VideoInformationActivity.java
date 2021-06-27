package com.example.myclip.ui.VideoInfromation;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.example.myclip.R;
import com.example.myclip.model.User;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

import utilities.Constants;
import utilities.Triple;
import various.Video;
import various.VideoChunk;

public class VideoInformationActivity extends AppCompatActivity {
    private User user = new User();

    public static final String  CHANNEL_NAME = "channelName",
                                DATE_CREATED = "dateCreated",
                                LENGTH = "length",
                                FRAMERATE = "framerate",
                                FRAME_WIDTH = "frameWidth",
                                FRAME_HEIGHT = "frameHeight",
                                VIDEO_PATH = "videoPath";

    private String videoName, channelName, dateCreated, videoPath;
    private float length;
    private int framerate, frameWidth, frameHeight;
    private ArrayList<String> associatedHashtags = new ArrayList<>();
    private byte[] videoBytes;

    private EditText edtName, edtHashtags;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_information);

        //METADATA OF VIDEO
        Intent intent = getIntent();
        channelName = intent.getStringExtra(CHANNEL_NAME);
        dateCreated = intent.getStringExtra(DATE_CREATED);
        length = intent.getFloatExtra(LENGTH, 0);
        framerate = intent.getIntExtra(FRAMERATE, 0);
        frameHeight = intent.getIntExtra(FRAME_HEIGHT, 0);
        frameWidth = intent.getIntExtra(FRAME_WIDTH, 0);
        videoPath = intent.getStringExtra(VIDEO_PATH);
        videoBytes = convertVideoToBytes(videoPath);

        //NAME AND HASHTAGS OF VIDEO
        edtName = findViewById(R.id.video_infromation_video_name);
        edtHashtags = findViewById(R.id.video_infromation_video_hashtags);
        button = findViewById(R.id.video_infromation_upload_button);

        button.setOnClickListener(v -> {
            videoName = edtName.getText().toString();
            extractAssociatedHashtags(edtHashtags.getText().toString());

            new AddVideoToChannelBroker().execute();
            new AddHashtagsToTopicBroker().execute();
            onMainActivity();
        });
    }

    private void extractAssociatedHashtags(String stringOfHashtags) {
        /**
         * params: input of hashtags
         * extract every hashtag from input and add it in a list
         */

        String hashtag = null;
        for(int i=0; i < stringOfHashtags.length(); i++){
            if( stringOfHashtags.charAt(i) == '#' ){
                if( i != 0){
                    associatedHashtags.add(hashtag);
                }
                hashtag = "#";
            }else{
                hashtag += stringOfHashtags.charAt(i);
                if( i == stringOfHashtags.length() - 1){
                    associatedHashtags.add(hashtag);
                }
            }
        }
    }

    private byte[] convertVideoToBytes(String videoPath) {
        /**
         * params: video path
         * find video in path and convert it in byte array
         */
        byte[] videoBytes = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            File file = new File(videoPath);
            FileInputStream fis = new FileInputStream(file);
            byte[] buf = new byte[1024];
            int n;
            while (-1 != (n = fis.read(buf)))
                baos.write(buf, 0, n);

            videoBytes = baos.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return videoBytes;
    }

    private void onMainActivity() {
        /**
         * called when upload of video is finished in order to return to main activity
         */
        this.finish();
    }

    private class AddVideoToChannelBroker extends AsyncTask<Void, Void, Void>{

        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Socket requestSocket;

        private String randomBrokerIp = Constants.SERVER_IP;
        private int randomBrokerPort;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            user.getPublisher().postVideo(new Video(videoName, channelName, dateCreated, length, framerate, frameWidth, frameHeight, associatedHashtags, videoBytes));
            Random rn = new Random();
            int answer = rn.nextInt(3) + 1;
            if(answer == 1){
                randomBrokerPort = 1500;
            }else if(answer == 2){
                randomBrokerPort = 2000;
            }else{
                randomBrokerPort = 2500;
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {

            //make a socket to find correct broker for this hashtag
            try {
                requestSocket = new Socket(randomBrokerIp, randomBrokerPort);
                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());

                out.writeUTF("publisher");                              //inform for being publisher
                out.flush();
                out.writeUTF("notify");                                 //inform for action
                out.flush();
                out.writeUTF(user.getPublisherId());                        //inform about publisher id
                out.flush();
                out.writeUTF(user.getChannel().getChannelName());           //inform about channelName
                out.flush();

                Triple infoOfCorrectBroker = (Triple) in.readObject();
                infoOfCorrectBroker.setIpAddress(Constants.SERVER_IP);

                in.close();
                out.close();
                requestSocket.close();

                //make a socket to send hashtag to correct broker
                requestSocket = new Socket(infoOfCorrectBroker.getIpAddress(), infoOfCorrectBroker.getPortNumber());
                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());

                out.writeUTF("publisher");                      //inform for being publisher
                out.flush();
                out.writeUTF("addVideo");                       //infrom about action
                out.flush();
                out.writeUTF(user.getPublisherId());                //inform about publisher id
                out.flush();

                out.writeUTF(videoName);                            //inform about video name
                out.flush();

                Video video = user.getChannel().getVideo(videoName);

                out.writeUTF(video.getChannelName());           //send channelName
                out.flush();
                out.writeUTF(video.getDateCreated());           //send dateCreated
                out.flush();
                out.writeFloat(video.getLength());              //send length
                out.flush();
                out.writeInt(video.getFramerate());             //send framerate
                out.flush();
                out.writeInt(video.getFrameWidth());            //send frameWidth
                out.flush();
                out.writeInt(video.getFrameHeight());           //send frameHeight
                out.flush();
                out.writeObject(video.getAssociatedHashTags()); //send hashtags
                out.flush();

                out.writeInt(video.getChunks().size());
                out.flush();
                for(VideoChunk chunk : video.getChunks()) {
                    out.writeObject(chunk);                     //send videoChunks
                    out.flush();
                }

                in.close();
                out.close();
                requestSocket.close();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }


            return null;
        }
    }

    private class AddHashtagsToTopicBroker extends AsyncTask<Void, Void, Void > {
        /**
         * called when publisher adds a new video
         * add video to user channel and send new video associated hashtags in responsible broker
         * 1. add video
         * 2. for every hashtag:
         *                      connect to random broker in order to get info about correct broker
         *                      connect to responsible broker and inform about new hashtag (send hashtag and video)
         */

        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Socket requestSocket;

        private String randomBrokerIp = Constants.SERVER_IP;
        private int randomBrokerPort;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Random rn = new Random();
            int answer = rn.nextInt(3) + 1;
            if(answer == 1){
                randomBrokerPort = 1500;
            }else if(answer == 2){
                randomBrokerPort = 2000;
            }else{
                randomBrokerPort = 2500;
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for(String hashtag: associatedHashtags){
                try {
                    //make a socket to find correct broker for this hashtag
                    requestSocket = new Socket(randomBrokerIp, randomBrokerPort);
                    out = new ObjectOutputStream(requestSocket.getOutputStream());
                    in = new ObjectInputStream(requestSocket.getInputStream());

                    out.writeUTF("publisher");                              //inform for being publisher
                    out.flush();
                    out.writeUTF("notify");                                 //inform for action
                    out.flush();
                    out.writeUTF(user.getPublisherId());                        //inform about publisher id
                    out.flush();
                    out.writeUTF(hashtag);                                      //inform about topic
                    out.flush();

                    Triple infoOfCorrectBroker = (Triple) in.readObject();
                    infoOfCorrectBroker.setIpAddress(Constants.SERVER_IP);

                    in.close();
                    out.close();
                    requestSocket.close();

                    //make a socket to send hashtag to correct broker
                    requestSocket = new Socket(infoOfCorrectBroker.getIpAddress(), infoOfCorrectBroker.getPortNumber());
                    out = new ObjectOutputStream(requestSocket.getOutputStream());
                    in = new ObjectInputStream(requestSocket.getInputStream());

                    out.writeUTF("publisher");                      //inform for being publisher
                    out.flush();
                    out.writeUTF("addhashtag");                     //inform for action
                    out.flush();
                    out.writeUTF(user.getPublisherId());                //inform about publisher id
                    out.flush();
                    out.writeUTF(user.getChannel().getChannelName());   //inform about channel name
                    out.flush();
                    out.writeUTF(videoName);                            //inform about video name
                    out.flush();
                    out.writeUTF(hashtag);                              //inform about hashtag
                    out.flush();

                    //send video
                    Video video = user.getChannel().getVideo(videoName);

                    out.writeUTF(video.getChannelName());           //send channelName
                    out.flush();
                    out.writeUTF(video.getDateCreated());           //send dateCreated
                    out.flush();
                    out.writeFloat(video.getLength());              //send length
                    out.flush();
                    out.writeInt(video.getFramerate());             //send framerate
                    out.flush();
                    out.writeInt(video.getFrameWidth());            //send frameWidth
                    out.flush();
                    out.writeInt(video.getFrameHeight());           //send frameHeight
                    out.flush();
                    out.writeObject(video.getAssociatedHashTags()); //send hashtags
                    out.flush();

                    out.writeInt(video.getChunks().size());
                    out.flush();
                    for(VideoChunk chunk : video.getChunks()) {
                        out.writeObject(chunk);                     //send videoChunks
                        out.flush();
                    }



                    in.close();
                    out.close();
                    requestSocket.close();

                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}