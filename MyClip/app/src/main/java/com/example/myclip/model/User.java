package com.example.myclip.model;

import various.Channel;

public class User {

    private static String id;
    private static Channel channel;
    private static Publisher publisher;
    private static Consumer consumer;
    private static String publisherId;
    private static String consumerId;

    public User(){}

    public User(String id, String channelName){
        this.id = id;
        publisherId = "p" + id;
        consumerId = "c" + id;
        this.channel = new Channel(channelName);

        //create user's publisher
        publisher = new Publisher(publisherId, channel);

        //create user's consumer
        consumer = new Consumer(consumerId, channel.getChannelName());
    }

    public static Publisher getPublisher() {
        return publisher;
    }

    public static Consumer getConsumer() {
        return consumer;
    }

    public static String getPublisherId() {
        return publisherId;
    }

    public static String getConsumerId() {
        return consumerId;
    }

    public static Channel getChannel() {
        return channel;
    }

}
