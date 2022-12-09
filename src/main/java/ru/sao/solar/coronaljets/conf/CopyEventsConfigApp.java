package ru.sao.solar.coronaljets.conf;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.spi.LoggerContext;
import ru.sao.solar.coronaljets.utils.MyUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CopyEventsConfigApp {

    /**
     * Version
     */
    private final static String VERSION = "0.3";

    //private final static String EVENTS_CONF_PATH = "D:/Projects/Coronal Jets/data/events-conf-tmp";
    //private final static String EVENTS_PROCESSING_PATH = "D:/Projects/Coronal Jets/data/processing";
    //private final static String RESOURCES_PATH = "D:/Projects/Coronal jets/dbfillapp/resources/";

    public final static String RESOURCES_PATH = "/main/coronal-jets/db-fill-app/resources/";
    private final static String EVENTS_CONF_PATH = "/main/coronal-jets/data/events-conf-tmp";
    private final static String EVENTS_PROCESSING_PATH = "/main/coronal-jets/data/processing";

    /**
     * Set logger
     */
    final static LoggerContext loggerContext = Configurator.initialize(null, RESOURCES_PATH + "config/log4j2.xml");
    final static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    public CopyEventsConfigApp(){

        System.out.println("Q");

        logger.info("CopyEventsConfigApp. Version " + VERSION);

        if (Files.exists(Paths.get(EVENTS_PROCESSING_PATH))) {
            logger.info("Processing path: " + EVENTS_PROCESSING_PATH);
        } else {
            logger.info("Processing path doesn't exists: " + EVENTS_PROCESSING_PATH);
        }

        /**
         *
         * /main/coronal-jets/data/processing/2010/20101114_091500_20101114_102500_-944_58_500_500/objects_m2/config.json
         * /main/coronal-jets/data/events-conf-tmp/2010/20101114_091500_20101114_102500_-944_58_500_500/config.json
         */

        //проверяю что существует EVENTS_CONF_PATH, если нет, создаю
        try {
            Files.createDirectories(Paths.get(EVENTS_CONF_PATH));
            logger.info("Destination path for config.json's: " + EVENTS_CONF_PATH);
        } catch (IOException e) {
            logger.error("Can't create directory " + EVENTS_CONF_PATH);
            throw new RuntimeException(e);
        }

        //создаю список файлов config.json
        List<File> filesList = MyUtils.listAllFilesInDir(new File(EVENTS_PROCESSING_PATH));

        List<File> confFilesList = new ArrayList<File>();
        for (File file : filesList) {

            if (file.getName().endsWith("config.json")){
                confFilesList.add(file);
            }
        }
        //MyUtils.printList(confFilesList);

        /**
         * TEST
         * подсчитать количество папок событий в processings
         *
         */

        String regexEvent = ".*(\\d{8}_\\d{6}_\\d{8}_\\d{6}_\\-*\\d+_\\-*\\d+_\\d+_\\d+).*";

        List<File> subDirectories = new ArrayList<File>();
        MyUtils.listAllDirectoriesInDir(new File(EVENTS_PROCESSING_PATH), subDirectories);
        //MyUtils.printList(subDirectories);
        //logger.info();
        //FileFilter fileFilter = new RegexFileFilter("^\\d{8}_\\d{6}_\\d{8}_\\d{6}_\\-*\\d+_\\-*\\d+_\\d+_\\d+$");
        String regex = "^\\d{8}_\\d{6}_\\d{8}_\\d{6}_\\-*\\d+_\\-*\\d+_\\d+_\\d+$";
        FileFilter fileFilter = new RegexFileFilter(regex);

        List<File> eventDirectories = new ArrayList<File>();
        for (File dir : subDirectories) {
            File[] dirs = dir.listFiles(fileFilter);
            for (File file : dirs) {
                eventDirectories.add(file);
            }
        }
        //MyUtils.printList(eventDirectories);
        logger.info("Found event folders: " + eventDirectories.size());

        /**
         * END TEST
         */

        /*//копирую файлы config.json в директорию events-conf
        String regexEventFolder = ".*" + Pattern.quote(FileSystems.getDefault().getSeparator()) + "(\\d{8}_\\d{6}_\\d{8}_\\d{6}_\\-*\\d+_\\-*\\d+_\\d+_\\d+)" + Pattern.quote(FileSystems.getDefault().getSeparator()) + ".*";
        //String regexEventFolder = ".*(\\d{8}_\\d{6}_\\d{8}_\\d{6}_\\-*\\d+_\\-*\\d+_\\d+_\\d+).*"; //working
        Pattern patternEventFolder = Pattern.compile(regexEventFolder);

        String regexYear = ".*" + Pattern.quote(FileSystems.getDefault().getSeparator()) + "(\\d{4})" + Pattern.quote(FileSystems.getDefault().getSeparator());
        Pattern patternYear = Pattern.compile(regexYear);
        logger.info("Found " + confFilesList.size() + " config.json files.");

        for (File sourceFile : confFilesList){

            logger.info("File: " + sourceFile);
            //logger.info(sourceFile.getAbsolutePath());
            String eventFolderName = null;
            Matcher matcherEventFolder = patternEventFolder.matcher(sourceFile.getAbsolutePath());

            //logger.info(regexEventFolder);
            if (matcherEventFolder.find()) {
                eventFolderName = String.valueOf(matcherEventFolder.group(1));
            }

            String year = null;
            Matcher matcherYear = patternYear.matcher(sourceFile.getAbsolutePath());
            if (matcherYear.find()) {
                year = String.valueOf(matcherYear.group(1));
            }

            if (eventFolderName != null && year != null){ //target file
                File destFile = new File(EVENTS_CONF_PATH + FileSystems.getDefault().getSeparator()
                        + year + FileSystems.getDefault().getSeparator() + eventFolderName + FileSystems.getDefault().getSeparator() + sourceFile.getName());
                //logger.info(destFile.getAbsolutePath());
            try {
                Files.createDirectories(Path.of(destFile.getAbsolutePath()).getParent());
                FileUtils.copyFile(sourceFile, destFile);
                logger.info("Copied to '" + destFile.getAbsolutePath() + "'");
            } catch (IOException e) {
                logger.error("Can't copy file '" + sourceFile.getAbsolutePath() + "' to '" + destFile.getAbsolutePath() + "'");
                throw new RuntimeException(e);
            }
            }

        }*/

        logger.info("Completed.");

    }

    public static void main(String[] args) {

        new CopyEventsConfigApp();
    }
}
