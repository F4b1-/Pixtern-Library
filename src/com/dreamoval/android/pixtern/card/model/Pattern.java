package com.dreamoval.android.pixtern.card.model;

import org.opencv.core.Point;
/**
 * Representation of a Pattern of a card than can either be symbol on the card or a text pattern.
 */
public class Pattern {
	private String resource;
	private Point tl;
	private Point br;
	private double thresh;
	
	public Pattern(String resource, Point tl, Point br, double thresh) {
		this.resource = resource;
		this.tl = tl;
		this.br = br;
		this.thresh = thresh;
	}
	
	public double getThresh() {
		return thresh;
	}

	public void setThresh(double thresh) {
		this.thresh = thresh;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public Point getTl() {
		return this.tl;
	}

	public void setTl(Point tl) {
		this.tl = tl;
	}

	public Point getBr() {
		return br;
	}

	public void setBr(Point br) {
		this.br = br;
	}
	
}
