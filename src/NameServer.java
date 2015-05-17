import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by RandyZhongbin on 4/24/2015.
 */
public class NameServer {
    //set server parameters
    Map<String,String> registerServers = new HashMap<String,String>();
    public NameServer(int portNo) throws Exception {
        // check if the arguments valid
        if (portNo < 0 || portNo > 65535){
            System.err.println("Invalid command line argument for Name Server");
            System.exit(1);
        }
        listeningForConnections(portNo, "Name Server");
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
        // split the message to check the message is registration or looking up message
        String[] incomingMsg = message.split(";");
        // if the message is registration
        if ("R".equalsIgnoreCase(incomingMsg[0])) {
            // get the server name, port and ip address from the registration message
            String serverName = incomingMsg[1];
            String port = incomingMsg[2];
            String ipAddr = incomingMsg[3];
            //save server info
            try{
                // put the server registration message into registerServers hashmap
                registerServers.put(serverName, port + " ; " + ipAddr);
                reply = "Registration is successful";
            } catch (Exception e){
                // if there is something wrong when registration, print out the error message
                System.err.println("Error occurred when register with name server.");
                message = "Error.";
            }
            // if the message is Looking up server request
        } else if("L".equalsIgnoreCase(incomingMsg[0])){
            // get the server name from the request
            String serverName = incomingMsg[1];
            // check if the registerServers contains the looked up server
            if(registerServers.containsKey(serverName)){
                // construct the reply message from the hash map if the server is registered
                reply = registerServers.get(serverName);
            } else {
                // construct the reply message if the server is not registered
                reply = serverName + " is not registered with name server";
            }
        } else {
            // if other error occurs, print out the error message
            reply = "Error incoming message format";
        }
        return reply;
    }

    public static void main(String[] args)  throws IOException {
        if(args.length!=1){
            System.err.println("Invalid command line arguments");
            System.exit(1);
        }
        try{
            int portNumber = Integer.parseInt(args[0]);
            new NameServer(portNumber);
        } catch(NumberFormatException e){
            System.err.println("Invalid command line arguments");
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
