package ru.sao.solar.coronaljets.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MyUtils {
	
	final static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static Date parseDate(String dateString, List<String> dateFormatList, String timezone){

		for (String dateFormat : dateFormatList){
			SimpleDateFormat df = new SimpleDateFormat(dateFormat);
			//df.setTimeZone(TimeZone.getTimeZone("UTC"));
			df.setTimeZone(TimeZone.getTimeZone(timezone));

			try {
				return new SimpleDateFormat(dateFormat).parse(dateString);
			}
			catch (ParseException e) {}
		}

		return null;
	}

	public static void printList(List<?> list) {
		
		for(Object elem : list) {
            System.out.println(elem.toString());
        }
		//System.out.println(Arrays.toString(list.toArray()));
	}
	
	public static void printArray(Object[] array) {
		System.out.println(Arrays.toString(array));
	}
	
	public static void printArray(int[] array) {
		System.out.println(Arrays.toString(array));
	}
	
	public static void printArray(double[] array) {
		System.out.println(Arrays.toString(array));
	}
	
	public static void printArray(long[] array) {
		System.out.println(Arrays.toString(array));
	}
	
	public static long minValue(long[] array) {
	    long minValue = array[0];
	    for (int i = 1; i < array.length; i++) {
	        if (array[i] < minValue) {
	            minValue = array[i];
	        }
	    }
	    return minValue;
	}
	
	public static long minIndex(long[] array) {
	    long minValue = array[0];
	    for (int i = 1; i < array.length; i++) {
	        if (array[i] < minValue) {
	            minValue = array[i];
	        }
	    }
	    return minValue;
	}
	
	public static Pair min(double[] arr) {
		double min = arr[0];
		int index = 0;
		   
	    for(int i=1; i<arr.length; i++){
	    		if (arr[i] < min) {
	    			min = arr[i];
	    			index = i;
	    		}
	    	}
	    return new Pair().index(index, min);
	}
	
	public static Pair max(double[] arr) {
		double max = arr[0];
		int index = 0;
		   
	    for(int i=1; i<arr.length; i++){
	    		if (arr[i] > max) {
	    			max = arr[i];
	    			index = i;
	    		}
	    	}
	    return new Pair().index(index, max);
	}
	
	public static Pair min(long[] arr) {
		long min = arr[0];
		int index = 0;
		   
	    for(int i=1; i<arr.length; i++){
	    		if (arr[i] < min) {
	    			min = arr[i];
	    			index = i;
	    		}
	    	}
	    return new Pair().index(index, min);
	}
	
	public static Pair max(long[] arr) {
		long max = arr[0];
		int index = 0;
		   
	    for(int i=1; i<arr.length; i++){
	    		if (arr[i] > max) {
	    			max = arr[i];
	    			index = i;
	    		}
	    	}
	    return new Pair().index(index, max);
	}
	
	public static double round(double d, int decimalPlace) {
		BigDecimal bd = new BigDecimal(Double.toString(d));
        //bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        bd = bd.setScale(decimalPlace, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
	
	public static double roundUp(double d, int decimalPlace) {
		BigDecimal bd = new BigDecimal(Double.toString(d));
        //bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        bd = bd.setScale(decimalPlace, RoundingMode.UP);
        return bd.doubleValue();
    }
	
	public static double roundDown(double d, int decimalPlace) {
		BigDecimal bd = new BigDecimal(Double.toString(d));
        //bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        bd = bd.setScale(decimalPlace, RoundingMode.DOWN);
        return bd.doubleValue();
    }
	
	public static Date stringToDate(String dateString, String formatString) {
		
		SimpleDateFormat dateFormat = new SimpleDateFormat(formatString);
		Date date = null;
		try {
			date = dateFormat.parse(dateString);
		} catch (ParseException e) {
			logger.error("Error: ", new Exception("parsing string to date: '" + dateString + "' with format '" + formatString + "'"), e);
		}
		return date;
	}

	/**
	 * Get all files from a directory.
	 */
	public static List<File> listAllFilesInDir(File directory) {

		List<File> files = new ArrayList<File>();
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
		return files;
	}
	
	/**
	 * Get all files from a directory.
	 */
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

	/**
	 * Get all directories from a directory.
	 */
	public static void listAllDirectoriesInDir(File directory, List<File> directories) {

		File[] fList = directory.listFiles();
		if (fList != null)
			for (File file : fList) {
				if (file.isDirectory()) {
					directories.add(file);
					listAllDirectoriesInDir(new File(file.getAbsolutePath()), directories);
				}
			}
	}

	/**
	 * is URL
	 */
	public static boolean isValidURL(String url) {

		try {
			new URL(url).toURI();
		} catch (MalformedURLException | URISyntaxException e) {
			return false;
		}

		return true;
	}
}
