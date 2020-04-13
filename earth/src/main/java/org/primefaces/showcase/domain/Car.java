package org.primefaces.showcase.domain;

import java.io.Serializable;

public class Car implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5616802774499984921L;
	private String id;
	private String brand;
	private int year;
	private String color;
	private int price;
	private boolean randomSoldState;
	
	
	public Car(String id, String brand, int year, String color, int price, boolean randomSoldState) {
		super();
		this.id = id;
		this.brand = brand;
		this.year = year;
		this.color = color;
		this.price = price;
		this.randomSoldState = randomSoldState;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getBrand() {
		return brand;
	}
	public void setBrand(String brand) {
		this.brand = brand;
	}
	public int getYear() {
		return year;
	}
	public void setYear(int year) {
		this.year = year;
	}
	public String getColor() {
		return color;
	}
	public void setColor(String color) {
		this.color = color;
	}
	public int getPrice() {
		return price;
	}
	public void setPrice(int price) {
		this.price = price;
	}
	public boolean isRandomSoldState() {
		return randomSoldState;
	}
	public void setRandomSoldState(boolean randomSoldState) {
		this.randomSoldState = randomSoldState;
	}
	

}	