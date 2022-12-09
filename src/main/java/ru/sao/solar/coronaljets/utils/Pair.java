package ru.sao.solar.coronaljets.utils;

public class Pair {
	
	private int index;
	private double value;
	
	public Pair() {
		
	}
	
	public int index() {
		return index;
	}
	
	public double value() {
		return value;
	}
	
	public Pair index(int index, double value) {
		this.index = index;
		this.value = value;
		return this;
	}

}
