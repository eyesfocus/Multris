package de.medieninf.mobcomp.multris.game.data;

import java.io.Serializable;
import java.util.Observable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import de.medieninf.mobcomp.multris.game.GameState;
import de.medieninf.mobcomp.multris.game.enums.Motion;

/**
 * @author tina schedlbauer
 * @author marcel bechtold
 * 
 *         This Class represents the game area. It is represented by a two-dimensional int-Array.
 * */
public class Wall extends Observable implements Serializable {

	private static final long serialVersionUID = -1934607071978424931L;
	private int[][] wall;
	private int rows;
	private int cols;

	/**
	 * Constructor for Wall Object
	 * 
	 * */
	public Wall() {
		this.cols = GameState.COLS;
		this.rows = GameState.ROWS;
		wall = new int[rows + 1][cols];
		fillWall();
	}

	/**
	 * Fills the wall-Array with initial values, which are 0 in every position
	 * */
	private void fillWall() {
		for (int row = 0; row < rows + 1; row++) {
			for (int col = 0; col < cols; col++) {
				wall[row][col] = 0;
			}
		}
	}

	/**
	 * Puts a Shape into the Wall.
	 * 
	 * @param shape
	 *            Shape that has to be "transformed" into Wall
	 * 
	 * @return true if successfully put into wall false if there was no more space left on the top of the wall (GAME
	 *         OVER!)
	 * */
	public boolean putShapeIntoWall(Shape shape) {
		int col = shape.getPosX();
		int row = shape.getPosY();

		Boolean model[][] = shape.getModel();
		for (Boolean[] arrayRow : model) {
			for (Boolean colVal : arrayRow) {
				if (colVal) {
					// Check Game Over
					if (isFull(col)) {
						return false;
					} else {
						wall[row][col] = shape.getBitmapId();
						wall[rows][col] = calculateTowerheight(col);
					}
				}
				col += 1;
			}
			col = shape.getPosX();
			row += 1;
		}
		// printHeights();
		return true;
	}

	/**
	 * Checks if there is a row that has to be deleted. It starts with the highest row. If there is a full one, it gets
	 * deleted.
	 * 
	 * @return number of deleted rows *
	 */
	public int checkRows() {
		int highest = getHeightOfHighestTower();
		int deleted = 0;
		for (int row = rows - highest; row < rows; row++) {
			if (isRowFull(row)) {
				deleted++;
				deleteRow(row);
				updateHeights();
			}
		}
		return deleted;
	}

	/**
	 * Parses the wall-Array and actualizes the heights depending on the filling
	 * */
	private void updateHeights() {
		for (int col = 0; col < cols; col++) {
			wall[rows][col] = calculateTowerheight(col);
		}
	}

	/**
	 * Calculates the current towerheight of a specific col
	 * 
	 * @param col
	 *            Column you want the height of
	 * @return towerheight of col
	 * */
	private int calculateTowerheight(int col) {
		for (int row = 0; row < rows; row++) {
			if (isOccupied(row, col)) {
				return rows - row;
			}
		}
		return 0;
	}

	/**
	 * Finds the Height of the currently highest Tower
	 * 
	 * @return height of highest tower
	 * */
	private int getHeightOfHighestTower() {
		int highest = 0;
		for (int col = 0; col < cols; col++) {
			if (highest < getTowerHeight(col)) {
				highest = getTowerHeight(col);
			}
		}
		return highest;
	}

	/**
	 * Checks if a row is completely occupied
	 * 
	 * @param row
	 *            Row that needs to be checked
	 * @return true if full, else false
	 * */
	private boolean isRowFull(int row) {
		for (int col = 0; col < cols; col++) {
			if (!isOccupied(row, col)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Deletes a row from the wall and pulls down the upper rows
	 * 
	 * @param row
	 *            Row that has to be deleted
	 * */
	private void deleteRow(int row) {
		for (int helperrow = row; helperrow > rows - 1 - getHeightOfHighestTower(); helperrow--) {
			for (int col = 0; col < cols; col++) {
				wall[helperrow][col] = wall[helperrow - 1][col];
			}
		}
	}

	/**
	 * Checks if the height of the specified column is higher than the maximum tower height.
	 * 
	 * @param col
	 *            Column that has to be checked
	 * @return true if one col is higher than the maximum tower height, else false
	 * */
	private boolean isFull(int col) {
		int maxTowerHeight = rows;
		return wall[rows][col] + 1 > maxTowerHeight;
	}

	/**
	 * Checks if the brick on position col/row is currently occupied
	 * 
	 * @param col
	 *            Column
	 * @param row
	 *            Row
	 * @return true if occupied, else false
	 * */
	private boolean isOccupied(int row, int col) {
		return wall[row][col] != 0;
	}

	/**
	 * Checks if the Shape is still in the gamearea.
	 * 
	 * @param tmpShape
	 *            Shape that has to be checked
	 * @param posX
	 *            current position of Shape on x-axis
	 * @param posY
	 *            current position of Shape on y-axis
	 * @return true if in game area, false else
	 * */

	private boolean inGameArea(Shape tmpShape, int posX, int posY) {
		int shapeWidth = tmpShape.getWidth();
		int shapeHeight = tmpShape.getHeight();
		if (shapeWidth + posX > cols) {
			return false;
		}
		if (posX < 0) {
			return false;
		}
		if (shapeHeight + posY > rows) {
			return false;
		}
		return true;
	}

	/**
	 * Checks if the shape is docked
	 * 
	 * @param shape
	 *            Shape that has to be checked
	 * @return true if docked, else false
	 * */
	public boolean isShapeDocked(Shape shape) {
		Boolean model[][] = shape.getModel();
		int row = shape.getPosY();
		int col = shape.getPosX();

		for (Boolean[] arrayRow : model) {
			for (Boolean colVal : arrayRow) {
				if (colVal) {
					if (wall[row + 1][col] != 0) {
						return true;
					}
				}
				col += 1;
			}
			col = shape.getPosX();
			row += 1;
		}
		return false;
	}

	/**
	 * Checks if the Shape is colliding with the Wall (Edges or already�� docked Shapes)
	 * 
	 * @param shape
	 *            Shape that is going to be rotated
	 * @param motion
	 *            sort of Motion
	 * @return true if colliding, else false
	 */
	public boolean collidesWith(Shape shape, Motion motion) {
		Shape tmpShape = shape.clone();

		switch (motion) {
		case ROTATE:
			tmpShape.rotate();
			break;
		case LEFT:
			tmpShape.moveLeft();
			break;
		case RIGHT:
			tmpShape.moveRight();
			break;
		case DOWN:
			tmpShape.update();
			break;
		}

		Boolean[][] tmpModel = tmpShape.getModel();
		int tmpX = tmpShape.getPosX();
		int tmpY = tmpShape.getPosY();
		int tmpHeight = tmpShape.getHeight();
		int tmpWidth = tmpShape.getWidth();

		if (!inGameArea(tmpShape, tmpX, tmpY)) {
			return true;
		}

		for (int row = 0; row < tmpHeight; row++) {
			for (int col = 0; col < tmpWidth; col++) {
				if (tmpModel[row][col]) {
					if (isOccupied(row + tmpY, col + tmpX)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Getter for towerheight of a specific column
	 * 
	 * @param col
	 *            Column that you want the height of
	 * @return height of the column col
	 * */
	public int getTowerHeight(int col) {
		return wall[rows][col];
	}

	/**
	 * Getter for wall array
	 * */
	public int[][] getWall() {
		return wall;
	}

	/**
	 * Draws the Wall onto the Canvas
	 * 
	 * @param canvas
	 *            Canvas the Wall has to be drawn on
	 * @param bitmaps
	 *            The bitmaps that exist
	 * @param bricksize
	 *            edge length of a box
	 * 
	 * */
	public void draw(Canvas canvas, Bitmap[] bitmaps, int top, int left, int bricksize) {
		int id;
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				id = wall[row][col];
				canvas.drawBitmap(bitmaps[id], left + col * bricksize, top + row * bricksize, null);
			}
		}
	}

	/**
	 * Notifies its Observers
	 * 
	 * @param pId
	 * */
	private void notifyyy(String pId) {
		setChanged();
		notifyObservers(pId);
	}

	/**
	 * Notifies its Observers after a Shape has docked
	 * 
	 * @param pId
	 *            ID of the player whose Shape has docked
	 * */
	public void onDockingCompleted(String pId) {
		notifyyy(pId);
	}

	@Override
	public String toString() {
		return String.format("Wall: cols: %d, rows: %d", cols, rows);
	}
}
