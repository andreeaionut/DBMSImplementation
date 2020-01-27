import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MergeJoiner {

    public static void mergeJoinFiles(String firstFile, String secondFile, String outputFile, Comparator<String> cmp) {
        BufferedReader firstBfr = null;
        BufferedReader secondBfr = null;
        BufferedWriter outputwriter = null;
        try {
            firstBfr = new BufferedReader(new FileReader(new File(firstFile)));
            secondBfr = new BufferedReader(new FileReader(new File(secondFile)));
            outputwriter = new BufferedWriter(new FileWriter(new File(outputFile)));
            String parentFilePointer = firstBfr.readLine();
            String childFilePointer = secondBfr.readLine();
            while (parentFilePointer != null && childFilePointer != null) {
                while(cmp.compare(childFilePointer, parentFilePointer)==0){
                    //add join result
                    outputwriter.write(parentFilePointer + " " + childFilePointer);
                    outputwriter.newLine();
                    childFilePointer = secondBfr.readLine();
                }
                parentFilePointer = firstBfr.readLine();
            }
            outputwriter.close();
            firstBfr.close();
            secondBfr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
