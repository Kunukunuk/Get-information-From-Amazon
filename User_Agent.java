import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class User_Agent{

         private static String webpage = null;
         public static final String USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2) Gecko/20100115 Firefox/3.6";
         private static PrintWriter pw;
         public static InputStream getURLInputStream(String sURL) throws Exception {
               URLConnection oConnection = (new URL(sURL)).openConnection();
               oConnection.setRequestProperty("User-Agent", USER_AGENT);
               return oConnection.getInputStream();
         } // getURLInputStream

         public static BufferedReader read(String url) throws Exception {
               InputStream content = (InputStream)getURLInputStream(url);
               return new BufferedReader (new InputStreamReader(content));
         } // read

       public static void main (String name, File outfile) throws Exception{
    		   //get the name of the file
              webpage = name;
              //open the new output file
              pw = new PrintWriter(outfile);
              //write the content to the new outputfile
              pw.println("Contents of the following URL: "+webpage+"\n");
              //open the url
              BufferedReader reader = read(name);
              //read in the content
              String line = reader.readLine();
              int count = 1;
              //write all the content to a new outputfile
              while (line != null) {
                     pw.println(line);
                     line = reader.readLine();
                     count++;
              } // while
              //write the number of lines to output file
              pw.println("Number of lines: " + count);
              pw.println("here");
              pw.close();

       } // main
} // WebpageReaderWithAgent