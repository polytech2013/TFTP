package tftp;

/**
 *
 * @author Sébastien
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TFTPClient {

    // Static public variables declaration.
    public static final int DEFAULT_UDP_PORT = 1025;

    public static final int DEFAULT_SOCKET_TIMEOUT = 3000;
    public static final int DEFAULT_NUMBER_OF_RETRIES = 3;

    public static final byte PACKET_DATA_TERMINATOR = 00;
    public static final byte PACKET_RRQ = 01;
    public static final byte PACKET_WRQ = 02;
    public static final byte PACKET_DATA = 03;
    public static final byte PACKET_ACK = 04;
    public static final byte PACKET_ERROR = 05;

    public static final String DOWNLOAD_NETASCII = "netascii";
    public static final String DOWNLOAD_OCTET = "octet";
    public static final String DOWNLOAD_MAIL = "mail";

    public static final int MAX_BUFFER_SIZE = 516;

    public static final byte ERROR_NOT_DEFINED = 00;
    public static final byte ERROR_FILE_NOT_FOUND = 01;
    public static final byte ERROR_ACCESS_VIOLATION = 02;
    public static final byte ERROR_DISK_FULL = 03;
    public static final byte ERROR_ILLEGAL_TFTP_OPERATION = 04;
    public static final byte ERROR_UNKNOWN_TRANSFER_ID = 05;
    public static final byte ERROR_FILE_ALREADY_EXISTS = 06;
    public static final byte ERROR_NO_SUCH_USER = 07;
    public static final String[] ERROR_MESSAGES = {"Timeout error occurred",
        "File not found",
        "Access violation",
        "Disk is full",
        "Illegal tftp operation",
        "Unknown transfer ID",
        "File already exists",
        "No such user",
        "No error message"};

    // Private instance variables.
    // Byte value that defines a no error situation.
    private static final byte ERROR_NO_ERROR = 10;

    // UDP socket and packet objects.
    private DatagramSocket socket;
    private DatagramPacket sendPacket, receivePacket;

    // Byte array stream objects.  Used to store all data sent/received.
    private ByteArrayOutputStream receivedData = new ByteArrayOutputStream();
    private ByteArrayOutputStream tftpSendPacket = new ByteArrayOutputStream();
    private ByteArrayInputStream tftpReceivedPacket;

    // Object instance status variables.
    // Byte arrays
    private byte[] opCode = new byte[2];
    private byte[] errorCode = new byte[2];
    private byte[] blockNumber = new byte[2];
    private byte[] data;

    // Integers that store communication ports, the length of the last piece of
    // data received, and the total data received
    // private int localPort;
    private int remotePort;
    private int dataLength;
    private int totalDataLength;
    private int totalActualDataLength;

    // Integers that store UDP socket timeout and number of retries
    // before client sends error packet.
    private int timeout;
    private int retries;

    // Strings storing fileName to retrieve, data transfer mode, error messages,
    // and server IP address.
    private String fileName;
    private String mode;
    private String errorMessage;
    private String serverIP;

    // End of variable declarations.
    private void setOpCode(byte secondOpCodeByte) {
        this.opCode[0] = 0;
        this.opCode[1] = secondOpCodeByte;
    }

    private void setErrorCode(byte secondErrorCodeByte) {
        this.opCode[0] = 0;
        this.opCode[1] = secondErrorCodeByte;
    }

    public TFTPClient(int remotePort, String serverIP) {
        this.serverIP = serverIP;
        this.mode = DOWNLOAD_NETASCII;
        this.errorMessage = ERROR_MESSAGES[8];
        this.timeout = DEFAULT_SOCKET_TIMEOUT;
        this.retries = DEFAULT_NUMBER_OF_RETRIES;
        this.remotePort = remotePort;
        this.dataLength = 0;
        this.totalDataLength = 0;
        byte[] bNb = {0, 0};
        this.blockNumber = bNb;
        setErrorCode(ERROR_NO_ERROR);
        try {
            initialiseSocket();
            initialisePackets();
        } catch (Exception ex) {
            Logger.getLogger(TFTPClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
     /*
     * Try to create a new socket, set the time out and return local port number.
     */
    private int initialiseSocket() throws Exception {
        // If there are any problems throw exception
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(this.timeout);
            return socket.getLocalPort();
        } catch (SocketException socketException) {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            throw new Exception("An error occurred while initialising the "
                    + "UDP socket. The error is "
                    + socketException.toString());
        }
    }

    /*
     * Create send and receive datagram packets.  Throw exception if server IP is
     * illegal.
     */
    private void initialisePackets() throws Exception {
        byte[] buffer = new byte[MAX_BUFFER_SIZE];

        try {
            sendPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.serverIP), this.remotePort);
            receivePacket = new DatagramPacket(buffer, buffer.length);
        } catch (UnknownHostException unknownHostException) {
            throw new Exception("IP address " + this.serverIP
                    + " is invalid. The error is "
                    + unknownHostException.toString());
        }
    }

    public int sendFile(String filename) throws Exception {
       
        boolean finished = false;
        int returnCode = 0;
        int retriesCounter = 1;
        int readTftpPacketValue = 0;

        setOpCode(PACKET_WRQ);
        this.fileName = filename;

        // Build TFTP Read request packet
        if (buildTftpPacket()) {
            // If packet has been built successfully, try to send it
            sendTftpPacket();
            // Loop until we are finished
            while (!finished) {
                // Try to read packet from the network
                readTftpPacketValue = readTftpPacket();
                switch (readTftpPacketValue) {
                    case 0:
                        retriesCounter = 1;
                        
                        break;
                    case 1:
                        // A socket timeout has occurred.  Need to resend last packet
                        // First check that the client has not exceeded the number of
                        // retries
                        if (retriesCounter < this.retries) {
                            // Send packet
                            sendTftpPacket();
                            retriesCounter++;
                        } else {
                            // If number of retries has been exceeded send an error packet
                            setOpCode(PACKET_ERROR);
                            setErrorCode(ERROR_NOT_DEFINED);
                            if (buildTftpPacket()) {
                                // Send packet
                                sendTftpPacket();
                                // After sending the error packet terminate
                                finished = true;
                                returnCode = 2;
                            }
                        }
                        break;
                }

            }

            try {
                // Close socket.  Ignore any thrown exceptions
                socket.close();
            } catch (Exception e) {
            }

        }
        return returnCode;
    }
/*
     * This method fetches a file from a TFTP server.
     */
    int receiveFile(String filename) throws Exception
    {
        boolean finished = false;
        int returnCode = 0;
        int retriesCounter = 1;
        int readTftpPacketValue = 0;

        this.fileName = filename;
        
        // Clear any previous received data
        receivedData.reset();
        // Set op code to read request and set remote port to 69
        setOpCode(PACKET_RRQ);
        this.errorMessage=ERROR_MESSAGES[8];
        // Build TFTP Read request packet
       if (buildTftpPacket())
         {
             // If packet has been built successfully, try to send it
             sendTftpPacket();
             // Loop until we are finished
                while (! finished)
                {
                    // Try to read packet from the network
                    readTftpPacketValue = readTftpPacket();
                    switch (readTftpPacketValue)
                    {
                        case 0:
                            // If successfull, parse packet
                            retriesCounter = 1;
                            switch (parseTftpPacket())
                            {
                                case 3:
                                     // Data packet
                                     // Check that data part is 512 bytes long
                                     if (this.dataLength == 512)
                                     {
                                         // Build an acknowledgement packet and send it back
                                         setOpCode(PACKET_ACK);
                                         if (buildTftpPacket())
                                              // Send packet
                                             sendTftpPacket();
                                     }
                                     else
                                     {
                                         // If data part is less then 512 bytes long then this is
                                         // the last data packet
                                         setOpCode(PACKET_ACK);
                                         // Build the last acknowledgement packet
                                         if (buildTftpPacket())
                                         {
                                             // Send packet
                                             sendTftpPacket();
                                             finished = true;
                                         }
                                     }
                                     break;
                                case 5:
                                    // Error packet has been received
                                    finished = true;
                                    returnCode = 1;
                                    break;
                            }
                            break;
                        case 1:
                            // A socket timeout has occurred.  Need to resend last packet
                            // First check that the client has not exceeded the number of
                            // retries
                            if (retriesCounter < this.retries)
                            {
                                // Send packet
                                sendTftpPacket();
                                retriesCounter++;
                            }
                            else
                            {
                                // If number of retries has been exceeded send an error packet
                                setOpCode(PACKET_ERROR);
                                setErrorCode(ERROR_NOT_DEFINED);
                                if (buildTftpPacket())
                                {
                                    // Send packet
                                    sendTftpPacket();
                                    // After sending the error packet terminate
                                    finished = true;
                                    returnCode = 2;
                                }
                            }
                            break;
                     }
                 }
             }
        try
        {
            // Close socket.  Ignore any thrown exceptions
            socket.close();
        }
        catch (Exception e)
        {}
        return returnCode;
    }
    private boolean buildTftpPacket() {
        Byte errorCode;

        tftpSendPacket.reset();

        switch (this.opCode[1]) {
            case PACKET_RRQ:
            case PACKET_WRQ:
                tftpSendPacket.write(opCode, 0, opCode.length);
                tftpSendPacket.write(this.fileName.getBytes(), 0, this.fileName.getBytes().length);
                tftpSendPacket.write(PACKET_DATA_TERMINATOR);
                tftpSendPacket.write(this.mode.getBytes(), 0, this.mode.getBytes().length);
                tftpSendPacket.write(PACKET_DATA_TERMINATOR);
                return true;
            case PACKET_ACK:
                tftpSendPacket.write(opCode, 0, opCode.length);
                tftpSendPacket.write(blockNumber, 0, blockNumber.length);
                return true;
            case PACKET_DATA:
                tftpSendPacket.write(opCode, 0, opCode.length);
                tftpSendPacket.write(blockNumber, 0, blockNumber.length);
                tftpSendPacket.write(data, 0, data.length);
                return true;
            case PACKET_ERROR:
                tftpSendPacket.write(opCode, 0, opCode.length);
                tftpSendPacket.write(this.errorCode, 0, this.errorCode.length);
                errorCode = new Byte(this.errorCode[1]);
                tftpSendPacket.write(ERROR_MESSAGES[errorCode.intValue()].getBytes(), 0, ERROR_MESSAGES[errorCode.intValue()].getBytes().length);
                tftpSendPacket.write(PACKET_DATA_TERMINATOR);
                return true;
            default:
                return false;
        }
    }

    private void sendTftpPacket() throws Exception {
        byte[] data = tftpSendPacket.toByteArray();

        try {
            sendPacket.setData(data, 0, data.length);
            if (sendPacket.getPort() != this.remotePort) {
                sendPacket.setPort(this.remotePort);
            }
            socket.send(sendPacket);
        } catch (IOException exception) {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            throw new Exception("Could not send packet.  Error is " + exception.toString());
        }
    }

    /*
     * This method tries to read a TFTP packet from the network.  If a socket
     * time out occurs this method returns 1 without reading any packet
     */
    private int readTftpPacket() throws Exception {
        int returnValue = 0;

        try {
            socket.receive(receivePacket);
            if (this.remotePort != receivePacket.getPort()) {
                this.remotePort = receivePacket.getPort();
            }
        } catch (SocketTimeoutException socketTimeoutException) {
            // If there is a socket time out then return 1
            returnValue = 1;
        } catch (IOException exception) {
            // If problems raise an exception
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            throw new Exception("Could not read packet.  Error is " + exception.toString());
        }
        return returnValue;
    }
/*
     * The method parses a tftp packet and disassembles it into its components
     */
    private int parseTftpPacket()
    {
        String errorMessage;
        byte[] blockNumber = new byte[2];
        byte[] oldBlockNumber;

        tftpReceivedPacket = new ByteArrayInputStream(receivePacket.getData(),
                                                      0, receivePacket.getLength());
        tftpReceivedPacket.read(opCode, 0, 2);

        switch (opCode[1])
        {
            case PACKET_ACK:
                tftpReceivedPacket.read(blockNumber, 0, 2);
                this.blockNumber=blockNumber;
                return 4;
            case PACKET_DATA:
                oldBlockNumber = this.blockNumber;
                tftpReceivedPacket.read(blockNumber, 0, 2);
                this.blockNumber=blockNumber;
                // set current packet length
                this.dataLength=tftpReceivedPacket.available();
                // If block numbers are different then it is a new data packet
                if (! Arrays.equals(blockNumber, oldBlockNumber))
                {
                    // At this time these steps are unnecessary but may be usefull in
                    // the future
                    data = new byte[this.dataLength];
                    tftpReceivedPacket.read(data, 0, data.length);
                    // Add new data chunck length to the old value
                    this.totalDataLength=this.totalDataLength+this.dataLength;
                    // Add new data chunck length to the total overall download length
                    this.totalActualDataLength=this.totalActualDataLength+this.dataLength;
                }
                else
                {
                    // If it is a retransmit just add its length to the overall download length
                   this.totalActualDataLength=this.totalActualDataLength+this.dataLength;
                }
                return 3;
            case PACKET_ERROR:
                tftpReceivedPacket.read(errorCode, 0, 2);
                this.dataLength=tftpReceivedPacket.available() - 1;
                data = new byte[this.dataLength];
                tftpReceivedPacket.read(data, 0, data.length);
                errorMessage = new String(data);
                this.errorMessage=errorMessage;
                return 5;
            case PACKET_RRQ:
                return 1;
            case PACKET_WRQ:
                return 2;
            default:
                return 6;
        }
    }
   
}
