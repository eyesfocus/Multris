package de.medieninf.mobcomp.multris.game.persistence;

/**
 * @author tina schedlbauer
 * 
 *         Class to represent an Highscore-Entry
 */
public class Highscore {
	private int id;
	private String name;
	private Integer points;
	private Integer multiplayer;

	public Highscore() {

	}

	public Highscore(int id) {
		this.id = id;
	}

	/**
	 * @param name
	 *            Name of Player
	 * @param points
	 *            Number of points
	 * @param multiplayer
	 *            true if multiplayer highscore, false otherwise
	 * */
	public Highscore(String name, int points, boolean multiplayer) {
		this.name = name;
		this.points = points;
		this.multiplayer = multiplayer ? 1 : 0;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public void setMultiplayer(boolean multiplayer) {
		this.multiplayer = multiplayer ? 1 : 0;
	}

	public int isMultiplayer() {
		return multiplayer;
	}

}
