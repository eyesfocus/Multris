package de.medieninf.mobcomp.multris.network;

import java.io.Serializable;

import de.medieninf.mobcomp.multris.game.data.Shape;
import de.medieninf.mobcomp.multris.game.data.Wall;

/**
 * @author marcel bechtold
 * 
 *         This Class stores Tetris information for Bluetooth Communication
 * */
public class BTMessage implements Serializable {

	private static final long serialVersionUID = -4519851900116011454L;

	public static final int SHAPE = 0;
	public static final int WALL = 1;
	public static final int POINTS = 2;
	public static final int GAME_OVER = 3;
	public static final int START_GAME = 4;
	public static final int PAUSE_GAME = 5;
	public static final int RESUME_GAME = 6;

	private String pID;
	private int mID;
	private Object data; // TODO nichts änderbares beim serialisieren

	/**
	 * Constructor for a BTMessage that stores a Shape
	 * 
	 * @param mID
	 *            message Id
	 * @param pID
	 *            player Id
	 * @param shape
	 * */
	public BTMessage(int mID, String pID, Shape shape) {
		this.pID = pID;
		this.mID = mID;
		this.data = shape;
	}

	/**
	 * Constructor for a BTMessage that stores a Wall
	 * 
	 * @param mID
	 *            message Id
	 * @param pID
	 *            player Id
	 * @param wall
	 * */
	public BTMessage(int mID, String pID, Wall wall) {
		this.pID = pID;
		this.mID = mID;
		this.data = wall;

	}

	/**
	 * Constructor for a BTMessage that stores a gameOver info
	 * 
	 * @param mID
	 *            message Id
	 * @param pID
	 *            player Id
	 * @param gameOver
	 * */
	public BTMessage(int mID, String pID, boolean gameOver) {
		this.pID = pID;
		this.mID = mID;
		this.data = gameOver;

	}

	/**
	 * Constructor for a BTMessage that stores a "pointschanged" info
	 * 
	 * @param mID
	 *            message Id
	 * @param pID
	 *            player Id
	 * @param points
	 */
	public BTMessage(int mID, String pID, int points) {
		this.pID = pID;
		this.mID = mID;
		this.data = points;

	}

	/**
	 * Constructor for a BTMessage that only stores the messageID
	 * 
	 * @param mID
	 *            message Id
	 * @param pID
	 *            player Id
	 * @param points
	 */
	public BTMessage(int mID) {
		this.pID = "";
		this.mID = mID;
		this.data = null;

	}

	/**
	 * Constructor for a BTMessage that stores a messageID and playerID without data
	 * 
	 * @param mID
	 *            message Id
	 * @param pID
	 *            player Id
	 * @param points
	 */
	public BTMessage(int mID, String pID) {
		this.pID = pID;
		this.mID = mID;
		this.data = null;

	}

	/**
	 * Getter for pID
	 * 
	 * @return pID player Id
	 */
	public String getpID() {
		return pID;
	}

	/**
	 * Getter for mID
	 * 
	 * @return mID message Id
	 */
	public int getmID() {
		return mID;
	}

	/**
	 * Getter for data
	 * 
	 * @return data (Shape, Wall or gameOver)
	 */
	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
}
