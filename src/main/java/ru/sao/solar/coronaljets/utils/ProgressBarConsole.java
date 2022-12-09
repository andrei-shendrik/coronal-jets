package ru.sao.solar.coronaljets.utils;

public class ProgressBarConsole {
	
	private double progress;
	
    //Test  10% [33m????                            ?[0m  10/100 (0:00:10 / 0:01:30) 
	//ProgressBar pb = new ProgressBar("Test", 100, ProgressBarStyle.ASCII); // name, initial max
    // Use ProgressBar("Test", 100, ProgressBarStyle.ASCII) if you want ASCII output style
    //pb.start(); // the progress bar starts timing
   // Or you could combine these two lines like this:
   //   ProgressBar pb = new ProgressBar("Test", 100).start();
	/*	 pb.step(); // step by 1
		 pb.stepBy(n); // step by n
		 ...
		 pb.stepTo(n); // step directly to n
		 ...
		 pb.maxHint(n);
		 // reset the max of this progress bar as n. This may be useful when the program
		 // gets new information about the current progress.
		 // Can set n to be less than zero: this means that this progress bar would become
		 // indefinite: the max would be unknown.
		 ...
		 pb.setExtraMessage("Reading..."); // Set extra message to display at the end of the bar
		}
		pb.stop() // stops the progress bar
	*/
	
	public ProgressBarConsole(String task, long initialMax) {
		progress = 1;
	}
	
	public void step() {
		
	}
	
	public void start() {
		
	}
	
	public void stop() {
		
	}
	
	public void addProgress(double progress) {
		this.progress += progress;
	}
	
	public double progress() {
		return progress;
	}
}
