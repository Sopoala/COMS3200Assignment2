import java.io.IOException;
import java.net.*;
import java.util.Timer;

/**
 * Created by RandyZhongbin on 4/25/2015.
 */
public class Client {
    private String ipAddr = "127.0.0.1";
    int storePort = 0;
    boolean storeKnown = false;
    public Client(int request, int nameServerPort) throws SocketException, UnknownHostException {
        // if the store server is unknown, send look up request message
        if(!storeKnown){
            // Look up for Bank server
            String storeAddr = contactServer("L;Store", ipAddr, nameServerPort);
            storePort = Integer.parseInt(storeAddr.split(";")[0].trim());
            storeKnown = true;
        }
        // connect to store server
        String result = contactServer(String.valueOf(request),ipAddr,storePort);
        System.out.println(result);
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
    public static void main(String[] args) throws IOException, NumberFormatException{
        if(args.length!=2){
            System.err.println("Invalid command line arguments");
            System.exit(1);
        }
        try{
            int request = Integer.parseInt(args[0]);
            if (request > 10 ){
                System.err.println("Invalid command line arguments");
                System.exit(1);
            }
            int nameServerPort = Integer.parseInt(args[1]);
            new Client(request,nameServerPort);
        } catch(NumberFormatException e){
            System.err.println("Invalid command line arguments");
            System.exit(1);
        }
    }
}
