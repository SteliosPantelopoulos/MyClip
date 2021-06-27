package ActionsForConnections;

import components.Broker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class BrokerActionsAsServer extends Thread{
    private Broker broker;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String typeOfConnection;


    public BrokerActionsAsServer(Broker broker, Socket connection){
        this.broker  = broker;
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run(){
        try {

            typeOfConnection = in.readUTF();

            if (typeOfConnection.equals("consumer")) {
                //consumer connected
                System.out.println("Consumer connected");
                broker.consumerConnection(out, in);
            }else if(typeOfConnection.equals("broker")){
                //broker connected
                System.out.println("Broker connected");
                broker.brokerConnection(out, in);
            }else if(typeOfConnection.equals("publisher")){
                //publisher is connected
                System.out.println("Publisher connected");
                broker.publisherConnection(out, in);
            }else{
                System.out.println("Wrong type of connection in broker");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
