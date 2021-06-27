package app;

import ActionsForConnections.RunServer;
import components.Broker;

import static java.lang.Integer.parseInt;

public class Server {

   public static void main(String args[]) {

        Broker broker = new Broker(args[0],args[1], parseInt(args[2]));

        Thread t = new RunServer(broker);
        t.start();
    }

}
