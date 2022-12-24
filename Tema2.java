import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Semaphore;

public class Tema2 {

    public static int getSizeToRead(File file, int P) {
        Path path = Paths.get(file.getAbsolutePath());
        int sizeToRead = -1;
        try {
            sizeToRead = (int)(Files.size(path) / P); 
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return sizeToRead;
    }

    public static void main(String[] args) {
        File dir = new File(args[0]);
        File[] inputFiles = dir.listFiles();
        int P = Integer.parseInt(args[1]);
        
        Semaphore semaphore = new Semaphore(P);

        File commandsFileOut = new File("orders_out.txt");
        File productsFileOut = new File("order_products_out.txt");

        try {
            if (!commandsFileOut.exists()) {
                commandsFileOut.createNewFile();
            } else {
                new FileWriter(commandsFileOut, false).close(); 
            }
            if (!productsFileOut.exists()) {
                productsFileOut.createNewFile();
            } else {
                new FileWriter(productsFileOut, false).close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        Thread[] commands = new Thread[P];
        int seekPos = 0;
        int sizeToRead = Tema2.getSizeToRead(inputFiles[0], P);
        
        for (int i = 0; i < P; i++) {
            commands[i] = new Thread(new Command(inputFiles[0], sizeToRead, seekPos, i, P, semaphore, inputFiles[1], commandsFileOut, productsFileOut));
            commands[i].start();
            seekPos += sizeToRead;
        } 

        for (int i = 0; i < P; i++) {
            try {
                commands[i].join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}
