package ru.sao.solar.coronaljets.catalog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sao.solar.coronaljets.utils.MyUtils;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Settings {

	//private final static String RESOURCES_PATH = "D:/Projects/Coronal jets/dbfillapp/resources/";
	public final static String RESOURCES_PATH = "/main/coronal-jets/db-fill-app/resources/";

	private final int NUM_THREADS = 8;

	final static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private Boolean remoteConnection;

	/**
	 * Input Paths + Files
	 */
	private String dataPathProcessing;
	private String eventsCatalogTableXLSX;
	private String eventsConfigPath;
	
	/**
	 * Output
	 */
	private String processingREF;

	/**
	 * MySQL
	 */
	private String connectionURL;
	private String databasename;

	private String eventsTable;

	private String wavesTable;

	private String detailsTable;

	private int port;
	private String hostname;
	private String username;
	private String password;
	
	private boolean sqlTestRun;

	/**
	 * Association parameters
	 */
	private double xcTolerance;
	private double ycTolerance;
	private long tstartToleranceSec;
	private long tendToleranceSec;
	
	/**
	 * Other
	 */
	private boolean noConfirm;
	
	private Date minDatetime;
	private Date maxDatetime;

	public Settings() {

	}

	public Settings init() {
		
		//important
		TimeZone.setDefault(TimeZone.getTimeZone("Etc/UTC"));
		Locale.setDefault(Locale.US);

		
		
		DateFormat df = new SimpleDateFormat("HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));

		List<String> lines = readSettingsIni();
		
		for (String line : lines) {

			//trim -- remove whitespaces from beginning and end
			line = line.replaceAll("\\\\", "/").trim();
			
			//events conf
			Pattern patternEventsConfigPath = Pattern.compile("eventsConfigPath\\s*=\\s*(.*)");
			Matcher matcherEventsConfigPath = patternEventsConfigPath.matcher(line);
			if (matcherEventsConfigPath.find()) {
				//remove quotations
				eventsConfigPath = matcherEventsConfigPath.group(1).replaceAll("^\"|\"$", "");
			}

			//datapath
			Pattern patternDataPathProcessing = Pattern.compile("dataPathProcessing\\s*=\\s*(.*)");
			Matcher matcherDataPathProcessing = patternDataPathProcessing.matcher(line);
			if (matcherDataPathProcessing.find()) {
				//remove quotations
				dataPathProcessing = matcherDataPathProcessing.group(1).replaceAll("^\"|\"$", "");
			}

			Pattern patternECT = Pattern.compile("events_catalogTable\\s*=\\s*(.*)");
			Matcher matcherECT = patternECT.matcher(line);
			if (matcherECT.find()) {
				//remove quotations
				eventsCatalogTableXLSX = matcherECT.group(1).replaceAll("^\"|\"$", "");
			}

			Pattern patternUsername = Pattern.compile("username\\s*=\\s*(.*)");
			Matcher matcherUsername = patternUsername.matcher(line);
			if (matcherUsername.find()) {
				//remove quotations
				username = matcherUsername.group(1).replaceAll("^\"|\"$", "");
			}

			Pattern patternPassword = Pattern.compile("password\\s*=\\s*(.*)");
			Matcher matcherPassword = patternPassword.matcher(line);
			if (matcherPassword.find()) {
				//remove quotations
				password = matcherPassword.group(1).replaceAll("^\"|\"$", "");
			}

			Pattern patternDatabase = Pattern.compile("database\\s*=\\s*(.*)");
			Matcher matcherDatabase = patternDatabase.matcher(line);
			if (matcherDatabase.find()) {
				//remove quotations
				databasename = matcherDatabase.group(1).replaceAll("^\"|\"$", "");
			}

			Pattern patternEventsTable = Pattern.compile("eventsTable\\s*=\\s*(.*)");
			Matcher matcherEventsTable = patternEventsTable.matcher(line);
			if (matcherEventsTable.find()) {
				//remove quotations
				eventsTable = matcherEventsTable.group(1).replaceAll("^\"|\"$", "");
			}

			Pattern patternWavesTable = Pattern.compile("wavesTable\\s*=\\s*(.*)");
			Matcher matcherWavesTable = patternWavesTable.matcher(line);
			if (matcherWavesTable.find()) {
				//remove quotations
				wavesTable = matcherWavesTable.group(1).replaceAll("^\"|\"$", "");
			}

			Pattern patternDetailsTable = Pattern.compile("detailsTable\\s*=\\s*(.*)");
			Matcher matcherDetailsTable = patternDetailsTable.matcher(line);
			if (matcherDetailsTable.find()) {
				//remove quotations
				detailsTable = matcherDetailsTable.group(1).replaceAll("^\"|\"$", "");
			}

			Pattern patternPort = Pattern.compile("port\\s*=\\s*(.*)");
			Matcher matcherPort = patternPort.matcher(line);
			if (matcherPort.find()) {
				//remove quotations
				port = Integer.parseInt(matcherPort.group(1));
			}

			Pattern patternAddress = Pattern.compile("hostname\\s*=\\s*(.*)");
			Matcher matcherAddress = patternAddress.matcher(line);
			if (matcherAddress.find()) {
				//remove quotations
				hostname = matcherAddress.group(1).replaceAll("^\"|\"$", "");
			}

			Pattern patternXcTolerance = Pattern.compile("xc_tolerance\\s*=\\s*(.*)");
			Matcher matcherXcTolerance = patternXcTolerance.matcher(line);
			if (matcherXcTolerance.find()) {
				//remove quotations
				xcTolerance = Double.parseDouble(matcherXcTolerance.group(1));
			}

			Pattern patternYcTolerance = Pattern.compile("yc_tolerance\\s*=\\s*(.*)");
			Matcher matcherYcTolerance = patternYcTolerance.matcher(line);
			if (matcherYcTolerance.find()) {
				//remove quotations
				ycTolerance = Double.parseDouble(matcherYcTolerance.group(1));
			}

			Pattern patternTstartTolerance = Pattern.compile("tstart_tolerance\\s*=\\s*(.*)");
			Matcher matcherTstartTolerance = patternTstartTolerance.matcher(line);
			if (matcherTstartTolerance.find()) {

				String str = matcherTstartTolerance.group(1).replaceAll("^\"|\"$", "");
				Date date = null;
				try {
					date = df.parse(str);
				} catch (ParseException ex) {
					logger.error(ex);
				}

				long seconds = date.getTime() / 1000L;
				tstartToleranceSec = seconds;
			}

			Pattern patternTendTolerance = Pattern.compile("tend_tolerance\\s*=\\s*(.*)");
			Matcher matcherTendTolerance = patternTendTolerance.matcher(line);
			if (matcherTendTolerance.find()) {

				String str = matcherTendTolerance.group(1).replaceAll("^\"|\"$", "");
				Date date = null;
				try {
					date = df.parse(str);
				} catch (ParseException ex) {
					logger.error(ex);
				}

				long seconds = date.getTime() / 1000L;
				tendToleranceSec = seconds;
			}
			
			Pattern patternProcessingREF = Pattern.compile("processingREF\\s*=\\s*(.*)");
			Matcher matcherProcessingREF = patternProcessingREF.matcher(line);
			if (matcherProcessingREF.find()) {
				//remove quotations
				processingREF = matcherProcessingREF.group(1).replaceAll("^\"|\"$", "");
			}
			
			Pattern patternNoConfirm = Pattern.compile("no_confirm\\s*=\\s*(.*)");
			Matcher matcherNoConfirm = patternNoConfirm.matcher(line);
			if (matcherNoConfirm.find()) {
				noConfirm = Boolean.valueOf(matcherNoConfirm.group(1));
			}
			
			Pattern patternSqlTestRun = Pattern.compile("sqlTestRun\\s*=\\s*(.*)");
			Matcher matcherSqlTestRun = patternSqlTestRun.matcher(line);
			if (matcherSqlTestRun.find()) {
				sqlTestRun = Boolean.valueOf(matcherSqlTestRun.group(1));
			}
			
			
			/**
			 * time_interval
			 */
			
			
			Pattern patternTimeInterval = Pattern.compile("time_interval\\s*=\\s*(.*)");
			Matcher matcherTimeInterval = patternTimeInterval.matcher(line);
			if (matcherTimeInterval.find()) {
				//remove quotations
				String res = matcherTimeInterval.group(1).replaceAll("^\"|\"$", "");
				
				String regex1 = "\\,";
			    Pattern pattern1 = Pattern.compile(regex1);
				
				//dates
				String dateLine = res.replaceAll(" ", "");
				
				//split by comma
				String[] fields0 = pattern1.split(dateLine);
				
				for (String splittedDateLine : fields0) {
					
					if (splittedDateLine.matches("(?i)" + "all") || splittedDateLine.matches("") ) {
						Calendar cal = Calendar.getInstance();
						cal.set(Calendar.YEAR, 1970);
						cal.set(Calendar.DAY_OF_YEAR, 1);
						cal.set(Calendar.HOUR_OF_DAY, 0);
						cal.set(Calendar.MINUTE, 0);
						cal.set(Calendar.SECOND, 0);
						minDatetime = cal.getTime();

						maxDatetime  = new Date();
					}
					
					if (splittedDateLine.matches("\\d{4}")) {

						Pattern patternYear = Pattern.compile("(\\d{4})");
						Matcher matches = patternYear.matcher(splittedDateLine);
						while (matches.find()) {
							String year = matches.group(1);
							
							Calendar cal = Calendar.getInstance();
							cal.set(Calendar.YEAR, Integer.valueOf(year));
							cal.set(Calendar.DAY_OF_YEAR, 1);
							cal.set(Calendar.HOUR_OF_DAY, 0);
							cal.set(Calendar.MINUTE, 0);
							cal.set(Calendar.SECOND, 0);
							minDatetime = cal.getTime();
							
							Calendar cal1 = Calendar.getInstance();
							cal1.set(Calendar.YEAR, Integer.valueOf(year));
							cal1.set(Calendar.MONTH, 11); // 11 = december
							cal1.set(Calendar.DAY_OF_MONTH, 31); // new years eve
							cal1.set(Calendar.HOUR_OF_DAY, 23);
							cal1.set(Calendar.MINUTE, 59);
							cal1.set(Calendar.SECOND, 59);
							maxDatetime  = cal1.getTime();
						}

					}
					
					if (splittedDateLine.matches("\\d{8}")) {
						
						Pattern patternSD = Pattern.compile("(\\d{8})");
						Matcher matches = patternSD.matcher(splittedDateLine);
						while (matches.find()) {
							String dateDay = matches.group(1);
							
							Date date = MyUtils.stringToDate(dateDay, "yyyyMMdd");
							
							Calendar calendar = Calendar.getInstance();
							calendar.setTime(date);
							calendar.set(Calendar.HOUR_OF_DAY, 0);
							calendar.set(Calendar.MINUTE, 0);
							calendar.set(Calendar.SECOND, 0);
							minDatetime = calendar.getTime();
							
							Calendar calendar1 = Calendar.getInstance();
							calendar1.setTime(date);
							calendar1.set(Calendar.HOUR_OF_DAY, 23);
							calendar1.set(Calendar.MINUTE, 59);
							calendar1.set(Calendar.SECOND, 59);
							maxDatetime = calendar1.getTime();
						}

					}
					
					if (splittedDateLine.matches("\\d{4}-\\d{4}")) {

						Pattern patternID = Pattern.compile("(\\d{4})-(\\d{4})");
						Matcher matches = patternID.matcher(splittedDateLine);
						while (matches.find()) {
							String startDateStr = matches.group(1);
							Date date1 = MyUtils.stringToDate(startDateStr, "yyyy");
							String endDateStr = matches.group(2);
							Date date2 = MyUtils.stringToDate(endDateStr, "yyyy");
							
							Calendar calendar = Calendar.getInstance();
							calendar.setTime(date1);
							calendar.set(Calendar.DAY_OF_YEAR, 1);
							calendar.set(Calendar.HOUR_OF_DAY, 0);
							calendar.set(Calendar.HOUR_OF_DAY, 0);
							calendar.set(Calendar.MINUTE, 0);
							calendar.set(Calendar.SECOND, 0);
							minDatetime = calendar.getTime();
							
							Calendar calendar1 = Calendar.getInstance();
							calendar1.setTime(date2);
							calendar1.set(Calendar.MONTH, 11); // 11 = december
							calendar1.set(Calendar.DAY_OF_MONTH, 31); // new years eve
							calendar1.set(Calendar.HOUR_OF_DAY, 23);
							calendar1.set(Calendar.MINUTE, 59);
							calendar1.set(Calendar.SECOND, 59);
							maxDatetime = calendar1.getTime();
							
							if (minDatetime.after(maxDatetime)) {
								logger.error("Error: ", new Exception("Wrong time interval, from " + minDatetime + " to " + maxDatetime));
								throw new RuntimeException();
							}

							/*Date startDate = MyUtils.stringToDate(startDateStr, "yyyyMMdd");
							Date endDate = MyUtils.stringToDate(endDateStr, "yyyyMMdd");
							
							if ((fileDate.after(startDate) || fileDate.equals(startDate))
									&& (fileDate.before(endDate) || fileDate.equals(endDate))) {
								dateMatches = true;
							}*/
						}
					}
					
					if (splittedDateLine.matches("\\d{8}-\\d{8}")) {

						Pattern patternID = Pattern.compile("(\\d{8})-(\\d{8})");
						Matcher matches = patternID.matcher(splittedDateLine);
						while (matches.find()) {
							String startDateStr = matches.group(1);
							Date date1 = MyUtils.stringToDate(startDateStr, "yyyyMMdd");
							String endDateStr = matches.group(2);
							Date date2 = MyUtils.stringToDate(endDateStr, "yyyyMMdd");
							
							Calendar calendar = Calendar.getInstance();
							calendar.setTime(date1);
							calendar.set(Calendar.HOUR_OF_DAY, 0);
							calendar.set(Calendar.MINUTE, 0);
							calendar.set(Calendar.SECOND, 0);
							minDatetime = calendar.getTime();
							
							Calendar calendar1 = Calendar.getInstance();
							calendar1.setTime(date2);
							calendar1.set(Calendar.HOUR_OF_DAY, 23);
							calendar1.set(Calendar.MINUTE, 59);
							calendar1.set(Calendar.SECOND, 59);
							maxDatetime = calendar1.getTime();
							
							if (minDatetime.after(maxDatetime)) {
								logger.error("Error: ", new Exception("Wrong time interval, from " + minDatetime + " to " + maxDatetime));
								throw new RuntimeException();
							}

							/*Date startDate = MyUtils.stringToDate(startDateStr, "yyyyMMdd");
							Date endDate = MyUtils.stringToDate(endDateStr, "yyyyMMdd");
							
							if ((fileDate.after(startDate) || fileDate.equals(startDate))
									&& (fileDate.before(endDate) || fileDate.equals(endDate))) {
								dateMatches = true;
							}*/
						}
					}
					
				}
			}
			
			//String regex = "\\t";
		    //Pattern pattern = Pattern.compile(regex);
		    
		    
		    
		    //line = line.trim().replaceAll(" +", " ");
			
			//String[] fields = pattern.split(line);
			
			
			//System.out.println(line);
		}

		remoteConnection = false;
		if ( MyUtils.isValidURL(dataPathProcessing) ) {
			remoteConnection = true;
		}

		connectionURL = "jdbc:mysql://" + hostname + ":" + port + "/" + databasename; //jdbc:mysql://hostname:port/databasename

		return this;
	}
	
	public Date minDatetime() {
		return minDatetime;
	}
	
	public Date maxDatetime() {
		return maxDatetime;
	}
	
	public int numThreads() {
		return NUM_THREADS;
	}

	public static String resourcesPath() {
		return RESOURCES_PATH;
	}

	public boolean remoteConnection() {
		return remoteConnection;
	}

	public boolean noConfirm() {
		return noConfirm;
	}
	
	public boolean sqlTestRun() {
		return sqlTestRun;
	}	
	
	public String processingREF() {
		return processingREF;
	}
	
	public String eventsConfigPath() {
		return eventsConfigPath;
	}

	public String dataPathProcessing() {
		return dataPathProcessing;
	}

	public String eventsCatalogTable() {
		return eventsCatalogTableXLSX;
	}

	public String username() {
		return username;
	}

	public String password() {
		return password;
	}

	public String databasename() {
		return databasename;
	}

	public String eventsTable(){
		return eventsTable;
	}

	public String wavesTable(){
		return wavesTable;
	}

	public String detailsTable(){
		return detailsTable;
	}

	public String hostname() {
		return hostname;
	}

	public int port() {
		return port;
	}

	public double xcTolerance() {
		return xcTolerance;
	}

	public double ycTolerance() {
		return ycTolerance;
	}

	public long tstartToleranceSec() {
		return tstartToleranceSec;
	}

	public long tendToleranceSec() {
		return tendToleranceSec;
	}

	public void testConnection() {

		Connection conn = this.getDBConnection();
		if (conn != null) {
			logger.info("MySQL: Connected");
		} else {
			logger.error("MySQL Connection Failed");
			throw new RuntimeException();
		}
	}

	public Connection getDBConnection() {
		Connection conn = null; //sets values to null before actually attempting a connection               
		//System.out.println("Connecting database...");

		//Loading driver...
		//System.out.println("Loading driver...");
		try {
			Class.forName("com.mysql.jdbc.Driver");
			//System.out.println("Driver loaded!");
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Cannot find the driver in the classpath!", e);
		}

		try {
			conn = DriverManager.getConnection(connectionURL, username, password); //username, password
			if (conn == null) {
				//check to make sure that it actually connected
				logger.error("MySQL Connection Failed");
				throw new RuntimeException();
			}

			/*if (!conn.isClosed()) {
				conn.close();
			}*/
		} catch (Exception ex) {
			//ex.printStackTrace();
			logger.error("MySQL Connection Failed");
			logger.error(ex.getMessage(), ex);
			throw new RuntimeException();
		}
		return conn;
	}

	private List<String> readSettingsIni() {

		//remove comments
		//remove whitespaces only outside quotes !!!
		//line delimeters: \n, ;

		//read section

		List<String> lines = new ArrayList<String>();
		String settingsFile = Settings.resourcesPath() + "config/settings.ini";

		try {
			InputStream inputStream = new FileInputStream(settingsFile); //absolute path
			//InputStream inputStream = MethodHandles.lookup().lookupClass().getResourceAsStream("/" + settingsFile); //relative path
			if (inputStream == null) {
				logger.error("Error: ", new Exception("File: " + settingsFile + " not found"));
				throw new RuntimeException();
			}
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

			String line = null;

			while ((line = bufferedReader.readLine()) != null) {

				if (line.length() > 0) {
					//line = line.replaceAll("(?m)^[ \t]*\r?\n", "");

					int lineEnd = line.indexOf("#");

					String subString;

					if (lineEnd != -1) {
						if (lineEnd != 0) {
							subString = line.substring(0, lineEnd);
							lines.add(subString);
						}
					} else {
						lines.add(line);
					}
				}

			}

			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}

}
