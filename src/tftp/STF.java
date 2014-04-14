
package tftp;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client class
 * @author Mario
 */
public class STF {
    
    public static final String TRANSFER_MODE = "netascii";
    
    public int server_port;
    public String server_ip;
    public InetAddress server_address;
    public DatagramSocket server_socket;
    
    public FileInputStream in;

    public STF(int server_port, String server_ip) {
        this.server_port = server_port;
        this.server_ip = server_ip;
        
        try {
            this.server_address = InetAddress.getByName(this.server_ip);
        } catch (UnknownHostException ex) {
            Logger.getLogger(STF.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            this.server_socket = new DatagramSocket(server_port, this.server_address);
        } catch (SocketException ex) {
            Logger.getLogger(STF.class.getName()).log(Level.SEVERE, null, ex);
        }
    }   
    
    /**
     * Send WRQ request to server and write a file
     * @return 
     */
    public boolean sendFile(String filename) {
        
        Packet packet_wrq, packet_ack, packet_data;
        
        byte[] data = new byte[Packet.MAX_DATA_SIZE];
        
        try {
            this.in = new FileInputStream("C:\\Users\\Mario\\Documents\\NetBeansProjects\\TFTP\\" + filename);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(STF.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // Send WRQ packet
        packet_wrq = new Packet();
        packet_wrq.createWRQ(filename);
        
        System.out.println(new String(packet_wrq.data));
        
        try {
            server_socket.send(packet_wrq.prepareToSend(server_address, server_port));             
        } catch (IOException ex) {
            Logger.getLogger(STF.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // Receive ACK packet
            
        // Send DATA packet

        // Receive ACK packet   
        
        
        return true;
    }
    
    /**
     * Send RRQ request to server and read a file
     * @return 
     */
    public boolean receiveFile() {
        
        return true;
    }
    
    /**
     * Close connection
     */
    public void disconnect() {
        
    }

}
