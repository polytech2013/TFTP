
package tftp;

/**
 * 
 * @author Mario
 */
public class TFTP {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        STF client = new STF(69, "127.0.0.1");
        client.sendFile("test_file.txt");
    }
    
}
