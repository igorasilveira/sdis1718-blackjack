import client.MyClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MyClientApp {

    private static InetAddress hostAddress;
    protected static String hostIP;
    protected static int hostPort = 8080;

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Usage: " +
                    "java Client <host_address> <host_port>");
            System.exit(1);
        }

        try {
            hostAddress = InetAddress.getByName(args[0]);
            hostIP = hostAddress.getHostAddress();
            hostPort = Integer.parseInt(args[1]);

        } catch (UnknownHostException e) {
            System.out.println("[ERROR] Unknow host provided: " + args[0]);
            System.exit(1);
        }

        try {
            System.out.println("Connecting to server on " + hostAddress.toString());
            if (!hostAddress.isReachable(10000)) {
                System.out.println("Connection time out");
                System.exit(1);
            }

            MyClient client = new MyClient(hostIP, hostPort);
            client.run();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
