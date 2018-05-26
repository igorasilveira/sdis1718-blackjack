import server.MyServer;

import java.io.IOException;

public class MyServerApp {

    public static void main(final String... args) throws IOException {
        MyServer server = null;
        try {
            server = new MyServer(Integer.parseInt(args[0]));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            server.startSSLContext();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[ERROR] Could not configure SSL context");
            System.exit(1);
        }
        server.start();
    }
}
