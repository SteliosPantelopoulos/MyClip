package com.example.myclip.ui.TableLayout;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.example.myclip.R;
import com.example.myclip.ui.TableLayout.Subscriptions.SubscriptionFragment;
import com.example.myclip.ui.VideoInfromation.VideoInformationActivity;
import com.example.myclip.model.Consumer;
import com.example.myclip.model.User;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

import utilities.Constants;
import utilities.Triple;

public class MainActivity extends AppCompatActivity {

    public static final String  USER_ID = "userId",
                                CHANNEL_NAME = "channelName",
                                DATE_CREATED = "dateCreated",
                                LENGTH = "length",
                                FRAMERATE = "framerate",
                                FRAME_WIDTH = "frameWidth",
                                FRAME_HEIGHT = "frameHeight",
                                VIDEO_PATH = "videoPath";

    private String userId, channelName;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private User user;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get user info from login activity
        Intent intent = getIntent();
        userId = intent.getStringExtra(USER_ID);
        channelName = intent.getStringExtra(CHANNEL_NAME);

        //make initial actions
        connectionInitialActions();

        //initialise activity elements
        tabLayout = findViewById(R.id.main_activity_tab_layout);
        viewPager = findViewById(R.id.main_activity_view_pager);

        tabLayout.setupWithViewPager(viewPager);

        VPadapter vPadapter = new VPadapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        vPadapter.addFragment(new SubscriptionFragment(), "Subscriptions");
        vPadapter.addFragment(new SearchFragment(), "Search");
        vPadapter.addFragment(new UploadFragment(), "Upload");
        viewPager.setAdapter(vPadapter);
    }

    private void connectionInitialActions() {
        /**
         * called when app is lanched for the first time after login activity
         * 1. creates new user
         * 2. open server thread for publisher
         * 3. make first contact with broker for consumer and publisher
         */

        user = new User(userId, channelName);

        new SendInitialInfoPublisher().execute();
        new FirstContactConsumer().execute();
    }

    private class SendInitialInfoPublisher extends AsyncTask<String, Void, Void >{
        /**
         * send publisher's information to broker
         * called when publisher is created
         * 1. connect with random broker
         * 2. find responsible server for publisher's channel name
         * 3. connect with correct broker
         * 4. send publisher's info
         */

        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Socket requestSocket;
        private String ipOfRandomBroker;
        private int portOfRandomBroker;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Random rn = new Random();
            int answer = rn.nextInt(3) + 1;
            if(answer == 1){
                ipOfRandomBroker = "10.0.2.2";
                portOfRandomBroker = 1500;
            }else if(answer == 2){
                ipOfRandomBroker = "10.0.2.2";
                portOfRandomBroker = 2000;
            }else{
                ipOfRandomBroker = "10.0.2.2";
                portOfRandomBroker = 2500;
            }
        }

        @Override
        protected Void doInBackground(String... strings) {

            try {
                //make a socket to connect to random broker and get correct broker
                requestSocket = new Socket(ipOfRandomBroker, portOfRandomBroker);
                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());

                out.writeUTF("publisher");                              //inform for being publisher
                out.flush();
                out.writeUTF("notify");                                 //inform for action
                out.flush();
                out.writeUTF(user.getPublisherId());                        //inform about publisher id
                out.flush();
                out.writeUTF(user.getChannel().getChannelName());           //inform about topic
                out.flush();

                Triple infoOfCorrectBroker = (Triple) in.readObject();
                infoOfCorrectBroker.setIpAddress(Constants.SERVER_IP);      //NOT REAL IP

                try {
                    in.close();
                    out.close();
                    requestSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //make a socket to connect to correct broker and send your information
                requestSocket = new Socket(infoOfCorrectBroker.getIpAddress(), infoOfCorrectBroker.getPortNumber());
                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());

                out.writeUTF("publisher");                      //inform for being publisher
                out.flush();
                out.writeUTF("addchannel");                     //inform for action
                out.flush();
                out.writeUTF(user.getPublisherId());                //inform about publisher id
                out.flush();
                out.writeUTF(user.getChannel().getChannelName());   //inform about channel name
                out.flush();

                try {
                    in.close();
                    out.close();
                    requestSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class FirstContactConsumer extends AsyncTask<Void, Void, Void>{
        /**
         * get responsible channels/hashtags for every broker
         * called when consumer is created
         * 1. connect with a random broker
         * 2. send consumer ino
         * 3. receive brokers list
         */

        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Socket requestSocket;

        private String ipOfRandomBroker;
        private int portOfRandomBroker;
        private int numberOfBrokers;

        private Consumer consumer = user.getConsumer();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Random rn = new Random();
            int answer = rn.nextInt(3) + 1;
            if(answer == 1){
                ipOfRandomBroker = "10.0.2.2";
                portOfRandomBroker = 1500;
            }else if(answer == 2){
                ipOfRandomBroker = "10.0.2.2";
                portOfRandomBroker = 2000;
            }else{
                ipOfRandomBroker = "10.0.2.2";
                portOfRandomBroker = 2500;
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                //make a socket to connect to random broker
                requestSocket = new Socket(ipOfRandomBroker, portOfRandomBroker);
                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());

                out.writeUTF("consumer");
                out.flush();
                out.writeUTF("true");
                out.flush();
                out.writeUTF(user.getConsumerId());
                out.flush();

                numberOfBrokers = in.readInt();
                for(int i=0; i<numberOfBrokers; i++){

                    //broker's info and hashcode
                    String brokerId = in.readUTF();
                    Triple brokerInfo = (Triple) in.readObject();
                    brokerInfo.setIpAddress(Constants.SERVER_IP);       //NOT REAL IP
                    consumer.addBrokersInfo(brokerId, brokerInfo);

                    //broker's channels responsible
                    int numberOfChannelsResponsible = in.readInt();
                    ArrayList<String> channels = new ArrayList();
                    for(int j=0; j<numberOfChannelsResponsible; j++){
                        channels.add(in.readUTF());
                    }
                    consumer.addBrokersChannelsResponsible(brokerId, channels);

                    //broker's hashtags responsible
                    int numberOfHahstagsResponsible = in.readInt();
                    ArrayList<String> hashtags = new ArrayList();
                    for(int j=0; j<numberOfHahstagsResponsible; j++){
                        hashtags.add(in.readUTF());
                    }
                    consumer.addBrokersHashtagsResponsible(brokerId, hashtags);
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                    out.close();
                    requestSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    public void onVideoInformation(String channelName, String dateCreated, float length, int framerate, int frameWidth, int frameHeight, String videoPath){
        /**
         * starts new intent in VideoInformationActivity
         */

        Intent videoInformation = new Intent(this, VideoInformationActivity.class);

        videoInformation.putExtra(CHANNEL_NAME, channelName);
        videoInformation.putExtra(DATE_CREATED, dateCreated);
        videoInformation.putExtra(LENGTH, length);
        videoInformation.putExtra(FRAMERATE, framerate);
        videoInformation.putExtra(FRAME_WIDTH, frameWidth);
        videoInformation.putExtra(FRAME_HEIGHT, frameHeight);
        videoInformation.putExtra(VIDEO_PATH, videoPath);

        startActivity(videoInformation);
    }

}