package ru.sao.solar.coronaljets.catalog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import ru.sao.solar.coronaljets.utils.Excel;
import ru.sao.solar.coronaljets.utils.MyUtils;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;

public class EventProcessingTask2 implements Callable<Integer> {

	final static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private Settings settings;
	private File eventConfDir;
	private File eventProcessingDir;
	private LinkedHashMap<String, String[]> mapXLSX;
	//private ProgressBarConsole pb;

	public EventProcessingTask2(File eventConfDir, Settings settings, LinkedHashMap<String, String[]> mapXLSX) { //, ProgressBarConsole pb
		this.settings = settings;
		this.eventConfDir = eventConfDir;
		this.mapXLSX = mapXLSX;
		//this.eventProcessingDir = new File(settings.dataPathProcessing() + "/" + eventConfDir);
		//this.pb = pb;
	}

	@Override
	public Integer call() throws Exception {

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));

		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf1.setTimeZone(TimeZone.getTimeZone("UTC"));

		Connection conn = settings.getDBConnection();

		/**
		 * list all files in event conf dir
		 */
		List<File> files = new ArrayList<File>();
		MyUtils.listAllFilesInDir(eventConfDir, files);
		//MyUtils.printList(files);

		String fileConfigJSON = null;
		for (File file : files) {
			if (String.valueOf(file).endsWith("config.json")) {
				fileConfigJSON = String.valueOf(file);
			}
		}
		if (fileConfigJSON == null) {
			logger.warn("config.json" + " not found in: " + eventConfDir);
		} else {

			/**
			 * events reading config.json
			 */
			Event event = readConfigJSON(new File(fileConfigJSON), settings);

			/**
			 * association with event xlsx
			 */
			event.associate(mapXLSX, settings);

			/*logger.info("Folder " + eventNum + " of " + dirCount + " -----------");
			logger.info("TSTART: " + df.format(event.tstart()) + " && xarc:" + MyUtils.round(event.xarc(), 0)
					+ " && yarc:" + MyUtils.round(event.yarc(), 0));
			*/

			SimpleDateFormat dfYear = new SimpleDateFormat("yyyy");
			String year = dfYear.format(event.tstart());
			
			/*
			 * more test
			 */
			
			DateFormat dfDir = new SimpleDateFormat("yyyyMMdd_HHmmss"); 
			String eventDir_name = dfDir.format(event.tstartProcessing()) + "_" +
					dfDir.format(event.tendProcessing()) + "_" + (int) event.xarc() + "_" + (int)
					event.yarc() + "_" + event.wpixProcessing() + "_" + event.hpixProcessing();
			
			this.eventProcessingDir = new File(settings.dataPathProcessing() + "/" + year + "/" + eventDir_name);
			
			logger.info("-----------------");
			logger.info("Event: " + eventProcessingDir.getName() + ":");
			
			if (!eventProcessingDir.exists()) {
				//logger.warn("-----------------");
				//logger.warn("Event: " + eventProcessingDir.getName() + ":");
				logger.warn("Warning: No processing data existing for event config.json file: '" + eventConfDir.getAbsolutePath() + "'");
				logger.warn("Processing data expected in: '" + eventProcessingDir.getAbsolutePath() + "'");
				//logger.warn("-----------------");
				//throw new Exception("No processsing data");
			}
			
			//logger.info("Proc: " + eventProcessingDir);
			
			/*
			 * coping config.json's
			 * test. temporary. a crutch!!!
			 * 
			 */
			
			/*
			 * DateFormat dfDir = new SimpleDateFormat("yyyyMMdd_HHmmss"); String
			 * eventDir_name = dfDir.format(event.tstartProcessing()) + "_" +
			 * dfDir.format(event.tendProcessing()) + "_" + (int) event.xarc() + "_" + (int)
			 * event.yarc() + "_" + event.wpixProcessing() + "_" + event.hpixProcessing();
			 * 
			 * logger.debug("DirName:" + eventDir_name);
			 * 
			 * String pathToEventsConf = "/main/coronal-jets/data/events-conf"; String
			 * pathToProcessing = "/main/coronal-jets/data/processing";
			 * 
			 * File eventConfDir = new File(pathToEventsConf + "/" + year + "/" +
			 * eventDir_name); if (!eventConfDir.exists()){ eventConfDir.mkdirs(); }
			 * 
			 * //copy
			 * 
			 * Path copied = Paths.get(pathToEventsConf + "/" + year + "/" + eventDir_name +
			 * "/" + "config.json"); Path originalPath = new File(pathToProcessing + "/" +
			 * year + "/" + eventDir_name + "/" + "objects_m2/" + "config.json").toPath();
			 * Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);
			 * 
			 * logger.debug("Copied:" + pathToEventsConf + "/" + year + "/" + eventDir_name
			 * + "/" + "config.json"); logger.debug("Orig:" + pathToProcessing + "/" + year
			 * + "/" + "config.json");
			 */

		    //assertThat(copied).exists();
		    //assertThat(Files.readAllLines(originalPath)
		    //  .equals(Files.readAllLines(copied)));
			
			//endtest
			
			//sql test run
			
			String beforeSQL = "START TRANSACTION; ";
			String afterSQL = " COMMIT; ROLLBACK;";

			/**
			 * insert to database
			 */

			String query = "insert into " + settings.eventsTable() + " (tstart, tend, tref, xarc, yarc, lat, lon, source, sourceref)" + //, comment
					" values (?, ?, ?, ?, ?, ?, ?, ?, ?)" + "ON DUPLICATE KEY UPDATE " + "tstart=VALUES(tstart), "
					+ "tend=VALUES(tend), " + "tref=VALUES(tref), " + "xarc=VALUES(xarc), " + "yarc=VALUES(yarc), "
					+ "lat=VALUES(lat), " + "lon=VALUES(lon), " + "source=VALUES(source), "
					+ "sourceref=VALUES(sourceref);";
			
			if (settings.sqlTestRun()) {
				query = beforeSQL + query + afterSQL;
			}
			//"comment=VALUES(comment);

			// create the mysql insert preparedstatement
			PreparedStatement preparedStmt = null;
			try {
				preparedStmt = conn.prepareStatement(query);
				preparedStmt.setTimestamp(1, new java.sql.Timestamp(event.tstart().getTime()));
				preparedStmt.setTimestamp(2, new java.sql.Timestamp(event.tend().getTime()));

				/*if (event.trefProcessing() != null) {
					preparedStmt.setTimestamp(3, new java.sql.Timestamp(event.trefProcessing().getTime()));
				} else {
					preparedStmt.setTimestamp(3, null);
				}*/
				preparedStmt.setTimestamp(3, null);

				preparedStmt.setDouble(4, event.xarc());
				preparedStmt.setDouble(5, event.yarc());
				preparedStmt.setDouble(6, event.lat());
				preparedStmt.setDouble(7, event.lon());
				preparedStmt.setString(8, event.source());

				if (event.sourceREF() != null) {
					preparedStmt.setString(9, event.sourceREF());
				} else {
					preparedStmt.setString(9, null); //""
				}

				/*if ((event.comment() != null) || !event.comment().isEmpty()) {
					preparedStmt.setString(10, event.comment());
				}*/

				// execute the preparedstatement
				preparedStmt.execute();

			} catch (SQLException e) {
				logger.info("STATUS: error.");
				logger.error(e.getMessage(), e);
			}

			/**
			 * get event_id tstart tend
			 * 
			 * optional section code!
			 */
			String query1 = "SELECT UUID_FROM_BIN(event_id) event_id FROM " + settings.eventsTable() + " WHERE tstart = ? and tend = ?";

			PreparedStatement ps = null;
			try {
				ps = conn.prepareStatement(query1);

				ps.setString(1, sdf1.format(event.tstart()));
				ps.setString(2, sdf1.format(event.tend()));

				// process the results
				ResultSet rs = ps.executeQuery();
				int size = 0;
				if (rs != null) {
					rs.last(); // moves cursor to the last row
					size = rs.getRow(); // get row id 
				}

				if (size == 1) {
					String id = rs.getString("event_id");
					event.eventID(id);
				} else {
					logger.error("NO CORRESPONDING EVENT ID FOUND!");
					//System.exit(1);
				}

				rs.close();
				ps.close();
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
			}

			/**
			 * end of optional -------------------
			 */

			/**
			 * details + waves read CSV's objects_m2/***.csv
			 */

			LinkedHashMap<String, Wave> waves = new LinkedHashMap<String, Wave>();

			for (int wavelength : event.wavelengths()) {

				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
				sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

				File waveCSV = new File(eventProcessingDir + "/objects_m2/" + wavelength + ".csv");
				System.out.println(waveCSV.getAbsolutePath());
				if (!waveCSV.exists()) {
					logger.error("File not found: " + waveCSV.getAbsolutePath());
					//System.exit(1);
				} else {
					
					//need fix for different names in different versions
					//Max. cardinality	 Jet aspect ratio	 Max. aspect ratio	 LtoW aspect ratio

					LinkedHashMap<String, String[]> mapCSV = Excel.readCSV(waveCSV, ",");
					int detailsCount = mapCSV.get("#").length;
					/*
					 * for (String variableName : mapCSV.keySet())
					 * {
					 * String variableKey = variableName;
					 * //String variableValue = mapCSV.get(variableName);
					 * //System.out.println("Name: " + variableKey);
					 * //System.out.println("Number: " + variableValue);
					 * }
					 */
					String[] tstartArr = mapCSV.get("T start");
					String[] tmaxArr = mapCSV.get("T max");
					String[] tendArr = mapCSV.get("T end");
					String[] ndetArr = mapCSV.get("#");
					//String[] durationArr = mapCSV.get("Duration"); not needed
					String[] maxCardArr = mapCSV.get("Max. card.");
					if (maxCardArr == null) {
						maxCardArr = mapCSV.get("Max. cardinality");
					}
					String[] jetAspectRatioArr = mapCSV.get("Jet asp. ratio");
					if (jetAspectRatioArr == null) {
						jetAspectRatioArr = mapCSV.get("Jet aspect ratio");
					}
					String[] maxAspectRatioArr = mapCSV.get("Max. asp. ratio");
					if (maxAspectRatioArr == null) {
						maxAspectRatioArr = mapCSV.get("Max. aspect ratio");
					}
					String[] ltowAspectRatioArr = mapCSV.get("Max. LtoW asp. ratio"); //need or not ????
					if (ltowAspectRatioArr == null) {
						ltowAspectRatioArr = mapCSV.get("LtoW aspect ratio");
					}
					String[] speedArr = mapCSV.get("Speed est.");
					String[] totalLengthArr = mapCSV.get("Total length");
					String[] avWidthArr = mapCSV.get("Av. width");
					String[] xFromArr = mapCSV.get("X from");
					String[] xToArr = mapCSV.get("X to");
					String[] yFromArr = mapCSV.get("Y from");
					String[] yToArr = mapCSV.get("Y to");

					Wave wave = new Wave();
					wave.wave(wavelength);
					//wave.comment("");
					//add other params

					/**
					 * movie_ref
					 * 
					 * event---/visual_data_m2/datetime_wavelength.mp4
					 * 
					 * list files in dir find 1 which ends on *_wavelength.mp4
					 * 
					 */

					File[] movieWaveFiles = new File(eventProcessingDir + "/visual_data_m2/").listFiles(new FileFilter() {
						@Override
						public boolean accept(File pathname) {
							String name = pathname.getName().toLowerCase();
							return name.endsWith("_" + String.valueOf(wavelength) + ".mp4") && pathname.isFile();
						}
					});

					if (movieWaveFiles.length == 0) {
						logger.error("No movie found for wave " + String.valueOf(wavelength));
						//System.exit(1);
					}

					if (movieWaveFiles.length > 1) {
						logger.error("Several movies found for wave " + String.valueOf(wavelength));
						for (int i = 0; i < movieWaveFiles.length; i++) {
							logger.error(movieWaveFiles[i]);
						}
						//System.exit(1);
					}

					String movieWave = movieWaveFiles[0].getName();
					wave.movie_ref(settings.processingREF() + "/" + year + "/" + eventProcessingDir.getName() + "/visual_data_m2/"
							+ movieWave);

					/**
					 * fits
					 * 
					 * 20190417_080700_20190417_090700_810_25_500_500\aia_data\171
					 */

					File fitsDirWave = new File(eventProcessingDir + "/aia_data/" + String.valueOf(wavelength));
					if (!fitsDirWave.exists() || !fitsDirWave.isDirectory()) {
						logger.error("No fits directory found for wave " + String.valueOf(wavelength));
						//System.exit(1);
					}
					wave.fits_ref(settings.processingREF() + "/" + year + "/" + eventProcessingDir.getName() + "/aia_data/"
							+ fitsDirWave.getName());

					/**
					 * pict ref 20190417_080700_20190417_090700_810_25_500_500\visual_data_m2\171
					 */

					File pictDirWave = new File(eventProcessingDir + "/visual_data_m2/" + String.valueOf(wavelength));
					if (!pictDirWave.exists() || !pictDirWave.isDirectory()) {
						logger.error("No pict directory found for wave " + String.valueOf(wavelength));
						//System.exit(1);
					}
					wave.pict_ref(settings.processingREF() + "/" + year + "/" + eventProcessingDir.getName() + "/visual_data_m2/"
							+ pictDirWave.getName());

					waves.put(Integer.toString(wavelength), wave);

					/**
					 * insert to waves
					 */

					/*INSERT INTO TAB_STUDENT(name_student, id_teacher_fk)
					SELECT 'Joe The Student', id_teacher
					  FROM TAB_TEACHER
					 WHERE name_teacher = 'Professor Jack'
					 LIMIT 1*/

					/*String query2 = "insert into waves" + 
					" (event_id, wave, movie_ref, pict_ref, fits_ref, comment) " + 
					"SELECT (?, ?, ?, ?, ?, ?) " +
					"FROM events " +
					"WHERE tstart = ? and tend = ? LIMIT 1";*/

					String query2 = "insert into " + settings.wavesTable() + " (wave, movie_ref, pict_ref, fits_ref, comment, event_id) "
							+ "SELECT ?, ?, ?, ?, ?, event_id " + "FROM events "
							+ "WHERE tstart = ? and tend = ? LIMIT 1 " + "ON DUPLICATE KEY UPDATE "
							+ "wave=VALUES(wave), " + "movie_ref=VALUES(movie_ref), " + "pict_ref=VALUES(pict_ref), "
							+ "fits_ref=VALUES(fits_ref), " + "comment=VALUES(comment);";
					
					if (settings.sqlTestRun()) {
						query2 = beforeSQL + query2 + afterSQL;
					}

					// create the mysql insert preparedstatement
					PreparedStatement ps1 = null;
					try {
						ps1 = conn.prepareStatement(query2);

						ps1.setInt(1, wave.wave());
						ps1.setString(2, wave.movie_ref());
						ps1.setString(3, wave.pict_ref());
						ps1.setString(4, wave.fits_ref());

						ps1.setString(5, wave.comment());

						ps1.setString(6, sdf1.format(event.tstart()));
						ps1.setString(7, sdf1.format(event.tend()));

						ps1.execute();
						ps1.close();
						//logger.info("Waves STATUS: successful.");
					} catch (SQLException e) {
						logger.info("STATUS: error.");
						logger.error(e.getMessage(), e);
					}

					LinkedHashMap<String, Detail> details = new LinkedHashMap<String, Detail>();

					for (int i = 0; i < detailsCount; i++) {
						Detail detail = new Detail();
						//2019-04-17T08:27:09.346
						Date dateSt = null;
						Date dateMax = null;
						Date dateEnd = null;
						try {
							dateSt = sdf.parse(tstartArr[i]);
							dateMax = sdf.parse(tmaxArr[i]);
							dateEnd = sdf.parse(tendArr[i]);
						} catch (ParseException e) {
							logger.error(e.getMessage(), e);
						}
						detail.tstart(dateSt);
						detail.tmax(dateMax);
						detail.tend(dateEnd);
						detail.ndet(Integer.valueOf(ndetArr[i]));
						detail.cardmax(Integer.valueOf(maxCardArr[i]));
						detail.asp_jet(Double.valueOf(jetAspectRatioArr[i]));
						detail.asp_max(Double.valueOf(maxAspectRatioArr[i]));
						detail.asp_ltow(Double.valueOf(ltowAspectRatioArr[i]));
						detail.total_length(Double.valueOf(totalLengthArr[i]));
						detail.av_width(Double.valueOf(avWidthArr[i]));
						detail.speed(Double.valueOf(speedArr[i]));
						detail.x_from(Double.valueOf(xFromArr[i]));
						detail.x_to(Double.valueOf(xToArr[i]));
						detail.y_from(Double.valueOf(yFromArr[i]));
						detail.y_to(Double.valueOf(yToArr[i]));

						/**
						 * movie_ref
						 * 
						 * 20190417_080700_810_25_171_detail0001.mp4
						 * 
						 * event---/visual_data_m2/datetime_wavelength_detail(int 4).mp4
						 * 
						 * list files in dir find 1 which ends on *_wavelength.mp4
						 * 
						 */

						File[] movieDetailsFiles = new File(eventProcessingDir + "/visual_data_m2/").listFiles(new FileFilter() {
							@Override
							public boolean accept(File pathname) {
								String name = pathname.getName().toLowerCase();
								return name.endsWith("_" + String.valueOf(wavelength) + "_detail"
										+ String.format("%04d", detail.ndet()) + ".mp4") && pathname.isFile();
							}
						});

						if (movieDetailsFiles.length == 0) {
							logger.error("No movie found for wave " + String.valueOf(wavelength) + " and detail #"
									+ detail.ndet());
							//System.exit(1);
						}

						if (movieDetailsFiles.length > 1) {
							logger.error("Several movies found for wave " + String.valueOf(wavelength) + " and detail #"
									+ detail.ndet());
							for (int j = 0; j < movieDetailsFiles.length; i++) {
								logger.error(movieDetailsFiles[i]);
							}
							//System.exit(1);
						}

						String movieDetail = movieDetailsFiles[0].getName();
						detail.movie_ref(settings.processingREF() + "/" + year + "/" + eventProcessingDir.getName()
								+ "/visual_data_m2/" + movieDetail);

						/**
						 * fits
						 * 
						 * 20190417_080700_20190417_090700_810_25_500_500\aia_data\171
						 */

						File fitsDirDetail = new File(eventProcessingDir + "/aia_data/" + String.valueOf(wavelength));
						if (!fitsDirDetail.exists() || !fitsDirDetail.isDirectory()) {
							logger.error("No fits directory found for wave " + String.valueOf(wavelength)
									+ " and detail #" + detail.ndet());
							//System.exit(1);
						}
						detail.fits_ref(settings.processingREF() + "/" + year + "/" + eventProcessingDir.getName() + "/aia_data/"
								+ fitsDirDetail.getName());

						/**
						 * pict ref
						 * 20190417_080700_20190417_090700_810_25_500_500\visual_data_m2\171\detail0001\
						 */

						File pictDirDetail = new File(eventProcessingDir + "/visual_data_m2/" + String.valueOf(wavelength)
								+ "/detail" + String.format("%04d", detail.ndet()));
						if (!pictDirDetail.exists() || !pictDirDetail.isDirectory()) {
							logger.error("No pict directory found for wave " + String.valueOf(wavelength)
									+ " and detail #" + detail.ndet());
							//System.exit(1);
						}
						detail.pict_ref(settings.processingREF() + "/" + year + "/" + eventProcessingDir.getName()
								+ "/visual_data_m2/" + String.valueOf(wavelength) + "/" + pictDirDetail.getName());

						detail.comment("");

						details.put(Integer.toString(detail.ndet()), detail);

						/**
						 * sav ref 20190417_080700_20190417_090700_810_25_500_500\objects_m2\171.sav
						 */

						File savWaveFile = new File(eventProcessingDir + "/objects_m2/" + String.valueOf(wavelength) + ".sav");
						if (!savWaveFile.exists()) {
							logger.error("No .sav file found for wave " + String.valueOf(wavelength));
							//System.exit(1);
						}

						detail.sav_ref(settings.processingREF() + "/" + year + "/" + eventProcessingDir.getName() + "/objects_m2/"
								+ savWaveFile.getName());

						/**
						 * ----------------------------------------------------------- ndet tstart tmax
						 * tend cardmax asp_jet asp_max total_length av_width speed x_from x_to y_from
						 * y_to movie_ref pict_ref fits_ref sav_ref comment
						 */

						/*String query3 = "insert into details " + 
							    "(ndet, tstart, tend, comment, wave_id) " + 
							    "SELECT ?, ?, ?, ?, wave_id " +
							    "FROM waves " +
							    "WHERE wave = ? and event_id = ? LIMIT 1 " +
							    "ON DUPLICATE KEY UPDATE " +
							    "ndet=VALUES(ndet), " +
							    "tstart=VALUES(tstart), " +
							    "tend=VALUES(tend), " +
							    "comment=VALUES(comment);";*/

						String query3 = "insert into " + settings.detailsTable()
								+ " (ndet, tstart, tmax, tend, cardmax, asp_jet, asp_max, asp_ltow, total_length, av_width, speed, x_from, x_to,"
								+ "y_from, y_to, movie_ref, pict_ref, fits_ref, sav_ref, comment, wave_id) "
								+ "SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, wave_id "
								+ "FROM " + settings.wavesTable() + " WHERE wave = ? and event_id = UUID_TO_BIN(?) LIMIT 1 "
								+ "ON DUPLICATE KEY UPDATE " + "ndet=VALUES(ndet), " + "tstart=VALUES(tstart), "
								+ "tmax=VALUES(tmax), " + "tend=VALUES(tend), " + "cardmax=VALUES(cardmax), "
								+ "asp_jet=VALUES(asp_jet), " + "asp_max=VALUES(asp_max), "
								+ "asp_ltow=VALUES(asp_ltow), " + "total_length=VALUES(total_length), "
								+ "av_width=VALUES(av_width), " + "speed=VALUES(speed), " + "x_from=VALUES(x_from), "
								+ "x_to=VALUES(x_to), " + "y_from=VALUES(y_from), " + "y_to=VALUES(y_to), "
								+ "movie_ref=VALUES(movie_ref), " + "pict_ref=VALUES(pict_ref), "
								+ "fits_ref=VALUES(fits_ref), " + "sav_ref=VALUES(sav_ref), "
								+ "comment=VALUES(comment);";
						
						if (settings.sqlTestRun()) {
							query3 = beforeSQL + query3 + afterSQL;
						}

						// create the mysql insert preparedstatement
						PreparedStatement ps2 = null;
						try {
							ps2 = conn.prepareStatement(query3);

							ps2.setInt(1, detail.ndet());
							ps2.setString(2, sdf1.format(detail.tstart()));
							ps2.setString(3, sdf1.format(detail.tmax()));
							ps2.setString(4, sdf1.format(detail.tend()));
							ps2.setInt(5, detail.cardmax());
							ps2.setDouble(6, detail.asp_jet());
							ps2.setDouble(7, detail.asp_max());
							ps2.setDouble(8, detail.asp_ltow());
							ps2.setDouble(9, detail.total_length());
							ps2.setDouble(10, detail.av_width());
							ps2.setDouble(11, detail.speed());
							ps2.setDouble(12, detail.x_from());
							ps2.setDouble(13, detail.x_to());
							ps2.setDouble(14, detail.y_from());
							ps2.setDouble(15, detail.y_to());
							ps2.setString(16, detail.movie_ref());
							ps2.setString(17, detail.pict_ref());
							ps2.setString(18, detail.fits_ref());
							ps2.setString(19, detail.sav_ref());

							ps2.setString(20, detail.comment());
							ps2.setInt(21, wave.wave());
							ps2.setString(22, event.eventID());

							ps2.execute();
							ps2.close();
						} catch (SQLException e) {
							logger.info("STATUS: error.");
							logger.error(e.getMessage(), e);
						}
					}

				}
			}

			//logger.info("STATUS: successful.");
			//logger.info("--------------");

		}

		try {
			conn.close();
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}

		int result = 1;
		return result;
	}

	public Event readConfigJSON(File file, Settings settings) {

		/**
		 * { -- starting json object [ -- starting json array
		 * 
		 */

		//InputStream is = ReadJSONString.class.getResourceAsStream(resourceName);
		InputStream is = null;
		try {
			is = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			logger.info(e.getMessage());
			//throw new NullPointerException("Cannot find resource file " + resourceName);
		}

		JSONTokener tokener = new JSONTokener(is);
		JSONObject jsonObject = new JSONObject(tokener);

		String tstartProcessing = jsonObject.getString("tstart");
		String tstopProcessing = jsonObject.getString("tstop");
		String trefProcessing = jsonObject.getString("tref");
		int xcProcessing = jsonObject.getInt("xc");
		int ycProcessing = jsonObject.getInt("yc");
		int wpixProcessing = jsonObject.getInt("wpix");
		int hpixProcessing = jsonObject.getInt("hpix");

		JSONArray wavesJson = jsonObject.getJSONArray("waves");
		int[] waves = new int[wavesJson.length()];
		for (int i = 0; i < wavesJson.length(); i++) {
			waves[i] = (int) wavesJson.get(i);
		}

		//2019-04-1708:07:00
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date1 = null;
		Date date2 = null;
		Date date3 = null;
		try {
			date1 = df.parse(tstartProcessing);
			date2 = df.parse(tstopProcessing);
			date3 = df.parse(trefProcessing);
		} catch (ParseException e) {
			logger.error(e.getMessage());
		}

		Event event = new Event();
		event.tstartProcessing(date1);
		event.tendProcessing(date2);
		event.trefProcessing(date3);
		event.xcProcessing(xcProcessing);
		event.ycProcessing(ycProcessing);
		event.wpixProcessing(wpixProcessing);
		event.hpixProcessing(hpixProcessing);
		event.wavelengths(waves);

		return event;
	}

	/*	*//**
			 * Get all files from a directory.
			 */
	/*
	public static void listAllFilesInDir(File directory, List<File> files) {
	
	//File processingDir = new File(directory);
	File[] fList = directory.listFiles();
	if (fList != null)
		for (File file : fList) {
			if (file.isFile()) {
				files.add(file);
			} else if (file.isDirectory()) {
				listAllFilesInDir(new File(file.getAbsolutePath()), files);
			}
		}
	}
	
	*//**
		 * Get all directories from a directory.
		 *//*
			public static void listAllDirectoriesInDir(File directory, List<File> directories) {
			
			File[] fList = directory.listFiles();
			if (fList != null)
				for (File file : fList) {
					if (file.isDirectory()) {
						directories.add(file);
						listAllDirectoriesInDir(new File(file.getAbsolutePath()), directories);
					}
				}
			}*/
}
