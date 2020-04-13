package org.primefaces.showcase.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class Book {

	public Book() {
		
	}
	
	public Map<Integer,String> genres;
	public String bookQryString;
	public int topK;
	public String bookId;
	List<Map<String, String>> totalResults;
	
	
	
	public String getBookId() {
		return bookId;
	}
	public void setBookId(String bookId) {
		this.bookId = bookId;
	}
	public Map<Integer, String> getGenres() {
		return genres;
	}
	public void setGenres(Map<Integer, String> genres) {
		this.genres = genres;
	}
	
	public String getBookQryString() {
		return bookQryString;
	}
	public void setBookQryString(String bookQryString) {
		this.bookQryString = bookQryString;
	}
	
	
	public int getTopK() {
		return topK;
	}
	public void setTopK(int topK) {
		this.topK = topK;
	}
	public List<Map<String, String>> getTotalResults() {
		return totalResults;
	}
	public void setTotalResults(List<Map<String, String>> totalResults) {
		this.totalResults = totalResults;
	}
	
	

}
