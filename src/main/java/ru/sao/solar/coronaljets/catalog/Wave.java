package ru.sao.solar.coronaljets.catalog;

import java.util.LinkedHashMap;

public class Wave {
	
	private String event_id;
	private int wave;
	private String movie_ref;
	private String pict_ref;
	private String fits_ref;
    private String comment;
    
	private LinkedHashMap<String, Detail> details;

    public Wave() {
    	
    }
    
    public void details(LinkedHashMap<String, Detail> details) {
		this.details = new LinkedHashMap<String, Detail>(details);
	}
	
	public LinkedHashMap<String, Detail> details() {
		return details;
	}
    
    public void event_id(String event_id) {
    	this.event_id = event_id;
    }
    
    public String event_id() {
    	return event_id;
    }
    
    public void wave(int wave) {
    	this.wave = wave;
    }
    
    public int wave() {
    	return wave;
    }
    
    public void movie_ref(String movie_ref) {
    	this.movie_ref = movie_ref;
    }
    
    public String movie_ref() {
    	return movie_ref;
    }
    
    public void pict_ref(String pict_ref) {
    	this.pict_ref = pict_ref;
    }
    
    public String pict_ref() {
    	return pict_ref;
    }
    
    public void fits_ref(String fits_ref) {
    	this.fits_ref = fits_ref;
    }
    
    public String fits_ref() {
    	return fits_ref;
    }
    
    public void comment(String comment) {
		this.comment = comment;
	}
	
	public String comment() {
		return comment;
	}

}
