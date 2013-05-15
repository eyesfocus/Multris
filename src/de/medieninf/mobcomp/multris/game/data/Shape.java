package de.medieninf.mobcomp.multris.game.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Observable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import de.medieninf.mobcomp.multris.game.enums.Motion;
import de.medieninf.mobcomp.multris.game.enums.ShapeType;

/**
 * 
 * @author tina schedlbauer
 * @author marcel bechtold
 * 
 * Representation Class of a Tetris Shape. Does all the logic: move, rotate,
 * update..
 * */
public class Shape extends Observable implements Serializable {

	private static final long serialVersionUID = -5819442790439696885L;
	private final float DEFAULT_SPEED = 2.0f;
	private int posX;
	private int posY;
	private int bitmapId;

	private ShapeType shapeType;
	private Boolean[][] model;
	private int rotation;
	private float speed;

	/**
	 * Constructor. Creates a shape with of a random type
	 * 
	 * @param posX
	 *            Position on x-axis
	 * @param posY
	 *            Position on y-axis
	 * */
	public Shape(int posX, int posY) {
		randomize(posX, posY);
	}

	/**
	 * Constructor for cloning
	 * */
	private Shape(int posX, int posY, ShapeType shapeType, int bitmapid, int rotation, float speed, Boolean[][] model) {
		this.posX = posX;
		this.posY = posY;
		this.shapeType = shapeType;
		this.bitmapId = bitmapid;
		this.rotation = rotation;
		this.speed = speed;
		this.model = model;
	}

	/**
	 * Does default settings for the attributes. Gives the Shape a random type
	 * 
	 * @param posX
	 *            Position on x-axis
	 * @param posY
	 *            Position on y-axis
	 * 
	 * */
	public void randomize(int posX, int posY) {
		this.posX = posX;
		this.posY = posY;
		this.shapeType = ShapeType.getRandom();
		this.bitmapId = shapeType.getId();
		this.rotation = 0;
		this.speed = DEFAULT_SPEED;
		this.model = shapeType.getModel(rotation);
		notifyyy();
	}

	/**
	 * Moves the Shape one position on the x-axis to the left
	 * */
	public void moveLeft() {
		posX -= 1;
		notifyyy();
	}

	/**
	 * Moves the Shape one position on the x-axis to the right
	 * */
	public void moveRight() {
		posX += 1;
		notifyyy();
	}

	/**
	 * Rotates the Shape clockwise.
	 * */
	public void rotate() {
		rotation += 1;
		model = shapeType.getModel(rotation);
		notifyyy();
	}

	/**
	 * Moves the Shape one row further
	 * */
	public void update() {
		posY += 1;
		notifyyy();
	}
	
	/**
	 * Moves the Shape one row further
	 * */
	public void update(int value) {
		posY += value;
		notifyyy();
	}

	/**
	 * Checks if the Shape would collide with another Shape on doing a specific
	 * motion
	 * 
	 * @param otherShape
	 *            the shape it might collide with
	 * @param motion
	 *            the motion which would be done
	 * @return true if collision, else false
	 * */
	public boolean collidesWith(Shape otherShape, Motion motion) {
		Shape tmpShape = this.clone();
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
		Boolean[][] otherModel = otherShape.getModel();

		int tmpX = tmpShape.getPosX();
		int tmpY = tmpShape.getPosY();
		int otherX = otherShape.getPosX();
		int otherY = otherShape.getPosY();

		ArrayList<Point> tmpCoords = new ArrayList<Point>();

		for (int row = 0; row < tmpShape.getHeight(); row++) {
			for (int col = 0; col < tmpShape.getWidth(); col++) {
				if (tmpModel[row][col]) {
					tmpCoords.add(new Point(col + tmpX, row + tmpY));
				}
			}
		}

		Point p;
		for (int row = 0; row < otherShape.getHeight(); row++) {
			for (int col = 0; col < otherShape.getWidth(); col++) {
				if (otherModel[row][col]) {
					p = new Point(col + otherX, row + otherY);
					if (tmpCoords.contains(p)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Getter for bitmapid
	 * 
	 * @return bitmapid of the Shape (1...7)
	 */
	public int getBitmapId() {
		return bitmapId;
	}

	/**
	 * Setter for rotation
	 * 
	 * @param rotation
	 *            Bitmapid of the shape (Should be between 1 and 7)
	 */
	public void setRotation(int rotation) {
		this.rotation = rotation;
	}

	/**
	 * Setter for speed
	 * 
	 * @param speed
	 *            new Speed
	 */
	public void setSpeed(float speed) {
		this.speed = speed ;
		notifyyy();
	}

	/**
	 * Getter for value of current speed
	 * 
	 * @return value of current Speed
	 */
	public float getSpeed() {
		return speed;
	}

	/**
	 * Getter for Position on x-axis
	 * 
	 * @return position on x-axis
	 */
	public int getPosX() {
		return posX;
	}

	/**
	 * Getter for Position on y-axis
	 * 
	 * @return position on y-axis
	 */
	public int getPosY() {
		return posY;
	}

	/**
	 * Getter for model of this Shape
	 * 
	 * @return model
	 */
	public Boolean[][] getModel() {
		return model;
	}

	/**
	 * Getter for height of this Shape
	 * 
	 * @return number of rows of the Shape
	 */
	public int getHeight() {
		return model.length;
	}

	/**
	 * Getter for width of this Shape
	 * 
	 * @return number of cols the Shape
	 */
	public int getWidth() {
		return model[0].length;
	}

	/**
	 * Getter for lowest position of this Shape
	 * 
	 * @return lowest position on y-axis
	 */
	public int getBottomPosY() {
		return posY + getHeight() - 1;
	}

	/**
	 * Draws the Shape onto the Canvas
	 * 
	 * @param canvas
	 * @param bitmaps
	 *            The bitmaps that exist
	 * @param bricksize
	 *            edge length of a box
	 * */
	public void draw(Canvas canvas, Bitmap[] bitmaps, int bricksize, int top, int left, boolean myShape) {
		int col = getPosX();
		int row = getPosY();
		for (Boolean[] arrayRow : model) {
			for (Boolean data : arrayRow) {
				if (data) {
					if (myShape) {
						canvas.drawBitmap(bitmaps[bitmapId], left + col * bricksize, top + row * bricksize, null);
					} else {
						Paint transparentpainthack = new Paint();
						transparentpainthack.setAlpha(80);
						canvas.drawBitmap(bitmaps[bitmapId], left + col * bricksize, top + row * bricksize, transparentpainthack);
					}
				}
				col += 1;
			}
			col = getPosX();
			row += 1;
		}
	}

	/**
	 * Notifies its Observers
	 * */
	private void notifyyy() {
		setChanged();
		notifyObservers();
	}

	@Override
	public Shape clone() {
		return new Shape(posX, posY, shapeType, bitmapId, rotation, speed, model);
	}

	@Override
	public String toString() {
		return String.format("Shape: bitmapId: %d, PosX: %d, PosY: %d", bitmapId, posX, posY);
	}
}
