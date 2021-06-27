package components;

import ActionsForConnections.BrokerActionsAsServer;
import ActionsForConnections.BrokerActionsToSend;
import utilities.HashFunction;
import utilities.Triple;
import utilities.Tuple;
import various.Video;
import various.VideoChunk;

import java.io.*;
import java.util.*;

import static java.lang.Integer.parseInt;

public class Broker extends Node{
    //general for broker
    private ArrayList<String> hashtagsResponsible = new ArrayList();                                      //hashtags that this broker is responsible
    private ArrayList<String> channelsResponsible = new ArrayList();                                      //channel names that this broker is responsible
    private HashMap<String, ArrayList<Video>> responsibleVideos = new HashMap();                          //key: publisherId | value: responsibleVideos
    //other brokers information
    private final String brokersPath = "docs//brokersInfo.txt";
    private HashMap<String, ArrayList<String>> brokersTopics = new HashMap();                             //hashmap key: brokerId |  value: broker's topics responsible
    //connected consumers
    private HashMap<String, ArrayList<String>> registeredConsumersSubscriptionsHashtags = new HashMap();  //key: consumerId | value: [subscriptions of hashtags]
    private HashMap<String, ArrayList<String>> registeredConsumersSubscriptionsChannels = new HashMap();  //key: consumerId | value: [subscriptions of channels]
    private HashMap<String, String> registeredConsumersInfo = new HashMap();                              //key: consumerId | value: channelName
    //connected publishers
    private HashMap<String, ArrayList<Tuple>> registeredPublishersHashtags = new HashMap<>();             //key: publisherId | value: tuple(video name, hashtag) publisher's hashtags that correspond to this broker
    private HashMap<String, String> registeredPublishersChannelNames = new HashMap<>();                   //key: publisherId | value: publisher's channel name that correspond to this broker


    public Broker(String id, String ipAddress, int portNumber){
        super(id, ipAddress, portNumber);
        readBrokers();                              //get information about other brokers
    }

    protected void init() {
        super.init();
    }

    public void openServer() {
        super.openServer();
        //wait for request from other node
        while(true){
            try {
                clientSocket = serverSocket.accept();

                Thread t = new BrokerActionsAsServer(this, clientSocket);
                t.start();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    //METHODS FOR COMMUNICATION

    //AS A SERVER

    //CONSUMER CONNECTION
    public void consumerConnection(ObjectOutputStream out, ObjectInputStream in){
        /**
         * a consumer is connected with this broker
         * attributes: I/O streams with consumer
         * 1. synchronize broker
         * 2. check if consumer is connected for the first time in a node
         * 3. if yes return information about all the brokers
         * 4. else  if subscribe : subscribe consumer to topic
         *          if unsubscribe: unsubscribe consumer to topic
         *          if getAllVideos: return all videos that contain topic(s) that consumer is subscribed
         */

        synchronized (this){
            String consumerId;
            String firstTime;
            firstTime = readOneString(in);
            consumerId = readOneString(in);

            if(firstTime.equals("true")){
                //consumer connects for the first time in a broker
                pushOneInt(out, brokersInfo.size());
                for(String brokerToSend :brokersInfo.keySet()){

                    pushOneString(out, brokerToSend);                                   //send brokerId
                    pushOneObject(out, brokersInfo.get(brokerToSend));                  //send broker info

                    ArrayList<String> brokerChannels = findResponsibleChannelsForBroker(brokerToSend);
                    ArrayList<String> brokerHashtags = findResponsibleHashtagsForBroker(brokerToSend);

                    pushOneInt(out, brokerChannels.size());
                    for(String channelName: brokerChannels){
                        pushOneString(out, channelName);                                //send brokerResponsibleChannels
                    }

                    pushOneInt(out, brokerHashtags.size());
                    for(String hashtag: brokerHashtags){
                        pushOneString(out, hashtag);                                    //send brokerResponsibleHashtags
                    }
                }
            }else{
                //consumer connected to correct broker
                String action = readOneString(in);

                if(action.equals("subscribe")){
                    String consumerChannelName = readOneString(in);
                    acceptConnectionConsumer(consumerId, consumerChannelName);
                    String topic = readOneString(in);
                    registerConsumerInTopic(consumerId, topic);

                }else if(action.equals("unsubscribe")){
                    String topicUnsubscribe = readOneString(in);
                    unregisterConsumerFromTopic(consumerId, topicUnsubscribe);

                }else if(action.equals("getAllVideos")){
                    String consumerChannelName = readOneString(in);
                    String topic = readOneString(in);

                    if(topic.startsWith("#")){
                        //todo check if there is a better way to find how many video will be send
                        int counter = 0;
                        for (Map.Entry<String, ArrayList<Video>> entry : responsibleVideos.entrySet()){
                            String publisherChannelName = registeredPublishersChannelNames.get(entry.getKey());
                            if(filterConsumer(publisherChannelName, consumerChannelName)) {
                                for(Video video : entry.getValue()) {
                                    if (video.getAssociatedHashTags().contains(topic)) {
                                        counter++;
                                    }
                                }
                            }
                        }

                        pushOneInt(out, counter);
                        for (Map.Entry<String, ArrayList<Video>> entry : responsibleVideos.entrySet()){
                            String publisherChannelName = registeredPublishersChannelNames.get(entry.getKey());
                            if(filterConsumer(publisherChannelName, consumerChannelName)) {
                                for(Video video : entry.getValue()) {
                                    if (video.getAssociatedHashTags().contains(topic)) {
                                        Thread t = new BrokerActionsToSend(this, video, out, in);
                                        t.start();
                                    }
                                }
                            }
                        }

                    }else{
                        for (Map.Entry<String, String> entry : registeredPublishersChannelNames.entrySet()){
                            if(entry.getValue().equals(topic) && filterConsumer(topic, consumerChannelName)) {
                                ArrayList<Video> videos = responsibleVideos.get(entry.getKey());
                                pushOneInt(out, videos.size());
                                for (Video video : videos) {
                                    Thread t = new BrokerActionsToSend(this, video, out, in);
                                    t.start();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private ArrayList<String> findResponsibleHashtagsForBroker(String brokerToSend) {
        /**
         * for a certain broker finds and returns all responsible hashtags
         */

        if(brokerToSend.equals(nodeId)){ return hashtagsResponsible; }

        ArrayList<String> brokerHashtags = new ArrayList<>();
        for(String topic: brokersTopics.get(brokerToSend)){
            if(topic.startsWith("#")){
                brokerHashtags.add(topic);
            }
        }
        return brokerHashtags;
    }

    private ArrayList<String> findResponsibleChannelsForBroker(String brokerToSend) {
        /**
         * for a certain broker finds and returns all responsible channels
         */

        if(brokerToSend.equals(nodeId)){ return channelsResponsible; }

        ArrayList<String> brokerChannels = new ArrayList<>();
        for(String topic: brokersTopics.get(brokerToSend)){
            if(!topic.startsWith("#")){
                brokerChannels.add(topic);
            }
        }
        return brokerChannels;
    }

    private void acceptConnectionConsumer(String consumerId, String channelName){
        /**
         * add consumer info to broker
         * attributes: consumer's id, channel
         * 1. add consumer to registered consumers
         * 2. initialise consumer's subscriptions
         *
         * called when consumer is connected for the first time with this broker
         */

        registeredConsumersInfo.put(consumerId, channelName);
        registeredConsumersSubscriptionsHashtags.put(consumerId, new ArrayList());
        registeredConsumersSubscriptionsChannels.put(consumerId, new ArrayList());
    }

    private void registerConsumerInTopic(String consumerId, String topicToSubscribe){
        /**
         * subscribe a consumer in a topic
         * attributes: consumer's id, topic to subscribe
         * 1. add subscription of consumer
         *
         * called when consumer wants to subscribe in a channel or a hashtag
         */

        if(topicToSubscribe.startsWith("#")){
            registeredConsumersSubscriptionsHashtags.get(consumerId).add(topicToSubscribe);
        }else{
            registeredConsumersSubscriptionsChannels.get(consumerId).add(topicToSubscribe);
        }
        System.out.println("Consumer: " + consumerId + " subscribed to: " + topicToSubscribe);
    }

    private void unregisterConsumerFromTopic(String consumerId, String topicToUnsubscribe){
        /**
         * unsubscribe a consumer from a topic
         * attributes: consumer's id, topic to unsubscribe
         * 1. remove subscription of consumer
         * 2. if consumer doesn't have any other subscriptions in this broker remove his info
         *
         * called when consumer wants to unsubscribe from a channel or a hashtag
         */

        if(topicToUnsubscribe.startsWith("#")){
            registeredConsumersSubscriptionsHashtags.get(consumerId).remove(topicToUnsubscribe);
        }else{
            registeredConsumersSubscriptionsChannels.get(consumerId).remove(topicToUnsubscribe);
        }

        if(registeredConsumersSubscriptionsChannels.get(consumerId).isEmpty() && registeredConsumersSubscriptionsHashtags.get(consumerId).isEmpty()){
            registeredConsumersSubscriptionsHashtags.remove(consumerId);
            registeredConsumersSubscriptionsChannels.remove(consumerId);
            registeredConsumersInfo.remove(consumerId);
        }

        System.out.println("Consumer: " + consumerId + " unsubscribed to: " + topicToUnsubscribe);
    }

    //BROKER CONNECTION
    public void brokerConnection(ObjectOutputStream out, ObjectInputStream in){
        /**
         * broker is connected with this broker
         * attributes: I/O streams with broker
         * 1. synchronize the broker
         * 2. check if the other broker has added/remove topics and update brokersTopics
         *
         * called when a broker is connected to this broker to inform about changes
         */

        synchronized (this){

            String connectedBrokerId = readOneString(in);
            int numberOfChannels = readOneInteger(in);


            ArrayList<String> newTopics = new ArrayList<>();
            //for added topics
            for(int i=0; i<numberOfChannels; i++){
                String channelName = readOneString(in);
                newTopics.add(channelName);
                if(!(brokersTopics.get(connectedBrokerId).contains(channelName))){
                    brokersTopics.get(connectedBrokerId).add(channelName);
                }
            }

            int numberOfHashtags = readOneInteger(in);
            for(int i=0; i<numberOfHashtags; i++){
                String hashtag = readOneString(in);
                newTopics.add(hashtag);
                if(!(brokersTopics.get(connectedBrokerId).contains(hashtag))){
                    brokersTopics.get(connectedBrokerId).add(hashtag);
                }
            }

            //for removed topics
            ArrayList<String> copy = (ArrayList<String>) brokersTopics.get(connectedBrokerId).clone();
            copy.removeAll(newTopics);
            brokersTopics.get(connectedBrokerId).removeAll(copy);
        }
    }

    //PUBLISHER CONNECTION
    public void publisherConnection(ObjectOutputStream out, ObjectInputStream in){
        /**
         * publisher is connected to this broker
         * attributes: I/O streams with publisher
         * 1. synchronize broker
         * 2. check for action of publisher
         * 3. notify: return correct broker about a topic
         * 4. addhashtag: add new hashtag, inform all the other brokers
         * 5. removehashtag: remove hashtag and inform all other brokers
         * 6. addchannel: add new channel, inform all the other brokers
         * 7. removechannel: remove channel, inform all the other brokers
         * 8. addVideo: add video for channel
         *
         * called when a publisher is connected to this broker either to inform about added/removed content or to ask for correct broker about a topic
         */

        synchronized (this){
            String typeOfAction = readOneString(in);
            String publisherId = readOneString(in);

            if(typeOfAction.equals("notify")){
                String message = readOneString(in);
                int hashedMessage = HashFunction.hashFunction(message);

                Triple infoOfCorrectBroker = findResponsibleBroker(hashedMessage);
                pushOneObject(out, infoOfCorrectBroker);

            }else if(typeOfAction.equals("addhashtag")){
                String channelName = readOneString(in);
                acceptConnectionPublisher(publisherId, channelName);

                //get hashtag info
                String videoName = readOneString(in);
                String hashtag = readOneString(in);
                addHashtag(publisherId, new Tuple(videoName, hashtag));

                //save new video
                Video addedVideo = pull(out, in, videoName);
                if(!videoExists(addedVideo)){
                    responsibleVideos.get(publisherId).add(addedVideo);
                }

                notifyBrokersOnChanges();
            }else if(typeOfAction.equals("removehashtag")){
                String videoName = readOneString(in);
                String hashtag = readOneString(in);
                removeHashtag(publisherId, new Tuple(videoName, hashtag));

                notifyBrokersOnChanges();
            }else if(typeOfAction.equals("addchannel")){
                String channelName = readOneString(in);
                acceptConnectionPublisher(publisherId, channelName);
                addChannelName(publisherId, channelName);

                notifyBrokersOnChanges();
            }else if(typeOfAction.equals("removechannel")){
                String channelName = readOneString(in);
                removeChannelName(publisherId, channelName);

                notifyBrokersOnChanges();
            }else if(typeOfAction.equals("addVideo")){
                String videoName = readOneString(in);

                Video newVideo = pull(out, in, videoName);

                if(!videoExists(newVideo)){
                    responsibleVideos.get(publisherId).add(newVideo);
                }
            }
        }
    }

    private void acceptConnectionPublisher(String publisherId, String channelName){
        /**
         * add publisher info to broker
         * attributes: publisher's id, channel name
         * 1. add publisher to responsible videos
         * 2. add channel name to responsible topics
         * 3. notify other brokers
         *
         * called when publisher is connected for the first time with this broker(add channel name or add hashtag)
         */
        if(!(registeredPublishersChannelNames.containsKey(publisherId))){
            responsibleVideos.put(publisherId, new ArrayList());
            registeredPublishersChannelNames.put(publisherId, channelName);
            registeredPublishersHashtags.put(publisherId, new ArrayList());
        }
    }

    private Video pull(ObjectOutputStream out, ObjectInputStream in, String videoName){
        /**
         * pull video from a publisher
         * attributes: out, in streams and name of video
         * returns Video
         * 1. send video name
         * 2. receive video's information
         */

        synchronized (this){

            String channelName, dateCreated;
            float length;
            int framerate, frameWidth, frameHeight;
            ArrayList<String> hashtags;
            ArrayList<VideoChunk> videoChunks = new ArrayList<>();
            int numberOfBytes = 0;

            try {

                channelName = in.readUTF();
                dateCreated = in.readUTF();
                length = in.readFloat();
                framerate = in.readInt();
                frameWidth = in.readInt();
                frameHeight = in.readInt();
                hashtags = (ArrayList<String>) in.readObject();

                int chunksNumber = in.readInt();
                for(int i=0; i<chunksNumber; i++){
                    VideoChunk chunk = (VideoChunk) in.readObject();
                    videoChunks.add(chunk);
                    numberOfBytes += chunk.getData().length;
                }

                byte[] videoBytes = new byte[numberOfBytes];
                int counter = 0;
                for(VideoChunk chunk: videoChunks){
                    for(int i=0; i<chunk.getData().length; i++){
                        videoBytes[counter + i] = chunk.getData()[i];
                    }
                    counter += chunk.getData().length;
                }

                Video video = new Video(videoName, channelName, dateCreated, length, framerate, frameWidth, frameHeight, hashtags, videoBytes);
                return video;

            }catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }



            disconnect();
            return null;
        }
    }

    private boolean videoExists(Video addedVideo) {
        /**
         * this method returns if video is already saved in broker
         */

        boolean found = false;
        for (ArrayList<Video> videosOfPublisher : responsibleVideos.values()) {
            for(Video video: videosOfPublisher){
                if(video.getVideoName().equals(addedVideo.getVideoName())){
                    found = true;
                }
            }
        }
        return found;
    }

    private void addHashtag(String publisherId, Tuple videoAndHashtag){
        /**
         * add hashtag to broker
         * attributes: publisher id that adds hashtag, Tuple(videoName, hashtag)
         * 1. updates data structures (hashtagsResponsible, registeredPublisherHashtags)
         *
         * called when a user adds a new video
         * for all hashtags of new video call this function
         */

        if(!(hashtagsResponsible.contains(videoAndHashtag.getHashtag()))){                              //check if hashtag is already assigned to broker
            hashtagsResponsible.add(videoAndHashtag.getHashtag());                                      //update hashtags responsible
        }

        if(!(registeredPublishersHashtags.get(publisherId).contains(videoAndHashtag))){                 //check if this broker has already add this hashtag for another video
            registeredPublishersHashtags.get(publisherId).add(videoAndHashtag);                         //update hashmap(hashtags) of registered publisher
        }

        System.out.println("Publisher: " + publisherId + " added hashtag: " + videoAndHashtag.getHashtag() + " for video: " + videoAndHashtag.getVideoName());
    }

    private void removeHashtag(String publisherId, Tuple videoAndHashtag){
        /**
         * remove hashtag from broker
         * attributes: publisher id that removes hashtag, Tuple(videoName, hashtag)
         * 1. check if this hashtag is used by other publishers
         * 2. if yes remove hashtag from broker's responsible topics
         * 3. remove hashtag from this publisher's responsible topics
         * 4. remove video from responsibleVideos
         *
         * called when a user delete one video
         * for all hashtags of new video call this function
         */

        int counter = 0;
        for(String publisher: registeredPublishersHashtags.keySet()){                                                               //for all registered publishers
            for(Tuple x: registeredPublishersHashtags.get(publisher)){                                                              //for a publisher for all tuples(videoName, hashtag)
                if(videoAndHashtag.getHashtag().equals(x.getHashtag())){
                    counter++;
                }
            }
        }

        if(counter == 1){                                                                                                           //hashtag only contained in one publisher so need to remove it from hashtagsResponsible
            hashtagsResponsible.remove(videoAndHashtag.getHashtag());                                                               //remove hashtag from brokers responsible hashtags
        }
        for(Tuple x: registeredPublishersHashtags.get(publisherId)){
            if(videoAndHashtag.getHashtag().equals(x.getHashtag()) && videoAndHashtag.getVideoName().equals(x.getVideoName())){
                registeredPublishersHashtags.get(publisherId).remove(x);                                                            //remove Tuple(video, hashtag) from this publisher responsible hashtags
            }
        }

        for (Map.Entry<String, ArrayList<Video>> entry : responsibleVideos.entrySet()) {
            String publisher = entry.getKey();
            ArrayList<Video> videos = entry.getValue();

            if(publisher.equals(publisherId)){
                for(Video video : videos){
                    if(video.getVideoName().equals(videoAndHashtag.getVideoName())){
                        videos.remove(video);
                    }
                }
            }
        }

        System.out.println("Publisher: " + publisherId + " removed hashtag: " + videoAndHashtag.getHashtag() + " for video: " + videoAndHashtag.getVideoName());
    }

    private void addChannelName(String publisherId, String channelName){
        /**
         * add new channel to broker
         */
        channelsResponsible.add(channelName);                                  //update broker's responsible channels
        System.out.println("Publisher " + publisherId + " added channel: " + channelName);
    }

    private void removeChannelName(String publisherId, String channelName){
        /**
         * remove channel from data
         * attributes: publisher Id(owner of channel), channel name
         * 1. remove channel from broker's topics responsible
         * 2. remove publisher from registered publishers
         *
         * called when a channel is deleted
         */
        channelsResponsible.remove(channelName);
        registeredPublishersChannelNames.remove(publisherId);
        registeredPublishersHashtags.remove(publisherId);

        System.out.println("Publisher " + publisherId + " removed channel: " + channelName);
    }


    //AS A CLIENT

    private void readBrokers(){
        /**
         * read brokers info from a file
         * called when broker is created
         */

        synchronized (this){
            try {
                File myObj = new File(brokersPath);
                Scanner myReader = new Scanner(myObj);
                while (myReader.hasNextLine()) {
                    String line = myReader.nextLine();
                    String[] data = line.split(" ");
                    //data[0] = broker id
                    //data[1] = broker ipAddress
                    //data[2] = broker portNumber

                    brokersTopics.put(data[0], new ArrayList<>());
                    brokersInfo.put(data[0], new Triple(data[1], parseInt(data[2]), Integer.toString(HashFunction.hashFunction(data[1]+data[2]))));
                }
                myReader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    private boolean filterConsumer(String publisherChannel, String consumerChannel){
        /**
         * checks if consumer and broker are from the same user
         */
        if(publisherChannel.equals(consumerChannel)){
            return false;
        }else{
            return true;
        }
    }

    private void notifyBrokersOnChanges(){
        /**
         * when a topic is changed(add/remove) in this broker notify all the other brokers about this change
         * 1. for all brokers except this one connect with them
         * 2. send them broker id and responsible channel's name and hashtags
         *
         * called when there is a change in responsibleChannelName or responsibleHashtags
         */

        Set<String> keys = brokersInfo.keySet();
        for(String brokerKey: keys){
            if(!(brokerKey.equals(nodeId))){
                ipAddressOfServer= brokersInfo.get(brokerKey).getIpAddress();
                portNumberOfServer = brokersInfo.get(brokerKey).getPortNumber();
                connect(ipAddressOfServer, portNumberOfServer);

                pushOneString(outAsClient, "broker");
                pushOneString(outAsClient, nodeId);                    //inform about broker id
                pushOneInt(outAsClient, channelsResponsible.size());
                for(String hashCodeOfTopic: channelsResponsible){
                    pushOneString(outAsClient, hashCodeOfTopic);       //send channel names
                }
                pushOneInt(outAsClient, hashtagsResponsible.size());
                for(String hashCodeOfTopic: hashtagsResponsible){
                    pushOneString(outAsClient, hashCodeOfTopic);       //send hashtag
                }
                disconnect();
            }
        }
    }

    public void sendVideoToSubscribers(ObjectOutputStream out, ObjectInputStream in, Video video){
        /**
         * send video info
         * called to send video: broker -> consumer
         */
        synchronized (this){
            pushOneString(out, video.getVideoName());
            pushOneString(out, video.getChannelName());
            pushOneString(out, video.getDateCreated());
            pushOneFloat(out, video.getLength());
            pushOneInt(out, video.getFramerate());
            pushOneInt(out,video.getFrameWidth());
            pushOneInt(out, video.getFrameHeight());
            pushOneObject(out, video.getAssociatedHashTags());

            pushOneInt(out, video.getChunks().size());

            for(VideoChunk chunk: video.getChunks()){
                try {
                    out.writeObject(chunk);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}