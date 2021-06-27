package ActionsForConnections;

import components.Broker;
import various.Video;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class BrokerActionsToSend extends Thread{

    private Broker broker;
    private Video video;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public BrokerActionsToSend(Broker broker, Video video, ObjectOutputStream out, ObjectInputStream in) {
        this.broker = broker;
        this.video = video;
        this.out = out;
        this.in = in;
    }

    public void run(){
        broker.sendVideoToSubscribers(out, in, video);
    }
}
