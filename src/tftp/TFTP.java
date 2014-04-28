package tftp;

import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mario
 */
public class TFTP {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        int choix = 0;

        TFTPClient client = new TFTPClient(69, "127.0.0.1");

        System.out.println("------------MENU CLIENT-------------");
        System.out.println("1. Write a file");
        System.out.println("2. Read a file");
        do {
            System.out.println("Your choice ? (1 or 2)");
            choix = sc.nextInt();
        } while (choix < 0 || choix > 2);

        int result=0;
        switch (choix) {
            case 1:
                try {
                    result = client.sendFile("test_file.txt");
                } catch (Exception ex) {
                    Logger.getLogger(TFTP.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            case 2:
                try {
                    result = client.receiveFile("catchMe.txt");
                } catch (Exception ex) {
                    Logger.getLogger(TFTP.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
        }
        System.out.println(result);
        if(result==0){
            System.out.println("Succesfull transaction");
        }else{
            System.out.println("An error occured, please try again");
        }

    }

}
