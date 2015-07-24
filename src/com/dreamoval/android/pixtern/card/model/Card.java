package com.dreamoval.android.pixtern.card.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.opencv.core.Point;

import android.util.Log;

/** Representation of a card that holds its patterns.*/
public class Card {
	private HashMap<String, Pattern> thePatterns; 

	public Card() {
		thePatterns = new HashMap<String, Pattern>();
	}

	public void addPattern(String resource, Point tl, Point br, double thresh) {
		thePatterns.put(resource, new Pattern(resource, tl, br, thresh));
	}
	
	public void addExtractPattern(String name, String regex, int patternLength) {
	}

	public Pattern getPattern(String resource) {
		Pattern foundPattern = null;
		if (thePatterns.containsKey(resource)) foundPattern = thePatterns.get(resource);
		return foundPattern;
	}
	
	public HashMap<String, Pattern> getPatternMap() {
		return thePatterns;
	}
		
}
