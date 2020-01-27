import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public class FileUtils {

    static void clearFile(String file){
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file);
            writer.print("");
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void writeRaw(String filename, List<String> records) {
        File file = new File(filename);
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            System.out.print("Writing raw... ");
            write(records, writer);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void write(List<String> records, Writer writer) {
        try {
            for (String record: records) {
                writer.write(record);
                writer.write("\n");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
