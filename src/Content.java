import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

/**
 * Created by RandyZhongbin on 4/25/2015.
 */
public class Content {
    private DatagramChannel channel = null;
    private DatagramSocket socket = null;
    private Selector selector = null;
    private String ipAddr = "127.0.0.1";
    private String itemID = null;
    private Map<String,String> contents = new HashMap<>();
    public Content(int contentPort, String fileName, int nameServerPort) throws Exception {
        if (contentPort < 0 || contentPort > 65533 || nameServerPort < 0 || nameServerPort > 65533){
            System.err.println("Invalid command line arguments for Content Server");
            System.exit(1);
        }
        // Prepare registration message
        String request = "R;Content;" + contentPort +";"+ipAddr;
        // Register with name server
        contactServer(request,ipAddr,nameServerPort);
        // read from file
        readFile(fileName);
        // listening for incoming connections
        listeningForConnections(contentPort, "Content Server");
    }
    private void listeningForConnections(int portNo, String serverName) throws Exception {
        // construct datagram socket
        DatagramSocket serverSocket = null;
        try {
            serverSocket = new DatagramSocket(portNo);
        } catch (BindException be){
            System.err.println("Cannot listen on the given port" + portNo);
            System.exit(1);
        }

        System.out.println( serverName+ " is activated. Listening on Port " + portNo);
        // waiting for incoming messages
        while (true) {
            // set buffers
            byte[] receiveData = new byte[1024];
            byte[] sendData = new byte[1024];
            // receive message from client
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            // get the message
            String msg = new String(receivePacket.getData()).trim();
            // get the port of the client
            InetAddress IPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();
            // send ACK to client
            //send ack
            sendData = "ACK".getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
            serverSocket.send(sendPacket);
            // react to the request
            if(!msg.equals("ACK")) {
                String upMsg = reactToMessage(msg);
                sendData = upMsg.getBytes();
                // send the message back to the client
                sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                serverSocket.send(sendPacket);
            }
        }
    }
    private String reactToMessage(String message) {
        String reply = null;
        String success = "The transaction is successful!\nThe item you purchased is: ";
        try{reply = success + message + " " + contents.get(message);}
        catch (Exception e){
            System.err.println("Error");
            reply = "Error";
        }
        return reply;
    }
    private void readFile(String fileName) {
        BufferedReader br = null;

        try {
            String sCurrentLine;
            br = new BufferedReader(new FileReader(fileName));

            while ((sCurrentLine = br.readLine()) != null) {
                String[] item = sCurrentLine.split(" ");
                contents.put(item[0], item[1]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }
    private String contactServer(String msg, String ipAddr, int serverPort) throws SocketException, UnknownHostException {
        // construct datagram socket
        DatagramSocket clientSocket = new DatagramSocket();
        // set server's ip address
        InetAddress IPAddress = InetAddress.getByName(ipAddr);
        // set buffers
        byte[] receiveData = new byte[1024];
        String reply = null;
        try {
            // Send request to server
            Timer timer = new Timer();
            timer.schedule(new SendDataTimer(msg,serverPort, clientSocket), 0,1000);
            //receive ACK
            DatagramPacket receiveACK = new DatagramPacket(new byte[1024], new byte[1024].length);
            clientSocket.receive(receiveACK);
            String ack = new String(receiveACK.getData()).trim();
            // if ACK received, cancel the timer
            if(ack.equals("ACK"))
            {
                timer.cancel();
                timer.purge();
            }

            // wait for the reply
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            // receive reply message from server
            clientSocket.receive(receivePacket);
            reply = new String(receivePacket.getData()).trim();

            // Send ACK to server
            byte[] sendAck = new byte[1024];
            sendAck = "ACK".getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendAck, sendAck.length, InetAddress.getByName("127.0.0.1"), serverPort);
            clientSocket.send(sendPacket);
        } catch(IOException e){
            e.printStackTrace();
        }
        // close up
        clientSocket.close();
        return reply;
    }
    public static void main(String[] args) throws IOException, NumberFormatException {
        if(args.length!=3){
            System.err.println("Invalid command line arguments");
            System.exit(1);
        }
        try{
            int contentPort = Integer.parseInt(args[0]);
            String fileName = args[1];
            int nameServerPort = Integer.parseInt(args[2]);
            new Content(contentPort,fileName,nameServerPort);
        } catch(NumberFormatException e){
            System.err.println("Invalid command line arguments");
            System.exit(1);
        }catch (FileNotFoundException e) {
                System.err.println("File Not Found!");
                System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
