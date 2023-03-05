/*
Christian Fuller
CSCI 4311 Programming Assignment 1
*/

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/*
ChatServer : Sets up the server side socket, accepts incoming clients,
and creates threads for each incoming client that run ServerThread object code
*/

public class ChatServer {

    private static final ArrayList<ServerThread> threads
            = new ArrayList<>();
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ServerSocket serverSocket;


    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(args[0]);
        ChatServer server = new ChatServer(port);
        System.out.println("Server Socket created : " + server.serverSocket);
        server.start();
    }

    public ChatServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    /*
    start() : Core loop for ChatServer: accept client socket connections and
                create ServerThread objects -- one thread per client
    */

    public void start(){
        System.out.println("Server running... accepting clients");
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                ServerThread serverThread = new ServerThread(socket);
                Thread thread = new Thread(serverThread);
                thread.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
    ServerThread : Manages IO for its client and relays messages to clients of
                                                       other ServerThreads
    */

    private class ServerThread implements Runnable{

        private static final String EXIT_CMD = "Bye";
        private static final String ACTIVE_USERS_CMD = "AllUsers";

        private Socket socket;
        private BufferedReader in;
        private BufferedWriter out;
        private String userName;

        public ServerThread(Socket socket)  {
            try {
                this.socket = socket;
                this.out = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream()));
                this.in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                threads.add(this); // add to ChatServer's list of active threads
            } catch (IOException e) {
                e.printStackTrace();
                closeConnection();
            }
        }

        @Override
        public void run() {
            setupUser(); // blocking!!
            while (!socket.isClosed()) {
                try {
                    String msg = in.readLine(); // blocking
                    if (msg.equals(EXIT_CMD)) {
                        closeConnection();
                        userExit();
                    } else if (msg.equals(ACTIVE_USERS_CMD)) {
                        listActiveUsers();
                    } else {
                        userMessageAll(msg); // blocking
                    }
                } catch (IOException e) {
                    closeConnection();
                }
            }
        }

        private void listActiveUsers() {
            LocalDateTime time = LocalDateTime.now();
            StringBuilder msg = new StringBuilder("Active Users at " + dtf.format(time) + ":\n");
            for (ServerThread thread : threads) {
                msg.append(thread.userName);
                msg.append("\n");
            }
            message(String.valueOf(msg));
        }

        private void userExit() {
            messageAll("Server: Goodbye, " + userName);
            threads.remove(this);
        }

        private void message(String message) {
            try {
                this.out.write(message);
                this.out.newLine(); // client input stream expects newline
                this.out.flush(); // force data to be sent to out before buffer is full
            } catch (IOException e) {
                closeConnection();
            }
        }

        private void messageAll(String message) {
            for (ServerThread thread : threads) {
                try {
                    thread.out.write(message);
                    thread.out.newLine(); // client input stream expects newline
                    thread.out.flush(); // force data to be sent to out before buffer is full
                } catch (IOException e) {
                    closeConnection();
                }
            }
            }

        private void userMessageAll(String message) {
            LocalDateTime time = LocalDateTime.now();
            messageAll(String.format("[%s] %s: %s",dtf.format(time) ,userName , message));
        }

        private void closeConnection() {
            try {
                in.close();
                out.close();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void setupUser() {
            try {
                message("Welcome!  Enter user name:");
                userName = in.readLine(); // name is validated on client side

                LocalDateTime time = LocalDateTime.now();
                messageAll(String.format("[%s] Server: Welcome, %s", dtf.format(time), userName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    } // end ServerThread

} // end Server
