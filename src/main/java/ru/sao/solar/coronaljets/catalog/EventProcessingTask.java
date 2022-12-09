package ru.sao.solar.coronaljets.catalog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
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

public class EventProcessingTask implements Callable<Event> {

	final static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private Settings settings;
	private File eventConfPath;
	private File eventProcessingPath;
	private LinkedHashMap<String, String[]> mapXLSX;
	//private ProgressBarConsole pb;

	public EventProcessingTask(File eventConfPath, Settings settings, LinkedHashMap<String, String[]> mapXLSX) { //, ProgressBarConsole pb
		this.settings = settings;
		this.eventConfPath = eventConfPath;
		this.mapXLSX = mapXLSX;
		//this.eventProcessingDir = new File(settings.dataPathProcessing() + "/" + eventConfDir);
		//this.pb = pb;
	}

	@Override
	public Event call() throws Exception {

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));

		/**
		 * list all files in event conf dir
		 */
		List<File> files = new ArrayList<File>();
		MyUtils.listAllFilesInDir(eventConfPath, files);
		//MyUtils.printList(files);

		File fileConfigJSON = null;
		for (File file : files) {
			if (String.valueOf(file).endsWith("config.json")) {
				fileConfigJSON = new File(String.valueOf(file));
			}
		}

		if (fileConfigJSON == null) {
			logger.warn("config.json" + " not found in: " + eventConfPath);
			Event event = new Event();
			event.eventLogAdd("config.json" + " not found in: " + eventConfPath);
			return event;
		}

		Event event = readConfigJSON(fileConfigJSON, settings);
		event.eventLogAdd("Event conf path: " + eventConfPath);

		/**
		 * ассоциация события со строкой в таблице xlsx
		 * Для получения ссылки на hek
		 * Опционально: замена времени события из conf processing на времена из таблицы excel
		 */

		event.associate(mapXLSX, settings);

		/**
		 * определить найдена ли ассоциация, если да, переписать времена события. Если нет, назначить от processing
		 */

		if (!event.isExcelAssociated()){
			event.tstart(event.tstartProcessing());
			event.tend(event.tendProcessing());
			event.tref(event.trefProcessing());
		}

		/**
		 * Reading processing directory
		 */

		SimpleDateFormat dfYear = new SimpleDateFormat("yyyy");
		String year = dfYear.format(event.tstart());

		DateFormat dfDir = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String eventDir_name = dfDir.format(event.tstartProcessing()) + "_" +
				dfDir.format(event.tendProcessing()) + "_" + (int) event.xarc() + "_" + (int)
				event.yarc() + "_" + event.wpixProcessing() + "_" + event.hpixProcessing();

		this.eventProcessingPath = new File(settings.dataPathProcessing() + "/" + year + "/" + eventDir_name);
		event.eventLogAdd("Event processing path: " + eventProcessingPath);

		//logger.info("-----------------");
		//logger.info("Event: " + eventProcessingPath.getName() + ":");

		if (!eventProcessingPath.exists()) {
			//logger.warn("-----------------");
			//logger.warn("Event: " + eventProcessingDir.getName() + ":");
			event.eventLogAdd("Warning: No processing data existing for event config.json file: '" + eventConfPath.getAbsolutePath() + "'");
			event.eventLogAdd("Processing data expected in: '" + eventProcessingPath.getAbsolutePath() + "'");
			//logger.warn("-----------------");
			//throw new Exception("No processsing data");
		}

		/**
		 * SQL
		 */

		Connection conn = settings.getDBConnection();

		//sql test run

		String beforeSQL = "START TRANSACTION; ";
		String afterSQL = " COMMIT; ROLLBACK;";

		/**
		 * insert to database
		 */

		String query = "insert into " + settings.eventsTable() + " (tstart, tend, tref, xarc, yarc, lat, lon, source, sourceref)" + //, comment
				" values (?, ?, ?, ?, ?, ?, ?, ?, ?)" + " ON DUPLICATE KEY UPDATE " + "tstart=VALUES(tstart), "
				+ "tend=VALUES(tend), " + "tref=VALUES(tref), " + "xarc=VALUES(xarc), " + "yarc=VALUES(yarc), "
				+ "lat=VALUES(lat), " + "lon=VALUES(lon), " + "source=VALUES(source), "
				+ "sourceref=VALUES(sourceref);";

		if (settings.sqlTestRun()) {
			query = beforeSQL + query + afterSQL;
		}

		//event.eventLogAdd("QUERY: " + query);

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
			event.eventLogAdd("SQL Statement STATUS: error.");
			event.eventLogAdd(e.getMessage());
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

			ps.setString(1, df.format(event.tstart()));
			ps.setString(2, df.format(event.tend()));

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

			File waveCSV = new File(eventProcessingPath + "/objects_m2/" + wavelength + ".csv");
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

				File[] movieWaveFiles = new File(eventProcessingPath + "/visual_data_m2/").listFiles(new FileFilter() {
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
				wave.movie_ref(settings.processingREF() + "/" + year + "/" + eventProcessingPath.getName() + "/visual_data_m2/"
						+ movieWave);

				/**
				 * fits
				 *
				 * 20190417_080700_20190417_090700_810_25_500_500\aia_data\171
				 */

				File fitsDirWave = new File(eventProcessingPath + "/aia_data/" + String.valueOf(wavelength));
				if (!fitsDirWave.exists() || !fitsDirWave.isDirectory()) {
					logger.error("No fits directory found for wave " + String.valueOf(wavelength));
					//System.exit(1);
				}
				wave.fits_ref(settings.processingREF() + "/" + year + "/" + eventProcessingPath.getName() + "/aia_data/"
						+ fitsDirWave.getName());

				/**
				 * pict ref 20190417_080700_20190417_090700_810_25_500_500\visual_data_m2\171
				 */

				File pictDirWave = new File(eventProcessingPath + "/visual_data_m2/" + String.valueOf(wavelength));
				if (!pictDirWave.exists() || !pictDirWave.isDirectory()) {
					logger.error("No pict directory found for wave " + String.valueOf(wavelength));
					//System.exit(1);
				}
				wave.pict_ref(settings.processingREF() + "/" + year + "/" + eventProcessingPath.getName() + "/visual_data_m2/"
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

					ps1.setString(6, df.format(event.tstart()));
					ps1.setString(7, df.format(event.tend()));

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

					File[] movieDetailsFiles = new File(eventProcessingPath + "/visual_data_m2/").listFiles(new FileFilter() {
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
					detail.movie_ref(settings.processingREF() + "/" + year + "/" + eventProcessingPath.getName()
							+ "/visual_data_m2/" + movieDetail);

					/**
					 * fits
					 *
					 * 20190417_080700_20190417_090700_810_25_500_500\aia_data\171
					 */

					File fitsDirDetail = new File(eventProcessingPath + "/aia_data/" + String.valueOf(wavelength));
					if (!fitsDirDetail.exists() || !fitsDirDetail.isDirectory()) {
						logger.error("No fits directory found for wave " + String.valueOf(wavelength)
								+ " and detail #" + detail.ndet());
						//System.exit(1);
					}
					detail.fits_ref(settings.processingREF() + "/" + year + "/" + eventProcessingPath.getName() + "/aia_data/"
							+ fitsDirDetail.getName());

					/**
					 * pict ref
					 * 20190417_080700_20190417_090700_810_25_500_500\visual_data_m2\171\detail0001\
					 */

					File pictDirDetail = new File(eventProcessingPath + "/visual_data_m2/" + String.valueOf(wavelength)
							+ "/detail" + String.format("%04d", detail.ndet()));
					if (!pictDirDetail.exists() || !pictDirDetail.isDirectory()) {
						logger.error("No pict directory found for wave " + String.valueOf(wavelength)
								+ " and detail #" + detail.ndet());
						//System.exit(1);
					}
					detail.pict_ref(settings.processingREF() + "/" + year + "/" + eventProcessingPath.getName()
							+ "/visual_data_m2/" + String.valueOf(wavelength) + "/" + pictDirDetail.getName());

					detail.comment("");

					details.put(Integer.toString(detail.ndet()), detail);

					/**
					 * sav ref 20190417_080700_20190417_090700_810_25_500_500\objects_m2\171.sav
					 */

					File savWaveFile = new File(eventProcessingPath + "/objects_m2/" + String.valueOf(wavelength) + ".sav");
					if (!savWaveFile.exists()) {
						logger.error("No .sav file found for wave " + String.valueOf(wavelength));
						//System.exit(1);
					}

					detail.sav_ref(settings.processingREF() + "/" + year + "/" + eventProcessingPath.getName() + "/objects_m2/"
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
						ps2.setString(2, df.format(detail.tstart()));
						ps2.setString(3, df.format(detail.tmax()));
						ps2.setString(4, df.format(detail.tend()));
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

		try {
			conn.close();
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}

		return event;
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
			throw new NullPointerException("Cannot find resource file " + file);
		}

		JSONTokener tokener = new JSONTokener(is);
		JSONObject jsonObject = new JSONObject(tokener);

		/**
		 * 		OLD format
		 *     "tstart":"2022-01-0706:46:05",
		 *     "tstop":"2022-01-0707:46:05",
		 *     "tref":"2022-01-0706:46:05",
		 *     "xc":-648.60000,
		 *     "yc":-543.20000,
		 *     "wpix":500,
		 *     "hpix":500,
		 *     "waves":[171],
		 *     "method":2,
		 *     "timeout":3,
		 *     "count":3,
		 *     "limit":30,
		 *     "timeout_post":5,
		 *     "count_post":3
		 *
		 * 		----
		 * 		NEW format
		 *     "TIME_START":"2022-03-13 14:32:02",
		 *     "TIME_STOP":"2022-03-13 16:05:57",
		 *     "TIME_REF":"2022-03-13 14:32:02",
		 *     "TIME_OCC":"2022-03-13 14:37:02",
		 *     "X_CENTER":-194.6,
		 *     "Y_CENTER":491.5,
		 *     "WIDTH_ARC":300,
		 *     "HEIGHT_ARC":300,
		 *     "WAVES":[171]
		 */

		List<String> tstartKeyList = Arrays.asList("tstart", "TIME_START");
		List<String> tstopKeyList = Arrays.asList("tstop", "TIME_STOP");
		List<String> trefKeyList = Arrays.asList("tref", "TIME_REF");
		List<String> xcKeyList = Arrays.asList("xc", "X_CENTER");
		List<String> ycKeyList = Arrays.asList("yc", "Y_CENTER");
		List<String> wpixList = Arrays.asList("wpix", "WIDTH_ARC"); //wrong!!!!!!! перевести пиксели в угловой размер. Можно взять wpix из названия папки события
		List<String> hpixList = Arrays.asList("hpix", "HEIGHT_ARC"); //wrong!!!!!!! перевести пиксели в угловой размер
		List<String> wavesKeyList = Arrays.asList("waves", "WAVES");

		List<String> dateFormatList = Arrays.asList("yyyy-MM-ddHH:mm:ss", "yyyy-MM-dd HH:mm:ss");

		String tstartProcessing = parseJSONstring(jsonObject, tstartKeyList);
		String tstopProcessing = parseJSONstring(jsonObject, tstopKeyList);
		String trefProcessing = parseJSONstring(jsonObject, trefKeyList);
		int xcProcessing = parseJSONint(jsonObject, xcKeyList);
		int ycProcessing = parseJSONint(jsonObject, ycKeyList);
		int wpixProcessing = parseJSONint(jsonObject, wpixList);
		int hpixProcessing = parseJSONint(jsonObject, hpixList);
		JSONArray wavesJson = parseJSONarray(jsonObject, wavesKeyList);

		/*String tstartProcessing = jsonObject.getString("tstart");
		String tstopProcessing = jsonObject.getString("tstop");
		String trefProcessing = jsonObject.getString("tref");
		int xcProcessing = jsonObject.getInt("xc");
		int ycProcessing = jsonObject.getInt("yc");
		int wpixProcessing = jsonObject.getInt("wpix");
		int hpixProcessing = jsonObject.getInt("hpix");

		JSONArray wavesJson = jsonObject.getJSONArray("waves");*/
		int[] waves = new int[wavesJson.length()];
		for (int i = 0; i < wavesJson.length(); i++) {
			waves[i] = (int) wavesJson.get(i);
		}

		/**
		 * kludge | костыль:
		 * если нет ключей wpix hpix ставить 500
		 */

		if (wpixProcessing != 500) wpixProcessing = 500;
		if (hpixProcessing != 500) hpixProcessing = 500;

		//2019-04-1708:07:00
		//SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
		//df.setTimeZone(TimeZone.getTimeZone("UTC"));

		Date date1 = null;
		Date date2 = null;
		Date date3 = null;
		date1 = MyUtils.parseDate(tstartProcessing, dateFormatList, "UTC");
		date2 = MyUtils.parseDate(tstopProcessing, dateFormatList, "UTC");
		date3 = MyUtils.parseDate(trefProcessing, dateFormatList, "UTC");

		Event event = new Event();
		if (date1 != null){
			event.tstartProcessing(date1);
		} else {
			event.eventLogAdd("tstart date is null");
		}

		if (date2 != null){
			event.tendProcessing(date2);
		} else {
			event.eventLogAdd("tend date is null");
		}

		if (date3 != null){
			event.trefProcessing(date3);
		} else {
			event.eventLogAdd("tref date is null");
		}

		event.xcProcessing(xcProcessing);
		event.ycProcessing(ycProcessing);
		event.wpixProcessing(wpixProcessing);
		event.hpixProcessing(hpixProcessing);
		event.wavelengths(waves);

		return event;
	}

	public JSONArray parseJSONarray(JSONObject jsonObject, List<String> keyVariantsList){

		for (String key : keyVariantsList){
			try {
				return jsonObject.getJSONArray(key);
			} catch (JSONException e) {
			}
		}
		return null;
	}

	public Integer parseJSONint(JSONObject jsonObject, List<String> keyVariantsList){

		for (String key : keyVariantsList){
			try {
				return jsonObject.getInt(key);
			} catch (JSONException e) {
			}
		}
		return null;
	}

	public String parseJSONstring(JSONObject jsonObject, List<String> keyVariantsList){

		for (String key : keyVariantsList){
			try {
				return jsonObject.getString(key);
			} catch (JSONException e) {
			}
		}
		return null;
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
