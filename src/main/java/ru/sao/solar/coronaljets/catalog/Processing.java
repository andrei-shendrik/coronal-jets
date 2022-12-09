package ru.sao.solar.coronaljets.catalog;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import ru.sao.solar.coronaljets.utils.Excel;
import ru.sao.solar.coronaljets.utils.MyUtils;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Processing {

	final static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public Processing(Settings settings) {
		processing(settings);
	}

	public void processing(Settings settings) {

		/**
		 * read events-conf
		 * логика
		 *
		 * создаю список всех файлов (листинг) конфига.
		 * создаю тредпул
		 * каждому таску передаю один файл конфига
		 * читаю поля события
		 * проверяю подпадает ли даты под интервал
		 *
		 */

		List<File> eventConfigPathList = getEventConfigPathList(new File(settings.eventsConfigPath()));

		/**
		 * read xlsx events table
		 */
		LinkedHashMap<String, String[]> mapXLSX = Excel.readXLSX(new File(settings.eventsCatalogTable()));

		/**
		 * establish db conn
		 */
		settings.testConnection();

		/**
		 * executor service
		 */

		//ExecutorService service = Executors.newSingleThreadExecutor();
		ExecutorService executorService = Executors.newFixedThreadPool(settings.numThreads());

		List<EventProcessingTask> taskList = new ArrayList<>(); //Future<Integer>

		for (File eventConfPath : eventConfigPathList) {
			//EventProcessingTask task = new EventProcessingTask(settings, progressBar);
			EventProcessingTask task = new EventProcessingTask(eventConfPath, settings, mapXLSX);
			taskList.add(task);

			//Future<Integer> future = execService.submit(task);
			//map.put(f, task.getId());
			//resultList.add(future);
		}

		List<Future<Event>> futureList = new ArrayList<>(); //Future<Integer>
		try {
			futureList = executorService.invokeAll(taskList);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));

		int num = 1;
		for (Future<Event> future : futureList) {
			try {
				Event event = future.get(); // future.get() the results of the tasks in the order it was submitted.
				logger.info("------");
				logger.info("Event folder #" + num);
				logger.info(event.getEventLog());
				logger.info("Parameters: ");
				if (event.tstartProcessing() != null){
					logger.info("Event tstart processing time: " + df.format(event.tstartProcessing()));
				} else {
					logger.warn("Event tstart processing is null");
				}

				if (event.trefProcessing() != null){
					logger.info("Event tref processing time: " + df.format(event.trefProcessing()));
				} else {
					logger.warn("Event tref processing is null");
				}

				if (event.tendProcessing() != null){
					logger.info("Event tend processing time: " + df.format(event.tendProcessing()));
				} else {
					logger.warn("Event tend processing is null");
				}

				if (event.tstart() != null){
					logger.info("Event tstart time: " + df.format(event.tstart()));
				} else {
					logger.warn("Event tstart is null");
				}

				if (event.tend() != null){
					logger.info("Event tend time: " + df.format(event.tend()));
				} else {
					logger.warn("Event tend is null");
				}

				//logger.info("Event tref time: " + df.format(event.tref()));
				logger.info("Event hek URL: " + event.hekURL());
				/*System.out.println("Future result is - " + " - " + future.get() + "; And Task done is "
						+ future.isDone());*/
				num++;
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}

		executorService.shutdown();
	}

	/**
	 * list event folders
	 */
	public List<File> getEventConfigPathList(File directory){

		List<File> subDirectories = new ArrayList<File>();
		MyUtils.listAllDirectoriesInDir(directory, subDirectories);
		//MyUtils.printList(subDirectories);
		//FileFilter fileFilter = new RegexFileFilter("^\\d{8}_\\d{6}_\\d{8}_\\d{6}_\\-*\\d+_\\-*\\d+_\\d+_\\d+$");
		String regex = "^\\d{8}_\\d{6}_\\d{8}_\\d{6}_\\-*\\d+_\\-*\\d+_\\d+_\\d+$";
		FileFilter fileFilter = new RegexFileFilter(regex);

		List<File> eventDirectories = new ArrayList<File>();
		for (File dir : subDirectories) {
			File[] dirs = dir.listFiles(fileFilter);
			for (File file : dirs) {
				eventDirectories.add(file);
				//System.out.println(file);
			}

			if (dir.length() == 0) {
				String file = dir.getParentFile().getName();
				if (file.matches(regex) && !eventDirectories.contains(dir.getParentFile())) {
					eventDirectories.add(dir.getParentFile());
					//System.out.println(dir.getParentFile());
				}

				//if no folders in event folder
				String file1 = dir.getName();
				if (file1.matches(regex) && !eventDirectories.contains(dir)) {
					eventDirectories.add(dir);
					//System.out.println(dir);
				}
			}
		}

		/*File[] directories = new File(settings.dataPathProcessing()).listFiles(File::isDirectory);
		int dirNum = directories.length;*/

		int dirCount = eventDirectories.size();
		logger.info("Found event folders in 'events-conf' directory: " + dirCount);

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));

		return eventDirectories;
	}

		/*class MyCallable implements Callable<Boolean> {

		private ProgressBarConsole pb;

		public MyCallable(ProgressBarConsole pb) {
			this.pb = pb;
		}

	    @Override
	    public Boolean call() throws Exception {

	    	long total = 100;
			long startTime = System.currentTimeMillis();

			while (true) {
				printProgress(startTime, total, (int) pb.progress(), 20);
				    			try {
				    TimeUnit.MILLISECONDS.sleep(10);
				} catch (InterruptedException e) {
				    e.getStackTrace();
				    return false;
				}
			}

				        for (int i = 0; i <= 10000; i++) {

				printProgress(startTime, total, (int) pb.progress(), 20);

				//System.out.println("PR: " + pb.progress());
			    //System.out.print("#");
			    try {
			        TimeUnit.MILLISECONDS.sleep(10);
			    } catch (InterruptedException e) {
			        e.getStackTrace();
			        return false;
			    }
			}
	        //return true;
	    }
	}*/

	/*public boolean processing2(Settings settings) {

		*//**
		 * read xlsx events table
		 *//*
		LinkedHashMap<String, String[]> mapXLSX = Excel.readXLSX(new File(settings.eventsCatalogTable()));

		*//**
		 * establish db conn
		 *//*

		settings.testConnection();

		//Connection conn = settings.getDBConnection();

		*//**
		 * test
		 *
		 * получить результат text (string), отсортировать в нужном порядке, и только
		 * после этого записать логгером на лету не получится
		 *//*

		//progressbar test
		//ProgressBarConsole progressBar = new ProgressBarConsole("Test", 100);
		*//*MyCallable callable1 = new MyCallable(progressBar);
		FutureTask<Boolean> futureTask1 = new FutureTask<>(callable1);
		executorService.execute(futureTask1);*//*

		

		//int taskCount = 100;
		*//*
		 * total = count of tasks
		 * i = task number
		 *//*
		*//*long total = 5;
		long startTime = System.currentTimeMillis();
		
		for (int i = 1; i <= total; i++) {
		    try {
		        Thread.sleep(1000);
		        printProgress(startTime, total, i, 20);
		    } catch (InterruptedException e) {
		    }
		}*//*

		//------------------

		*//*		Future future = service.submit(new Callable(){
				    public Object call() throws Exception {
				        //System.out.println("Another thread was executed");
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
				        return "result";
				    }
				});*//*

		*//*		while (!future.isDone()) {
					System.out.print("Processing...\r");
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}*//*

		//Future<?> f = executor.submit(task);
		//map.put(f, task.getId());

		*//*	    for (int i = 0; i < futureList.size(); i++) {
		    Future<Map<String, String>> future = futureList.get(i)
		    try {
		        Map<String, String> resultMap = future.get();
		
		        for(String key : resultMap.keySet()) {
		            System.out.println(resultMap.get(key));
		        }
		    } catch(ExecutionException ee) {
		        System.out.println("Exception in task " + taskList.get(i).getId());
		    }
		}*//*

		*//* Handling exception of you executing task.
		* 
		SubmitTask handleException = new SubmitTask();
		Callable<Long> someTask = callableTasks.get(0);
		Future<Long> calledSomeTask = handleException.submitTask(someTask);
		try {
		   calledSomeTask.get();
		} catch (Exception e) {
		   System.out.println("Caught the exception, handle carefully!!!");
		} finally {
		   handleException.shutdownNow();
		}        
		How to cancel further task in Executor Service ?
		
		SubmitTask cancelTask = new SubmitTask();
		int counter = 0;
		for (Callable<Long> task : callableTasks) {
		   Future<Long> future = cancelTask.submitTask(task);
		   if(counter == 0) {
		       // future.get() is blocking call
		       Long computedResult = future.get();
		       System.out.println(computedResult);
		   } else {
		       // cancel all our task from processing
		       future.cancel(true);
		   }
		   ++counter;
		}
		cancelTask.shutdownNow();*//*

		//Integer result = future.get();
		//boolean canceled = future.cancel(true);

		*//**
		 * list event folders
		 *//*
		List<File> subDirectories = new ArrayList<File>();
		MyUtils.listAllDirectoriesInDir(new File(settings.eventsConfigPath()), subDirectories);
		//MyUtils.printList(subDirectories);
		//FileFilter fileFilter = new RegexFileFilter("^\\d{8}_\\d{6}_\\d{8}_\\d{6}_\\-*\\d+_\\-*\\d+_\\d+_\\d+$");
		String regex = "^\\d{8}_\\d{6}_\\d{8}_\\d{6}_\\-*\\d+_\\-*\\d+_\\d+_\\d+$";
		FileFilter fileFilter = new RegexFileFilter(regex);

		List<File> eventDirectories = new ArrayList<File>();
		for (File dir : subDirectories) {
			
			File[] dirs = dir.listFiles(fileFilter);
			for (File file : dirs) {
				eventDirectories.add(file);
				//System.out.println(file);
			}
			
			if (dir.length() == 0) {
				String file = dir.getParentFile().getName();
				if (file.matches(regex) && !eventDirectories.contains(dir.getParentFile())) {
					eventDirectories.add(dir.getParentFile());
					//System.out.println(dir.getParentFile());
				}
				
				//if no folders in event folder
				String file1 = dir.getName();
				if (file1.matches(regex) && !eventDirectories.contains(dir)) {
					eventDirectories.add(dir);
					//System.out.println(dir);
				}
			}
		}

		*//*File[] directories = new File(settings.dataPathProcessing()).listFiles(File::isDirectory);
		int dirNum = directories.length;*//*

		int dirCount = eventDirectories.size();
		//int eventNum = 1;
		logger.info("Found event folders in 'events-conf' directory: " + dirCount);

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));

		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf1.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		*//**
		 * executor service
		 *//*
		
		//ExecutorService service = Executors.newSingleThreadExecutor();
		ExecutorService executorService = Executors.newFixedThreadPool(settings.numThreads());
				
		List<EventProcessingTask> taskList = new ArrayList<>(); //Future<Integer>
		
		for (File dir : eventDirectories) {
			//EventProcessingTask task = new EventProcessingTask(settings, progressBar);
			EventProcessingTask task = new EventProcessingTask(dir, settings, mapXLSX);
			taskList.add(task);
			
			//Future<Integer> future = execService.submit(task);
			//map.put(f, task.getId());
			//resultList.add(future);
		}
		
		List<Future<Integer>> futureList = new ArrayList<>(); //Future<Integer>
		try {
			futureList = executorService.invokeAll(taskList);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		for (Future<Integer> future : futureList) {
			try {
				future.get();
				*//*System.out.println("Future result is - " + " - " + future.get() + "; And Task done is "
						+ future.isDone());*//*
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}

		executorService.shutdown();



		*//*try {
			conn.close();
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}*//*
		return true;
	}*/

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

	/**
	 * best format: procent [blocks] taskNum/TotalTasks (elapsedsec / eta )
	 */
	/*private static void printProgress(long startTime, long total, long current, int width) {
		long eta = current == 0 ? 0 : (total - current) * (System.currentTimeMillis() - startTime) / current;
	
		String etaHms = current == 0 ? "N/A"
				: String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(eta),
						TimeUnit.MILLISECONDS.toMinutes(eta) % TimeUnit.HOURS.toMinutes(1),
						TimeUnit.MILLISECONDS.toSeconds(eta) % TimeUnit.MINUTES.toSeconds(1));
	
		StringBuilder string = new StringBuilder(10);
		int percent = (int) (current * 100 / total);
	
		int numPounds = (int) (percent * width / 100); //width in chars
		string.append('\r')
				.append(String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")))
				.append(String.format(" %d%% [", percent)).append(String.join("", Collections.nCopies(numPounds, "=")))
				.append('>').append(String.join("", Collections.nCopies(width - numPounds, " "))).append(']')
				.append(String.join("",
						Collections.nCopies((int) (Math.log10(total)) - (int) (Math.log10(current)), " ")))
				.append(String.format(" %d/%d, ETA: %s", current, total, etaHms));
		System.out.print(string);
	
		if (percent == 100)
			System.out.print("\n");
	}*/

}
