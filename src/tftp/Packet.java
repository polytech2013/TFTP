package tftp;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * Packet class
 *
 * @author Mario
 */
public class Packet {

    public static final char OPCODE_RRQ = 1;
    public static final char OPCODE_WRQ = 2;
    public static final char OPCODE_DATA = 3;
    public static final char OPCODE_ACK = 4;
    public static final char OPCODE_ERROR = 5;

    public static final int MAX_DATA_SIZE = 512;

    public byte[] data;
    public int current_size;

    public void createWRQ(String filename) {
        clear();
        addWord(OPCODE_WRQ);
        addString(filename);
        addByte((byte) 0);
        addString(STF.TRANSFER_MODE);
        addByte((byte) 0);
    }

    public void createRRQ(String filename) {

    }

    public void createData(char packet_num) {

    }

    public void createAck() {

    }

    public void createError(int error_code, String message) {

    }

    public boolean addByte(byte b) {

        if (current_size >= MAX_DATA_SIZE) {
            return false;
        }

        data[current_size] = b;
        current_size++;

        return true;

    }

    public boolean addWord(char w) {

        if (!addByte((byte) ((w & 0xFF00) >> 8))) {
            return false;
        }

        return addByte((byte) (w & 0x00FF));

    }

    public boolean addString(String s) {

        byte[] b = new byte[s.length()];
        b = s.getBytes();

        for (int i = 0; i < s.length(); i++) {

            if (!addByte(b[i])) {

                return false;

            }

        }

        return true;

    }

    public boolean addMemory(byte[] buf, int buf_size) {

        if (current_size + buf_size >= MAX_DATA_SIZE) {
            return false;
        }

        for (int i = 0; i < buf_size; i++) {

            data[current_size + i] = buf[i];

        }

        current_size += buf_size;

        return false;

    }

    public void clear() {
        this.data = new byte[MAX_DATA_SIZE];
    }

    public DatagramPacket prepareToSend(InetAddress address, int port) {
        return new DatagramPacket(data, current_size, address, port);
    }
    
    

}
