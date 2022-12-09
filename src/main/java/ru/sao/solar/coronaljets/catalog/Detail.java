package ru.sao.solar.coronaljets.catalog;

import java.util.Date;

public class Detail {
	
	//private String detail_id;
	private String wave_id;
	private int ndet;
	private Date tstart;
	private Date tmax;
	private Date tend;
	private int cardmax;
	private double asp_jet;
	private double asp_max;
	private double asp_ltow;
	private double total_length;
	private double av_width;
	private double speed;
	private double x_from;
	private double x_to;
	private double y_from;
	private double y_to;
	private String movie_ref;
	private String pict_ref;
	private String fits_ref;
	private String sav_ref;
	private String comment;
	
	public Detail() {
		
	}
	
	public void wave_id(String wave_id) {
    	this.wave_id = wave_id;
    }
    
    public String wave_id() {
    	return wave_id;
    }
	
	public void ndet(int ndet) {
    	this.ndet = ndet;
    }
    
    public int ndet() {
    	return ndet;
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
	
	public void tmax(Date tmax) {
		this.tmax = tmax;
	}
	
	public Date tmax() {
		return tmax;
	}
	
	public void cardmax(int cardmax) {
    	this.cardmax = cardmax;
    }
    
    public int cardmax() {
    	return cardmax;
    }
	
	public void asp_jet(double asp_jet) {
    	this.asp_jet = asp_jet;
    }
    
    public double asp_jet() {
    	return asp_jet;
    }
	
	public void asp_max(double asp_max) {
    	this.asp_max = asp_max;
    }
    
    public double asp_max() {
    	return asp_max;
    }
    
    public double asp_ltow() {
    	return asp_ltow;
    }
    
    public void asp_ltow(double asp_ltow) {
    	this.asp_ltow = asp_ltow;
    }
	
	public void total_length(double total_length) {
    	this.total_length = total_length;
    }
    
    public double total_length() {
    	return total_length;
    }
	
	public void av_width(double av_width) {
    	this.av_width = av_width;
    }
    
    public double av_width() {
    	return av_width;
    }
	
	public void speed(double speed) {
    	this.speed = speed;
    }
    
    public double speed() {
    	return speed;
    }
	
	public void x_from(double x_from) {
    	this.x_from = x_from;
    }
    
    public double x_from() {
    	return x_from;
    }
    
    public void x_to(double x_to) {
    	this.x_to = x_to;
    }
    
    public double x_to() {
    	return x_to;
    }
    
    public void y_from(double y_from) {
    	this.y_from = y_from;
    }
    
    public double y_from() {
    	return y_from;
    }
    
    public void y_to(double y_to) {
    	this.y_to = y_to;
    }
    
    public double y_to() {
    	return y_to;
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
    
    public void sav_ref(String sav_ref) {
    	this.sav_ref = sav_ref;
    }
    
    public String sav_ref() {
    	return sav_ref;
    }

	public void comment(String comment) {
		this.comment = comment;
	}
	
	public String comment() {
		return comment;
	}
}
