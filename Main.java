
import java.io.BufferedReader;
import java.io.File;

import java.io.FileReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;


public class Main {
	/**
	 * declare variables
	 */
	private static BufferedReader fr, br;
	private static PrintWriter pw;
	static File inname, nfile;
	static String imgName, fileName, site, ISBN;
	static int imgNum = 0, fileNum = 0;
	private static Date date = new Date();
	private static Statement myST;
	//private static File nfile;
	public static void main(String[] args) throws Exception{
		//if argument length is zero, jfilechoose both input and output files
		if(args.length == 0){
			inname = file();
			pw = new PrintWriter(file());
		}
		//if argument length is 1 than use jFilechoose for output file
		else if(args.length == 1){
			inname = new File(args[0]);
			pw = new PrintWriter(file());
		}
		//if both argument are passed then just take them as input and output
		else{
			inname = new File(args[0]);
			pw = new PrintWriter(args[1]);
		}
		/**
		 * set up connection to database and create the tables
		 */
		Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/pro?autoReconnect=true&useSSL=false" , "root" , "root");
		Statement myST = con.createStatement();
		String create = "CREATE TABLE IF NOT EXISTS BOOKS " +
						"(bookid int auto_increment primary key, "
						+ "ISBN varchar(15) UNIQUE, "
						+ "Anamefirst varchar(25), "
						+ "Anamelast varchar(30), "
						+ "title varchar(100), "
						+ "yearpub date, "
						+ "publisher varchar(100));";
		String creatCon = "CREATE TABLE IF NOT EXISTS BOOKCOND "
						  + "(pk int auto_increment primary key,"
						  + "ISB varchar(15),"
						  + "foreign key (ISB) references books(ISBN) ON DELETE CASCADE ON UPDATE CASCADE, "
						  + "cond varchar(15), "
						  + "price varchar(10) );";
		myST.execute(create);
		myST.execute(creatCon);
		/**
		 * open input file and read in the ISBN
		 */
		fr = new BufferedReader(new FileReader(inname));
		ISBN = fr.readLine();
		while(ISBN != null){
			findData(ISBN);
			ISBN = fr.readLine();
		}
		fr.close();
		pw.close();
		/**
		 * create GUI
		 */
		Swing sw = new Swing();
		sw.create();
	}
	/**
	 * find the data of a given ISBN book and store it in mySQL
	 * write to outfile the ISBN and ate it was entered into the server
	 */
	public static void findData(String ISBN) throws Exception{
		insert("books","ISBN", ISBN);
		pw.println(ISBN + " added on " + date.toString());
		if(ISBN.length() == 10){
			site = "https://www.amazon.com/dp/" + ISBN;
		}
		else{
			/**
			 * UPC, gets the asin then proceed
			 */
			site = "https://www.amazon.com/s?field-keywords=" + ISBN;
			nfile = new File(ISBN+"ASIN.html");
			nfile.createNewFile();
			User_Agent.main(site, nfile);
			br = new BufferedReader(new FileReader(nfile));
			String line = br.readLine();
			while(line != null){
				if(line.contains("data-asin")){
					String asin = line.substring(line.indexOf("data-asin=\"")+11, line.indexOf("data-asin=\"")+21);
					site = "https://www.amazon.com/s?field-keywords=" + asin;
					break;
					}
				line = br.readLine();
				}
			br.close();
			}
		/**
		 * create a temp file of the file and scan through it
		 */
			nfile = new File(ISBN+".html");
			nfile.createNewFile();
			User_Agent.main(site, nfile);
			br = new BufferedReader(new FileReader(nfile));
			String data = br.readLine();
			while(data != null){
				if(data.contains("<meta name=\"description\"")){
					String title = data.substring(data.indexOf("content=")+9, data.indexOf("["));
					update("title", title, ISBN);
					String author = data.substring(data.indexOf("[")+1, data.indexOf("]"));
					author = author.replaceAll(",", "");
					String[] names = author.split(" ");
					update("Anamefirst", names[0], ISBN);
					update("Anamelast", names[1], ISBN);
				}
				else if(data.contains("Product Details")){
					while(true){
						while(!data.contains("<li>")){
							data = br.readLine();
						}
						while(data.contains("<li>") && !data.contains("</li>")){
							data = data + br.readLine();
						}
						String cat = data.substring(data.indexOf("<li><b>")+7, data.indexOf("</b>")-1).trim();
						String catdata = "";
						if(data.contains("Lexile Measure") || data.contains("Shipping Weight")){
							catdata = data.substring(data.indexOf("</b>")+4, data.indexOf("</li>")).trim();
							catdata = catdata.substring(0, catdata.indexOf("(")).trim();
						}
						else if(data.contains("Publisher")){
							String dateofpub = data.substring(data.indexOf("(")+1, data.indexOf(")"));
							String[] dat = dateofpub.split(" ");
							dat[0] = datetoInt(dat[0]);
							dateofpub = dat[2] + "-" + dat[0] + "-" + dat[1];
							catdata = data.substring(data.indexOf("</b>")+4, data.indexOf("</li>")).trim();
							update("yearpub", dateofpub,ISBN);
						}
						else{
							catdata = data.substring(data.indexOf("</b>")+4, data.indexOf("</li>")).trim();
						}
						cat = cat.replaceAll(":", "");
						update(cat, catdata, ISBN);
						if(data.contains("Shipping Weight")) break;
						data = br.readLine();
					}
				}
				else if(data.contains("<span class=\"a-color-secondary\">List Price:")){
					String price = data.substring( data.indexOf("$"));
					price = price.substring(0, price.length()-7);
					update("ListPrice", price, ISBN);
				}
				else if(data.contains("<span class=\"olp-")){
					while(!data.contains("</span>")){
						data = data + br.readLine();
					}
					String conp = data.substring(data.indexOf("$"));
					if(data.contains("olp-used")){
						insertCon(ISBN, "used", conp);
					}
					else if(data.contains("olp-new")){
						insertCon(ISBN, "new", conp);
					}
					else if(data.contains("olp-collectible")){
						insertCon(ISBN, "collectible", conp);
					}
				}
				data = br.readLine();
			}
			br.close();			

	}
	/**
	 * translate string month to its corresponding numbers
	 * @param mon - what is the month to translate
	 * @return
	 */
	public static String datetoInt(String mon){
		String temp = "";
		String[] month = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
		for(int i =0; i < 12; i++){
			if(mon.equalsIgnoreCase(month[i])){
				temp = Integer.toString(i+1);
			}
		}
		return temp;
	
	}
	//to get the input/output file using jFilechooser if no input/output
	public static File file(){
    	JFileChooser fc = new JFileChooser();
    	int result = fc.showOpenDialog(null);
    	
    	if(result == JFileChooser.APPROVE_OPTION){
    		String ss = fc.getSelectedFile().getAbsolutePath();
    		File file = fc.getSelectedFile();
    		return file;
    	}
    	else{
    		return null;
    	}
    }
	/**
	 * insert into database the informations
	 * @param base - which table in database to insert
	 * @param column - what column name
	 * @param data - what is the information to insert
	 */
	public static void insert(String base, String column, String data){
		try{
			String insert = "insert into " + base + "(" + column + ")"
						+ "values('" + data + "')";
			myST.executeUpdate(insert);
		}catch(Exception e){
			
		}
		
	}
	/**
	 * insert into table bookcond
	 * @param ISBN - ISBN to insert
	 * @param cond - new, used, or collectible
	 * @param price - price for the condition
	 */
	public static void insertCon(String ISBN, String cond, String price) {
		try {
			String insert = "insert into BOOKCOND ( ISB, cond, price)"
					+ " values('" + ISBN + "', '" + cond + "', '" + price + "')";
			myST.executeUpdate(insert);
		} catch (Exception e) {
			
		}
	}
/**
 * delete from table books
 * @param column - column name
 * @param data - column data
 */
	public static void delete(String column, String data) {
		try{
			String delete = "delete from books where " + column + " = " + data;
			myST.execute(data);
		}catch (Exception e){
			
		}
	}
	/**
	 * look up the given ISBN book in the database
	 * @param ISBN - the ISBN to delete
	 */
	public static void search(String ISBN){
		try{
			String search = " select * from books where ISBN = " + ISBN;
			myST.execute(search);
		}catch (Exception e){
			
		}
		
	}
	
	/**
	 * create the column if it doesn't exist
	 * else put data into the column
	 * @param column - column name
	 * @param data - column information
	 * @param ISBN - ISBN number
	 * @throws SQLException
	 */
	public static void update(String column, String data, String ISBN){
		column = column.replaceAll(" ", "");
		column = column.replaceAll("-", "");
		try{
			int successorNot = myST.executeUpdate("ALTER TABLE BOOKS ADD " + column + " varchar(100)");
			String update = "update books set " + column + "= '" + data + "' where ISBN = " + ISBN;
			myST.executeUpdate(update);
		}
		catch(Exception e){
			
		}
	}
	
	/**
	 * create a GUI for insert, delete, and search of mysql
	 *
	 */
	public static class Swing{

		private JButton insert = new JButton("Insert");
		private JButton delete = new JButton("Delete");
		private JButton search = new JButton("Search");
		private JButton admin = new JButton("Admin");
		//private JButton show = new JButton("show");
		public void create(){
			
			JFrame frame = new JFrame("Book Database");
			
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			FlowLayout layout = new FlowLayout();
			frame.setLayout(layout);
			frame.add(insert);
			frame.add(delete);
			frame.add(search);
			frame.add(admin);
			//frame.add(show);
			frame.setSize(300,100);
			frame.setVisible(true);
			
			insert.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent event) {			
					String ISBN = JOptionPane.showInputDialog(null, "Enter the ISBN number to insert:");
					if(ISBN != null){
						try {
							findData(ISBN);
						} catch (Exception e) {
							
						}
					}
					
				}
			});
			
			delete.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					String ISBN = JOptionPane.showInputDialog(null, "Enter the ISBN numebr to delete:");
					if(ISBN != null){
						try {
							delete("ISBN", ISBN);
						} catch (Exception e) {
							
						}
					}
				}
			});
			
			search.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					String sea = JOptionPane.showInputDialog(null, "Enter an ISBN to search:");
					if(sea != null){
						try{
							search(sea);
						}catch (Exception e){
							
						}
					}
				}
			});
			admin.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					String ok = JOptionPane.showInputDialog(null, "Enter the password");
					if(ok.equals("12345")){
						JOptionPane.showMessageDialog(null, "good password");
					}else{
						JOptionPane.showMessageDialog(null, "Wrong password");
					}
				}
			});
			/*show.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					
				}
			});*/
		}
	}
}