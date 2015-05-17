import java.io.IOException;
import java.net.*;
import java.util.TimerTask;

/**
 * Created by RandyZhongbin on 5/17/2015.
 */
public class SendDataTimer extends TimerTask {
    private String msg;
    private int portNumber;
    private DatagramSocket clientSocket;
    private byte[] sendData = new byte[1024];

    public SendDataTimer(String msg,int portNumber, DatagramSocket clientSocket)
    {
        this.msg = msg;
        this.portNumber = portNumber;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run(){
        // construct datagram socket
        InetAddress IPAddress;
        try {
            IPAddress = InetAddress.getByName("127.0.0.1");
            // send the message to server
            sendData = msg.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, portNumber);
            // Simulate the packet loss
            if(Math.random()>=0.5)
            {
                clientSocket.send(sendPacket);
            }
            System.out.println("Trying to Send data to "+IPAddress+" at port number: "+portNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
