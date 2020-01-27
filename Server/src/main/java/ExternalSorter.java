import java.io.*;
import java.util.*;

public class ExternalSorter {
    public static List<File> sortInBatch(File file, Comparator<String> cmp) throws IOException {
        List<File> files = new ArrayList<File>();
        BufferedReader fbr = new BufferedReader(new FileReader(file));
        long totalFilesCreated = 0;
        try{
            List<String> tmplist =  new ArrayList<String>();
            String line = "";
            int noLines = 10000;
            try {
                while(line != null) {
                    while(((line = fbr.readLine()) != null) && noLines > 0){
                        tmplist.add(line);
                        noLines--;
                    }
                    files.add(sortAndSave(tmplist,cmp));
                    totalFilesCreated++;
                    tmplist.clear();
                    Runtime.getRuntime().gc();
                    noLines = 10000;
                    totalFilesCreated++;
                }
                System.out.println("Total files created: " + totalFilesCreated);
            } catch(EOFException oef) {
                if(tmplist.size()>0) {
                    files.add(sortAndSave(tmplist,cmp));
                    tmplist.clear();
                }
            }
        } finally {
            fbr.close();
        }
        System.out.println("Total files created: " + totalFilesCreated);
        return files;
    }


    public static File sortAndSave(List<String> tmplist, Comparator<String> cmp) throws IOException  {
        Collections.sort(tmplist,cmp);
        File newtmpfile = File.createTempFile("sortInBatch", "flatfile");
        newtmpfile.deleteOnExit();
        BufferedWriter fbw = new BufferedWriter(new FileWriter(newtmpfile));
        try {
            for(String r : tmplist) {
                fbw.write(r);
                fbw.newLine();
            }
        } finally {
            fbw.close();
        }
        return newtmpfile;
    }

    public static int mergeSortedFiles(List<File> files, File outputfile, Comparator<String> cmp) throws IOException {
        PriorityQueue<BinaryFileBuffer> pq = new PriorityQueue<BinaryFileBuffer>();
        for (File f : files) {
            BinaryFileBuffer bfb = new BinaryFileBuffer(f,cmp);
            pq.add(bfb);
        }
        BufferedWriter fbw = new BufferedWriter(new FileWriter(outputfile));
        int rowcounter = 0;
        try {
            while(pq.size()>0) {
                BinaryFileBuffer bfb = pq.poll();
                String r = bfb.pop();
                fbw.write(r);
                fbw.newLine();
                ++rowcounter;
                if(bfb.empty()) {
                    bfb.fbr.close();
                    bfb.originalfile.delete();
                } else {
                    pq.add(bfb);
                }
            }
        } finally {
            fbw.close();
        }
        return rowcounter;
    }

}


class BinaryFileBuffer implements Comparable<BinaryFileBuffer>{
    public static int BUFFERSIZE = 512;
    public BufferedReader fbr;
    private List<String> buf = new ArrayList<>();
    int currentpointer = 0;
    Comparator<String> mCMP;
    public File originalfile;

    public BinaryFileBuffer(File f, Comparator<String> cmp) throws IOException {
        originalfile = f;
        mCMP = cmp;
        fbr = new BufferedReader(new FileReader(f));
        reload();
    }

    public boolean empty() {
        return buf.size()==0;
    }

    private void reload() throws IOException {
        buf.clear();
        try {
            String line;
            while((buf.size()<BUFFERSIZE) && ((line = fbr.readLine()) != null))
                buf.add(line);
        } catch(EOFException oef) {
        }
    }

    public String peek() {
        if(empty()) return null;
        return buf.get(currentpointer);
    }

    public String pop() throws IOException {
        String answer = peek();
        ++currentpointer;
        if(currentpointer == buf.size()) {
            reload();
            currentpointer = 0;
        }
        return answer;
    }

    public int compareTo(BinaryFileBuffer b) {
        return mCMP.compare(peek(), b.peek());
    }
}