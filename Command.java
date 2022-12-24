import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Command implements Runnable{
    
    private int sizeToRead;
    private int seekPos;
    private File commandFile;
    private File productsFile;
    private int id;
    private int P;
    private Semaphore semaphore;
    private File commandsFileOut;
    private File productsFileOut;

    public Command(File file, int sizeToRead, int seekPos, int id, int P, Semaphore semaphore, File productsFile, File commandsFileOut, File productsFileOut) {
        this.commandFile = file;
        this.productsFile = productsFile;
        this.sizeToRead = sizeToRead;
        this.seekPos = seekPos;
        this.id = id;
        this.P = P;
        this.semaphore = semaphore;
        this.commandsFileOut = commandsFileOut;
        this.productsFileOut = productsFileOut;
    }

    public static int skipUntilNewLine(int seekPos, RandomAccessFile raf) {
        int skipped = 0;
        try {
            raf.seek(seekPos);
            //read until you find a \n
            byte[] last = new byte[1];
            if (seekPos != 0) {    
                raf.seek(seekPos - 1);
                raf.read(last);
                if (last[0] != '\n')
                {
                    raf.seek(seekPos);
                    while (true) {
                        int r = raf.read(last);
                        skipped ++;
                        if (last[0] == '\n' || r == -1)
                            break;
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return skipped;
    }
    
    public static ArrayList<Byte> nextBytes(int arrSize, byte[] bytes, RandomAccessFile raf) {
        ArrayList<Byte> nextBytes = new ArrayList<>();
        try {
            if (bytes[arrSize - 1] != '\n') {
                byte[] next = new byte[1];
                while (true) {
                    int r = raf.read(next);
                    nextBytes.add(next[0]);
                    if (next[0] == '\n' || r == -1)
                        break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return nextBytes;
    }

    public static byte[] listToArray(ArrayList<Byte> byteList) {
        byte[] byteArray = new byte[byteList.size()];
            
        int[] arr = byteList.stream().mapToInt(i -> i).toArray();
        for (int i = 0; i < arr.length; i++) {
            byteArray[i] = (byte)arr[i];
        }
        return byteArray;
    }

    public static String getString(byte[] bytes, byte[] nextBytes) {
        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append(new String(bytes));
        sBuilder.append(new String(nextBytes));
        return sBuilder.toString();
    }

    public void parseCommands(String commands) {
        String[] split = commands.split("\n");
        int length = split.length;

        Thread[] products = new Thread[P];
        
        int sizeToRead = Tema2.getSizeToRead(productsFile, P);
        try {
            FileWriter fw = new FileWriter(commandsFileOut.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            for (int i = 0; i < length; i++) {
                String[] splitComma = split[i].split(",");
                int seekPos = 0;
                if (!splitComma[1].equals("0")) {
                    for (int j = 0; j < P; j++) {
                        products[j] = new Thread(new Products(j, semaphore, productsFile, sizeToRead, seekPos, splitComma[0], productsFileOut));
                        products[j].start();
                        seekPos += sizeToRead;
                    }
                    for (int j = 0; j < P; j++) {
                        try {
                            products[j].join();
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                    synchronized(this) {
                        bw.write(split[i] + ",shipped\n");
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
            RandomAccessFile raf = new RandomAccessFile(commandFile, "r");
            raf.seek(seekPos);
            
            int skipped = skipUntilNewLine(seekPos, raf);
            int arrSize = sizeToRead - skipped; 
            
            byte[] bytes = new byte[arrSize];
            raf.read(bytes);

            ArrayList<Byte> nextBytes = nextBytes(arrSize, bytes, raf);
            byte[] nextBytesArray = listToArray(nextBytes);

            raf.close();

            String commands = getString(bytes, nextBytesArray);
            
            parseCommands(commands);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
