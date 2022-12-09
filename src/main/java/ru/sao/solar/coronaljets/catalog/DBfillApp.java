package ru.sao.solar.coronaljets.catalog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.spi.LoggerContext;

import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.TimeZone;

public class DBfillApp {
	
	/**
	 * Version
	 */
	private final static String versionApp = "0.9.22";
	
	/**
	 * Set logger
	 */
	final static LoggerContext loggerContext = Configurator.initialize(null,Settings.resourcesPath() + "config/log4j2.xml");
	final static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	// не заполнять базу = событие = пока не покажет что нет ошибок
	
    public static void main(String[] args) {

    	logger.info("--------------");
    	logger.info("*** Coronal Jets Catalog Database Filling Application v" + versionApp() + " ***");
    	/**
    	init settings
    	**/
    	Settings settings = new Settings().init();

		DateFormat df = new SimpleDateFormat("YYYY-MM-dd");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
    	
    	/**
    	 * 
    	 */

    	logger.info("Presented configuration [config/settings.ini] will be used:");
    	logger.info("--------------");
		logger.info("period: " + df.format(settings.minDatetime()) + " -- " + df.format(settings.maxDatetime()) );
		logger.info("remoteConnection = " + settings.remoteConnection());
    	logger.info("eventsConfigPath = " + settings.eventsConfigPath());
    	logger.info("dataPathProcessing = " + settings.dataPathProcessing());
    	logger.info("eventsCatalogTable = " + settings.eventsCatalogTable());
    	logger.info("processingREF = " + settings.processingREF());
    	logger.info("SQL Test Run: " + settings.sqlTestRun());
    	logger.info("username: " + settings.username());
    	logger.info("password: " + settings.password());
    	logger.info("databasename: " + settings.databasename());
    	logger.info("hostname: " + settings.hostname());
    	logger.info("port: " + settings.port());

		logger.info("eventsTable: " + settings.eventsTable());
    	
    	logger.info("xcTolerance: " + settings.xcTolerance() + " arcsec");
    	logger.info("ycTolerance: " + settings.ycTolerance() + " arcsec");
    	logger.info("tstartTolerance: " + settings.tstartToleranceSec() + " sec");
    	logger.info("tendTolerance: " + settings.tendToleranceSec() + " sec");
    	//logger.info("no_confirm: " + settings.noConfirm());
    	logger.info("--------------");
    	
    	boolean isProcessing = false;
    	if (settings.noConfirm()) {
    		isProcessing = true;
    	} else {
    		logger.info("Please review the configuration. Are you agree to run selected configuration? y/n");
    		Scanner scanner = new Scanner(System.in);
    		String val = scanner.next();
    		scanner.close();
    		if(val.equalsIgnoreCase("y")||val.equalsIgnoreCase("yes")) {
    				
    			isProcessing = true;
    			
    		} else if(val.equalsIgnoreCase("n")||val.equalsIgnoreCase("no")) {
    		} else { 
    			System.out.println("Invalid character");
    			return;
    		}
    	}
    	
    	if (isProcessing) {
    		/**
        	 * doing job
        	 */
    		new Processing(settings);
    	}

		logger.info("-----------\n" + "Application completed.");
    }
    
	public static String versionApp() {
		return versionApp;
	}
	
}
