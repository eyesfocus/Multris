package de.medieninf.mobcomp.multris.network.helpers;

import java.io.Serializable;

/** 
 * @author marcel bechtold
 * @author tina schedlbauer
 * 
 * simple Point class stores an x- and an y-Coordinate 
 * */
public class Point implements Serializable {

	private static final long serialVersionUID = -2294195561568053417L;
	private int x;
	private int y;

	/**
	 * Constructor
	 * 
	 * @param x
	 *            x-Coordinate
	 * @param y
	 *            y-Coordinate
	 */
	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Getter for x
	 * 
	 * @return x-Coordinate
	 */
	public int getX() {
		return x;
	}

	/**
	 * Setter for x
	 * 
	 * @param x
	 *            x-Coordinate
	 */
	public void setX(int x) {
		this.x = x;
	}

	/**
	 * Getter for y
	 * 
	 * @return y-Coordinate
	 */
	public int getY() {
		return y;
	}

	/**
	 * Setter for y
	 * 
	 * @param y
	 *            y-Coordinate
	 */
	public void setY(int y) {
		this.y = y;
	}
}
