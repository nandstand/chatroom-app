import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class ChatClient {

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private Scanner scanner;
    private String userName;

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String EXIT_MSG = "Bye";

    public static void main(String[] args) {
        String ipAddress = args[0];
        int port = Integer.parseInt(args[1]);
        ChatClient client = new ChatClient(ipAddress, port);
        client.listen(); // blocking / creates new thread
        client.setupUser(); // blocking / gets input from sys in until valid name, no new thread
        client.send(); // blocking / get input from sys in, does not create new thread
    }

    public ChatClient(String ipAddress, int port) {
        try {
            this.socket = new Socket(ipAddress, port);
            this.out = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream()));
            this.in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            this.scanner = new Scanner(System.in);
        } catch (IOException e) {
            //e.printStackTrace();
            closeConnection();
        }
    }

    private void setupUser() {
        String name;
        if (socket.isConnected()) {
            name = scanner.nextLine();
            while (name.equals("") || name.equals(EXIT_MSG)) {
                System.out.println("Invalid user name");
                name = scanner.nextLine();
            }
            try {
                out.write(name); // output validated name to server
                out.newLine();
                out.flush();
            } catch (IOException e) {
                closeConnection();
            }
        }
    }

    private void send() {
        while (socket.isConnected()) {
            String msg = scanner.nextLine();
            if (msg.equals(EXIT_MSG)) {
                exit(msg);
                break;
            }
            try {
                out.write(msg);
                out.newLine();
                out.flush();
            } catch (IOException e) {
                closeConnection();
            }
        }
        scanner.close();
    }

    private void exit(String exitMsg) {
        try {
            out.write(exitMsg);
            out.newLine();
            out.flush();
            closeConnection();
        } catch (IOException e) {
            closeConnection();
        }

    }

    private void listen() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!socket.isClosed()) {
                    try {
                        String incoming = in.readLine(); // blocking call
                        if (incoming != null) {
                            System.out.println(incoming);
                        }
                    } catch (IOException e) {
                        closeConnection();
                    }
                }
            }
        }); // end of anonymous Thread
        thread.start();
    }

    private void closeConnection() {
        try {
            in.close();
            out.close();
            socket.close();
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
