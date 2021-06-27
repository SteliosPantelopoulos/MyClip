package com.example.myclip.model;


import java.util.Collections;
import various.Channel;
import various.Video;

public class Publisher extends Node {
    //publisher general info
    private final Channel channel;

    public Publisher(String id, Channel channel){
        super(id);
        this.channel = channel;
    }

    public void postVideo(Video video){
        /**
         * called when user posts a video
         * attributes: video
         * 1. add hashtags to channel
         * 2. add video to channel
         */

        for(String hashtag : video.getAssociatedHashTags()){
            channel.getHashtagsPublished().add(hashtag);
        }
        channel.addVideo(video, video.getVideoName());
    }

    public void removeVideo(Video video){
        /**
         * called when user deletes a video
         * attributes: videoName
         * 1. remove hashtags from channel
         * 2. remove video from channel
         */

        for(String hashtag : video.getAssociatedHashTags()){
            if(Collections.frequency(channel.getHashtagsPublished(), hashtag) == 1){
                channel.getHashtagsPublished().remove(hashtag);
            }
        }

        channel.removeVideo(video.getVideoName());
    }

}

