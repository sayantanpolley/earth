package org.primefaces.showcase.view.ajax;

import java.io.IOException;

public class test {

public static void main(String[] args) throws IOException {
		
		DropdownView dr = new DropdownView();
		dr.setGenre("All");
		dr.setBook("Bucky O'Connor: A Tale of the Unfenced Border");
		dr.displayBook();
	}

}
