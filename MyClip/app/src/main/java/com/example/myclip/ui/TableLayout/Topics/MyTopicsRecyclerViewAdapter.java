package com.example.myclip.ui.TableLayout.Topics;

import androidx.recyclerview.widget.RecyclerView;

import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.example.myclip.databinding.FragmentTopicBinding;
import com.example.myclip.model.Consumer;
import com.example.myclip.model.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

import utilities.Constants;
import utilities.HashFunction;
import utilities.Triple;

public class MyTopicsRecyclerViewAdapter extends RecyclerView.Adapter<MyTopicsRecyclerViewAdapter.ViewHolder> {

    private final List<String> mTopics;


    private Consumer consumer = new User().getConsumer();

    public MyTopicsRecyclerViewAdapter(List<String> items) {
        mTopics = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolder(FragmentTopicBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mTopic = mTopics.get(position);
        holder.mTopicNameTextView.setText(holder.mTopic);


        holder.mSubscribeButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (holder.mSubscribeButton.isChecked())
                {
                    holder.mSubscribeButton.setChecked(true);
                    new Subscribe().execute(holder.mTopic);
                }
                else
                {
                    holder.mSubscribeButton.setChecked(false);
                    new Unsubscribe().execute(holder.mTopic);
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return mTopics.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public String mTopic;
        public TextView mTopicNameTextView;
        public CheckBox mSubscribeButton;


        public ViewHolder(FragmentTopicBinding binding) {
            super(binding.getRoot());
            mTopicNameTextView = binding.fragmentTopicNameTopic;
            mSubscribeButton = binding.fragmentTopicSubscribeCheckboc;
        }

    }


    private class Subscribe extends AsyncTask<String, Void, Void> {
        /**
         * called when user subscribe to a topic
         * params: topic
         * 1. find responsible broker about topic
         * 2. connect to broker and send information
         * 3. add topic to subscriptions
         *
         */

        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Socket requestSocket;

        private String topicExists = "existant";
        private String topic;

        private String correctBrokerIp = Constants.SERVER_IP;
        private Triple correctBroker;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... strings) {
            topic = strings[0];

            int hashedTopic = HashFunction.hashFunction(topic);
            correctBroker = consumer.findResponsibleBroker(hashedTopic);

            try {
                //make a socket to request data from a broker
                requestSocket = new Socket(correctBrokerIp, correctBroker.getPortNumber());
                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());

                out.writeUTF("consumer");
                out.flush();
                out.writeUTF("false");
                out.flush();
                out.writeUTF(new User().getConsumerId());
                out.flush();
                out.writeUTF("subscribe");
                out.flush();
                out.writeUTF(new User().getChannel().getChannelName());
                out.flush();
                out.writeUTF(topic);
                out.flush();

                System.out.println("Subscribed to: " + topic);


                in.close();
                out.close();
                requestSocket.close();

                consumer.addSubscription(topic);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class Unsubscribe extends AsyncTask<String, Void, Void>{
        /**
         * called when user unsubscribe from a topic
         * params: topic to unsubscribe
         * 1. find responsible broker about this topic
         * 2. connect with broker and send information
         * 3. remove topic from subscriptions
         */

        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Socket requestSocket;

        private String topic;

        private String correctBrokerIp = Constants.SERVER_IP;
        private Triple correctBroker;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... strings) {
            topic = strings[0];
            int hashedTopic = HashFunction.hashFunction(topic);
            correctBroker = consumer.findResponsibleBroker(hashedTopic);

            try {
                //make a socket to request data from a broker
                requestSocket = new Socket(correctBrokerIp, correctBroker.getPortNumber());
                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());

                out.writeUTF("consumer");
                out.flush();
                out.writeUTF("false");
                out.flush();
                out.writeUTF(new User().getConsumerId());
                out.flush();
                out.writeUTF("unsubscribe");
                out.flush();
                out.writeUTF(topic);
                out.flush();

                requestSocket.close();
                in.close();
                out.close();

                System.out.println("Unsubscribed from: " + topic);

                consumer.removeSubscription(topic);

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

}