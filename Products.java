import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Products implements Runnable {
    
    private int id;
    private Semaphore semaphore;
    private File productsFile;
    private int sizeToRead;
    private int seekPos;
    private String commandID;    
    private File productsFileOut;

    public Products(int id, Semaphore semaphore, File productsFile, int sizeToRead, int seekPos, String commandID, File productsFileOut){
        this.id = id;
        this.semaphore = semaphore;
        this.productsFile = productsFile;
        this.sizeToRead = sizeToRead;
        this.seekPos = seekPos;
        this.commandID = commandID;
        this.productsFileOut = productsFileOut;
    }

    public void parseProducts(String products) {
        String[] split = products.split("\n");
        int length = split.length;
        try {
            FileWriter fw = new FileWriter(productsFileOut.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            
            for (int i = 0; i < length; i++) {
                String[] splitComma = split[i].split(","); 
                if (splitComma.length > 1 && splitComma[0].equals(commandID)) {
                    synchronized(this) {
                        bw.write(commandID + "," + splitComma[1] + ",shipped\n");
                    }
                }
            }
            bw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            semaphore.acquire();
            try {
                RandomAccessFile raf = new RandomAccessFile(productsFile, "r");
                raf.seek(seekPos);

                int skipped = Command.skipUntilNewLine(seekPos, raf);
                int arrSize = sizeToRead - skipped; 

                byte[] bytes = new byte[arrSize];
                raf.read(bytes);

                ArrayList<Byte> nextBytes = Command.nextBytes(arrSize, bytes, raf);
                byte[] nextBytesArray = Command.listToArray(nextBytes);

                raf.close();

                String products = Command.getString(bytes, nextBytesArray);
                parseProducts(products);

            } catch (IOException ex) {
                ex.printStackTrace();
            }
            semaphore.release();
        } catch(InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
