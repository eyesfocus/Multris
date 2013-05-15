package de.medieninf.mobcomp.multris.game.data;

/**
 * 
 * @author marcel bechtold
 * 
 *         Class to manage Score during a running game.
 * */
public class Score {
	private final int POINTS_PER_ROW = 5;
	private final int BONUS = 50;

	int points;
	int rows;

	public Score() {
		this.points = 0;
		this.rows = 0;
	}

	public int getRows() {
		return rows;
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	/**
	 * Calculates the score depending on how many rows have been deleted
	 * 
	 * @param deletedRows
	 * @return true if points were changed, otherwise false
	 */
	public boolean calculateScore(int deletedRows) {
		points += (deletedRows * POINTS_PER_ROW);
		if (deletedRows == 4) {
			points += BONUS;
		}
		this.rows += deletedRows;
		if (deletedRows == 0) {
			return false;
		} else {
			return true;
		}
	}
}
