package com.example.myclip.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import utilities.Triple;
import various.VideoChunk;

import static java.lang.Integer.parseInt;

public class Consumer extends Node {

    //consumer general info
    private final String channelName;
    private ArrayList<String> subscriptions = new ArrayList<>();
    //brokers info
    private HashMap<String, ArrayList<String>> brokersChannelsResponsible = new HashMap<>();      //key: brokerId | value: responsible channels
    private HashMap<String, ArrayList<String>> brokersHashtagsResponsible = new HashMap<>();      //key: brokerId | value: responsible hashtags
    private HashMap<String, Triple> brokersInfo = new HashMap<>();                                //key: brokerId  |  value: broker's ip, port, hashCode


    public Consumer(String id, String channelName) {
        super(id);
        this.channelName = channelName;
    }

    public HashMap<String, ArrayList<String>> getBrokersChannelsResponsible() {
        return brokersChannelsResponsible;
    }

    public HashMap<String, ArrayList<String>> getBrokersHashtagsResponsible() {
        return brokersHashtagsResponsible;
    }

    public HashMap<String, Triple> getBrokersInfo() {
        return brokersInfo;
    }

    public ArrayList<String> getSubscriptions(){
        return subscriptions;
    }

    public void addBrokersInfo(String brokerId, Triple brokerInfo){
        brokersInfo.put(brokerId, brokerInfo);
    }

    public void addBrokersChannelsResponsible(String brokerId, ArrayList<String> channels){
        brokersChannelsResponsible.put(brokerId, channels);
    }

    public void addBrokersHashtagsResponsible(String brokerId, ArrayList<String> hashtags){
        brokersHashtagsResponsible.put(brokerId, hashtags);
    }

    public void addSubscription(String topic){
        subscriptions.add(topic);
    }

    public void removeSubscription(String topic){
        subscriptions.remove(topic);
    }

    public Triple findResponsibleBroker(int hashedTopic){
        /**
         * finds responsible broker about a topic and returns it's information
         * attributes: topic hashed
         * 1. for all brokers check which one's hashed value is closer to hashedTopic and is greater that 0
         * 2. find the correct broker and return its information (ip, port, hashCode)
         * called when a publisher wants to publish a new video and ask the first random broker about correct broker
         */

        ArrayList<Integer> hashCodes = new ArrayList();
        for(String broker: brokersInfo.keySet()){
            hashCodes.add(parseInt(brokersInfo.get(broker).getInfo()));
        }

        if(hashedTopic > Collections.max(hashCodes)){
            for(String broker: brokersInfo.keySet()){
                if(parseInt(brokersInfo.get(broker).getInfo()) == Collections.min(hashCodes)){
                    return brokersInfo.get(broker);
                }
            }
        }else{
            int min = 1000000;
            for(String x: brokersInfo.keySet()){

                int value = parseInt(brokersInfo.get(x).getInfo()) - hashedTopic;
                if(value >= 0 && value < min){
                    min = value;
                }
            }

            for(String x: brokersInfo.keySet()){
                if(parseInt(brokersInfo.get(x).getInfo()) - hashedTopic == min){
                    return brokersInfo.get(x);
                }
            }
        }
        return null;
    }

    public byte[] mergeVideoChunks(ArrayList<VideoChunk> videoChunks){
        /**
         * attributes: array list of videoChunks
         * merge all video chunks and return them in a byte array
         */

        Collections.sort(videoChunks);
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

        return data;
    }

}
