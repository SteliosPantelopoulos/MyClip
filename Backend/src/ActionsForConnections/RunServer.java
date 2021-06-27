package ActionsForConnections;

import components.Broker;

public class RunServer extends Thread{

    private Broker broker = null;

    public RunServer(Broker broker){
        this.broker = broker;
    }

    public void run(){
        if(broker!= null){
            broker.openServer();
        }
    }
}
