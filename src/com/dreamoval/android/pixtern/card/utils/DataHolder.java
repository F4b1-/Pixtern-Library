package com.dreamoval.android.pixtern.card.utils;
/**
 * Singleton Class that can be used to exchange data between applications.
 */
public class DataHolder {
	  private String data;
	  private String cardPath;
	  public String getData() {return data;}
	  public void setData(String data) {this.data = data;}
	  public void setCardPath(String cardPath) {this.cardPath = cardPath;}
	  public String getCardPath() {return cardPath;}

	  private static final DataHolder holder = new DataHolder();
	  public static DataHolder getInstance() {return holder;}
	}
