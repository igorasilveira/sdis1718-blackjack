import server.MyServer;

import java.io.IOException;

public class MyServerApp {

    public static void main(final String... args) {
        MyServer server = null;
        try {
            server = new MyServer(Integer.parseInt(args[0]));
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.start();
    }
}
