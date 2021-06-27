package components;

import utilities.Triple;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;

import static java.lang.Integer.parseInt;

public abstract class Node{
    //general for node
    protected String ipAddress;
    protected int portNumber;
    protected String nodeId;
    protected HashMap<String, Triple> brokersInfo = new HashMap();       // hashmap key: brokerId  |  value: broker's ip, port, hashCode
    //node act as a server
    protected ServerSocket serverSocket;        //server socket of this node
    protected int backlog = 30;
    protected Socket clientSocket;              //socket where clients connect
    //node act as a client
    protected String ipAddressOfServer;         //ip of server to connect
    protected int portNumberOfServer;           //port of server to connect
    protected Socket requestSocket;             //socket to request data
    protected ObjectOutputStream outAsClient;   //out stream to send information to server
    protected ObjectInputStream inAsClient;     //in stream to receive information from server

    public Node(String nodeId, String ipAddress, int portNumber){
        this.nodeId = nodeId;
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
    };

    protected void init(){
        /**
         * initialise server socket
         */
        try {
            serverSocket = new ServerSocket(this.portNumber, backlog);
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    protected void connect(String ipAddressOfServer, int portNumberOfServer){
        /**
         * connect with another node
         * attributes: ip and port of node to connect
         * initialize socket and streams
         * NEED TO CALL DISCONNECT() AFTER END OF COMMUNICATION
         */
        try {
            //make a socket to request data from a broker
            requestSocket = new Socket(ipAddressOfServer, portNumberOfServer);
            outAsClient = new ObjectOutputStream(requestSocket.getOutputStream());
            inAsClient = new ObjectInputStream(requestSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    protected void disconnect(){
        /**
         * disconnect node connected
         * called after connect()
         */
        try {
            //close streams and socket as a client
            inAsClient.close();
            outAsClient.close();
            requestSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    public void openServer(){
        /**
         * run a node as a server
         * initialise server socket
         */
        init();
    };

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

    protected void pushOneString(ObjectOutputStream out, String message){
        try {
            out.writeUTF(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void pushOneInt(ObjectOutputStream out, int message){
        try {
            out.writeInt(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void pushOneFloat(ObjectOutputStream out, float message){
        try {
            out.writeFloat(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void pushOneObject(ObjectOutputStream out, Object message){
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String readOneString(ObjectInputStream in){
        try {
            return in.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected int readOneInteger(ObjectInputStream in){
        try {
            return in.readInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

}
