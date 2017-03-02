package org.inventivetalent.mcauth;

import java.util.Random;

public class Main {

	public static void main(String[] args) {
		String string = "";
		Random random = new Random();
		for(int i =0;i<10;i++) {
			string += String.valueOf(random.nextInt(10));
		}

		System.out.println(string);
	}

}
