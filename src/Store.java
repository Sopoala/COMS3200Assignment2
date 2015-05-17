import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;


public class Store {
    private DatagramChannel channel = null;
    private DatagramSocket socket = null;
    private Selector selector = null;
    private int storePort, bankPort, contentPort;
    private String ipAddr = "127.0.0.1";
    private Map<String, String> stocks = new LinkedHashMap<String, String>();
    private ArrayList<String> stocksAL = new ArrayList<>();
    private String storeMsgSend = null;

    public Store(int storePort, String fileName, int nameServerPort) throws Exception {
        if (storePort < 0 || storePort > 65533 || nameServerPort < 0 || nameServerPort > 65533) {
            System.err.println("Invalid command line argument for Name Server");
            System.exit(1);
        }
        // connect to server and send registration request
        // prepare request message
        String registrationMsg = "R;Store;" + storePort+";"+ipAddr;
        contactServer(registrationMsg,ipAddr,nameServerPort);
        // send look up request message
        // Look up for Bank server
        String bankAddr = contactServer("L;Bank",ipAddr,nameServerPort);
        bankPort = Integer.parseInt(bankAddr.split(";")[0].trim());
        // Look up for Content server
        String contentAddr = contactServer("L;Content",ipAddr,nameServerPort);
        contentPort = Integer.parseInt(contentAddr.split(";")[0].trim());

        // read contents from txt file and store the contents to a hash map
        readFile(fileName);
        listeningForConnections(storePort, "Store server");
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
    private String reactToMessage(String message) throws SocketException, UnknownHostException {
        String reply = null;
        if (message.equals("0")) {
            reply ="";
            for(int i = 0; i < stocksAL.size(); i++){
                reply += stocksAL.get(i) + "\n";
            }
        } else {
            String itemID = stocksAL.get((Integer.parseInt(message)) - 1).split(" ")[0];
            double itemPrice = Double.parseDouble(stocksAL.get((Integer.parseInt(message)) - 1).split(" ")[1]);
            String creditCard = "1234567890123456";
            storeMsgSend =itemID +" " + itemPrice + creditCard;
            String bankReply = contactServer(storeMsgSend,ipAddr,bankPort);
            if(bankReply.equals("0")){
                reply = "Transaction aborted\n";
            } else {
                try {
                    reply = contactServer(itemID, ipAddr, contentPort);
                } catch (Exception e) {
                    reply = "Transaction Aborted\n";
                }
            }
        }
        return reply;
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
    private void readFile(String fileName) {
        BufferedReader br = null;

        try {
            String sCurrentLine;
            br = new BufferedReader(new FileReader(fileName));
            int n = 0;
            while ((sCurrentLine = br.readLine()) != null) {
                String[] item = sCurrentLine.split(" ");
                stocks.put(item[0], item[1]);
                stocksAL.add(n, sCurrentLine);
                n++;
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
    public static void main(String[] args) throws IOException, NumberFormatException {
        if(args.length!=3){
            System.err.println("Invalid command line arguments");
            System.exit(1);
        }
        try{
            int storePort = Integer.parseInt(args[0]);
            String fileName = args[1];
            int nameServerPort = Integer.parseInt(args[2]);
            new Store(storePort,fileName,nameServerPort);
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
