package ru.sao.solar.coronaljets.catalog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sao.solar.coronaljets.utils.MyUtils;

import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Event {
	
	private String eventID;
	
	private Date tstart;
	private Date tend;
	private Date tref;
	
	private double xarc;
	private double yarc;
	private double lat;
	private double lon;
	
	private Date tstartProcessing;
	private Date tendProcessing;
	private Date trefProcessing;
	
	private int xcProcessing;
	private int ycProcessing;
	private int wpixProcessing;
	private int hpixProcessing;
	
	private LinkedHashMap<String, Wave> waves;
	private int[] wavelengths;
	
	private String hekURL; //source
	private String source;
	private String sourceREF;
	private String comment;

	private boolean excelAssociated;

	private String eventLog;
	
	final static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	
	public Event() {
		
	}

	public void associated(boolean excelAssociated){
		this.excelAssociated = excelAssociated;
	};
	public boolean isExcelAssociated(){
		return excelAssociated;
	}
	
	public void eventID(String eventID) {
		this.eventID = eventID;
	}
	
	public String eventID() {
		return eventID;
	}
	
	public void waves(LinkedHashMap<String, Wave> waves) {
		this.waves = new LinkedHashMap<String, Wave>(waves);
	}
	
	public LinkedHashMap<String, Wave> waves() {
		return waves;
	}
	
	public void tstart(Date tstart) {
		this.tstart = tstart;
	}
	
	public Date tstart() {
		return tstart;
	}
	
	public void tend(Date tend) {
		this.tend = tend;
	}
	
	public Date tend() {
		return tend;
	}
	
	public void tref(Date tref) {
		this.tref = tref;
	}
	
	public Date tref() {
		return tref;
	}
	
	public void tstartProcessing(Date tstartProcessing) {
		this.tstartProcessing = tstartProcessing;
	}
	
	public Date tstartProcessing() {
		return tstartProcessing;
	}
	
	public void tendProcessing(Date tendProcessing) {
		this.tendProcessing = tendProcessing;
	}
	
	public Date tendProcessing() {
		return tendProcessing;
	}
	
	public void trefProcessing(Date trefProcessing) {
		this.trefProcessing = trefProcessing;
	}
	
	public Date trefProcessing() {
		return trefProcessing;
	}
	
	public void xcProcessing(int xcProcessing) {
		this.xcProcessing = xcProcessing;
	}
	
	public int xcProcessing() {
		return xcProcessing;
	}
	
	public void ycProcessing(int ycProcessing) {
		this.ycProcessing = ycProcessing;
	}
	
	public int ycProcessing() {
		return ycProcessing;
	}
	
	public void wpixProcessing(int wpixProcessing) {
		this.wpixProcessing = wpixProcessing;
	}
	
	public int wpixProcessing() {
		return wpixProcessing;
	}
	
	public void hpixProcessing(int hpixProcessing) {
		this.hpixProcessing = hpixProcessing;
	}
	
	public int hpixProcessing() {
		return hpixProcessing;
	}
	
	public void wavelengths(int[] wavelengths) {
		this.wavelengths = wavelengths.clone();
	}
	
	public int[] wavelengths() {
		return wavelengths;
	}
	
	public void hekURL(String url) {
		this.hekURL = url;
	}
	
	public String hekURL() {
		return hekURL;
	}
	
	public void xarc(Double xarc) {
		this.xarc = xarc;
	}
	
	public double xarc() {
		return xarc;
	}
	
	public void yarc(Double yarc) {
		this.yarc = yarc;
	}
	
	public double yarc() {
		return yarc;
	}
	
	public void lat(Double lat) {
		this.lat = lat;
	}
	
	public double lat() {
		return lat;
	}
	
	public void lon(Double lon) {
		this.lon = lon;
	}
	
	public double lon() {
		return lon;
	}
	
	public void source(String source) {
		this.source = source;
	}
	
	public String source() {
		return source;
	}
	
	public void sourceREF(String sourceREF) {
		this.sourceREF = sourceREF;
	}
	
	public String sourceREF() {
		return sourceREF;
	}
	
	public void comment(String comment) {
		this.comment = comment;
	}
	
	public String comment() {
		return comment;
	}

	public void eventLogAdd(String message){
		if (this.eventLog == null){
			this.eventLog = message;
		} else {
			this.eventLog = this.eventLog + System.lineSeparator() + message;
		}
	}

	public String getEventLog(){
		return this.eventLog;
	}
	
	public void associate(LinkedHashMap<String, String[]> map, Settings settings) {
		
		//print content
		/*Set<Map.Entry<String, String[]>> entrySet = map.entrySet();
		Iterator<Map.Entry<String, String[]>> it = entrySet.iterator();
		while(it.hasNext()){
			Map.Entry<String, String[]> me = (Map.Entry<String, String[]>)it.next();
		    System.out.println("Key is: "+me.getKey() +
		    " & " +
		    " value is: "+me.getValue());
		}*/
		
		//String[] startTimes = map.get("Start date-time");
		String[] startTimes = map.get("Start date-time");
		//Fri Jan 03 07:15:01 UTC 2020

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		SimpleDateFormat dfs = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
        dfs.setTimeZone(TimeZone.getTimeZone("UTC"));
		long[] diffs = new long[startTimes.length];
		for (int i=0; i<startTimes.length; i++) {
			Date date = null;
			try {
				date = dfs.parse(startTimes[i]);
			} catch (ParseException e) {
				logger.error(e.getMessage());
			}
			long diff = Math.abs(tstartProcessing().getTime() - date.getTime())/1000;
			diffs[i] = diff;
		}
		
		//find min
		int ind = MyUtils.min(diffs).index();
		
		if (diffs[ind] > settings.tstartToleranceSec()) {
			this.eventLogAdd("No corresponding event found in excel table for " + df.format(tstartProcessing()));
			this.excelAssociated = false;
			return;
		}

		this.excelAssociated = true;

		String hekURL = map.get("HEK summary")[ind];
		this.hekURL(hekURL);
		//System.out.println(this.hekURL());
		
		//Start date-time
		String tstartStr = map.get("Start date-time")[ind];
		Date tstart = null;
		try {
			tstart = dfs.parse(tstartStr);
		} catch (ParseException e) {
			logger.error(e.getMessage());
		}
		this.tstart = tstart;
		
		//End date-time
		String tendStr = map.get("End date-time")[ind];
		Date tend = null;
		try {
			tend = dfs.parse(tendStr);
		} catch (ParseException e) {
			logger.error(e.getMessage());
		}
		this.tend = tend;
		
		//X [arcsec]
		String xarc = map.get("X [arcsec]")[ind];
		this.xarc = Double.valueOf(xarc);
		
		//Y [arcsec]
		String yarc = map.get("Y [arcsec]")[ind];
		this.yarc = Double.valueOf(yarc);
		
		//Longitude
		String lon = map.get("Longitude")[ind];
		this.lon = Double.valueOf(lon);
		
		//Latitude
		String lat = map.get("Latitude")[ind];
		this.lat = Double.valueOf(lat);
		
		//!!
		this.source = this.hekURL;
		
		//Comments
		/*String comment = map.get("Comments")[ind];
		this.comment = String.valueOf(comment);*/
		
		//this.comment = "TEST // " + this.comment; //!!!
	}

}
