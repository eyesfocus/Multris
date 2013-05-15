package de.medieninf.mobcomp.multris.game;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import de.medieninf.mobcomp.multris.game.data.Score;
import de.medieninf.mobcomp.multris.game.data.Shape;
import de.medieninf.mobcomp.multris.game.data.Wall;
import de.medieninf.mobcomp.multris.game.enums.Motion;
import de.medieninf.mobcomp.multris.game.enums.Speed;
import de.medieninf.mobcomp.multris.network.BTMessage;

/**
 * 
 * @author tina schedlbauer
 * @author marcel bechtold
 * 
 *         GameState contains all the information of the current state of the game. It is indirectly updated by the
 *         GameThread and manipulated by user input (via TetrisView)
 * */
public class GameState extends Observable {

	private AtomicBoolean gameOver = new AtomicBoolean(false);
	private AtomicBoolean leftTouched = new AtomicBoolean(false);
	private AtomicBoolean rightTouched = new AtomicBoolean(false);
	private AtomicBoolean rotateTouched = new AtomicBoolean(false);
	private AtomicBoolean speedUpTouched = new AtomicBoolean(false);
	private AtomicBoolean slowDownTouched = new AtomicBoolean(false);
	private LinkedBlockingQueue<BTMessage> incomingQueue = new LinkedBlockingQueue<BTMessage>();

	private static int ticks = 0;

	public static final int COLS = 10;
	public static final int ROWS = 20;

	private Map<String, Shape> otherShapes; // otherPlayersId -> Shape
	private Shape myShape;
	private Wall wall;

	private boolean isMultiplayer;
	private boolean isServer;
	private boolean paused;
	private String myId;
	private Score score;
	boolean scoreChanged;

	private int playerNo;
	private int playerCount;

	/**
	 * Constructor
	 * 
	 * @param rows
	 *            number of rows of the game area
	 * */
	public GameState(boolean isServer, boolean isMultiplayer, int playerNo, int playerCount) {
		this.isServer = isServer;
		this.isMultiplayer = isMultiplayer;
		this.playerNo = playerNo;
		this.playerCount = playerCount;
		otherShapes = new HashMap<String, Shape>();
		myShape = new Shape(calculateInitialXPos(playerNo, playerCount), 0);
		wall = new Wall();
		score = new Score();
	}

	/**
	 * Calculates the initial x-position of a Shape.
	 * 
	 * @return initial x-Position of the Shape
	 */
	private int calculateInitialXPos(int playerNo, int playerCount) {
		if (isMultiplayer) {
			int fieldSize = COLS / playerCount;
			if (isServer) {
				return 1;
			} else {
				return (int) (fieldSize * (playerNo) + 1);
			}
		} else {
			return (int) (COLS / 2.0);
		}
	}

	/**
	 * Controls the progress of the GameState.
	 * 
	 * */
	public void tick() {
		ticks += 1;
		if (isMultiplayer) {
			while (!incomingQueue.isEmpty()) { // handle incoming messages first
				BTMessage message = incomingQueue.poll();
				handleMessage(message);
			}
			if (isServer) {
				serverTick();
			} else {
				clientTick();
			}
		} else {
			serverTick();
		}
	}

	/**
	 * Does the cliental progress. Just handling own shape. Incoming Messages did the rest
	 * 
	 * */
	private void clientTick() {
		if (!gameOver.get()) {
			handleTouchEvents();
			updateMyShape();
		}
	}

	/**
	 * Controls the server-side progress of the GameState First Handles own Shape, then updates it, then - if
	 * multiplayerMode - handles other's Shapes
	 * 
	 * */
	private void serverTick() {
		checkDockingAndDock(myShape, myId);
		updateMyShape(); // put my shape one down
		if (isMultiplayer) {
			Set<String> s = new HashSet<String>(otherShapes.keySet());
			for (String id : s) {
				Shape otherShape = otherShapes.get(id);
				checkDockingAndDock(otherShape, id);
			}
		}
	}

	/**
	 * Moves shape one position down
	 * 
	 * @param shape
	 *            Shape to be set down
	 * */
	private void updateMyShape() {
		if (ticks % (GameThread.FPS / myShape.getSpeed()) == 0) {
			ticks = 0; // that "int" doesn't explode
			if (!isShapeColliding(myShape, Motion.DOWN)) {
				myShape.update();
			}
		}
	}

	/**
	 * Analyzes the movements of the Shape. Checks if it has to be put into the Wall and puts it in if necessary
	 * 
	 * @param shape
	 *            Shape that has to be analyzed
	 * @param id
	 *            id of the player the Shape belongs to
	 * */
	private void checkDockingAndDock(Shape shape, String id) {
		boolean docked = isDocked(shape);
		scoreChanged = false;
		if (!gameOver.get()) {
			if (shape.equals(myShape)) {
				handleTouchEvents(); // handle registered moves (touched)
			}
			if (docked) {
				boolean shapeSuccesfullyPlaced = wall.putShapeIntoWall(shape);
				checkGameOver(shapeSuccesfullyPlaced);
				if (shape.equals(myShape)) {
					shape.randomize(calculateInitialXPos(playerNo, playerCount), 0);
				} else {
					// TODO hat 4x wall schicken problem geloest, ergibt allerdings concurrentmodificationexception
					// otherShapes.remove(id);
				}
				int deletedRows = wall.checkRows();
				scoreChanged = score.calculateScore(deletedRows);
				if (scoreChanged) {
					notifyyy(id);
				}
				wall.onDockingCompleted(id);
			}
		} else {
			notifyyy(id); // notify gameService for GameOver
		}
	}

	/**
	 * Checks if actual shape is docked either on the bottom of the wall or on another "shape". In MultiplayerMode this
	 * Method is only used by the Server!
	 * 
	 * @param shape
	 *            ShapeView that needs to be checked
	 * */
	private boolean isDocked(Shape shape) {
		if (wall.isShapeDocked(shape) || shape.getBottomPosY() >= ROWS - 1) {
			return true;
		}
		return false;
	}

	/**
	 * Sets the gameOver flag to true if he current shape just could not be put on the wall In MultiplayerMode this
	 * Method is only used by the Server!
	 * 
	 * @param shapeSuccesfullyPlaced
	 *            true if shape was successfully placed, else false
	 */
	private void checkGameOver(boolean shapeSuccesfullyPlaced) {
		if (!shapeSuccesfullyPlaced) {
			gameOver.set(true);
		}
	}

	/**
	 * runs the changed moves (ehem. move())
	 * */
	private void handleTouchEvents() {
		if (leftTouched.get()) {
			leftTouched.set(false);
			moveLeft();
		}

		if (rightTouched.get()) {
			rightTouched.set(false);
			moveRight();
		}

		if (rotateTouched.get()) {
			rotateTouched.set(false);
			rotate();
		}

		if (speedUpTouched.get()) {
			speedUpTouched.set(false);
			speedUp();
		}

		if (slowDownTouched.get()) {
			slowDownTouched.set(false);
			slowDown();
		}
	}

	/**
	 * Executes the message that has been stored in incomingQueue
	 * 
	 * @param message
	 *            IncomingData that has to be executed
	 * */
	private void handleMessage(BTMessage message) {
		String pId = message.getpID();
		int mId = message.getmID();
		Object data = message.getData();

		switch (mId) {
		case BTMessage.SHAPE:
			otherShapes.put(pId, (Shape) data);
			break;
		case BTMessage.WALL:
			if (!isServer) {
				if (pId.equals(myId)) { // I am Client and my Shape has docked
					myShape.randomize(calculateInitialXPos(playerNo, playerCount), 0);
				}
				this.wall = (Wall) data;
			}
			break;
		case BTMessage.GAME_OVER:
			gameOver.set(true);
			break;
		case BTMessage.POINTS:
			score.setPoints((Integer) data);
			break;
		case BTMessage.PAUSE_GAME:
			paused = true;
			break;
		case BTMessage.RESUME_GAME:
			paused = false;
			break;
		default:
			throw new RuntimeException("handle komische eigehende nachricht: " + "");
		}
	}

	/**
	 * registers that the user has touched moveLeft for the next Tick
	 * */
	public void registerMoveLeft() {
		leftTouched.set(true);
	}

	/**
	 * registers that the user has touched moveRight for the next Tick
	 * */
	public void registerMoveRight() {
		rightTouched.set(true);
	}

	/**
	 * registers that the user has touched rotate for the next Tick
	 * */
	public void registerRotate() {
		rotateTouched.set(true);
	}

	/**
	 * registers that the user has touched speed up for the next Tick
	 * */
	public void registerSpeedUp() {
		speedUpTouched.set(true);
	}

	/**
	 * registers that the user has touched slow down for the next Tick
	 * */
	public void registerSlowDown() {
		slowDownTouched.set(true);
	}

	/**
	 * Takes an incoming Data and puts it into the incomingQueue
	 * 
	 * @param id
	 *            id of the player the Data came from
	 * @param data
	 *            the data
	 * */
	public void setIncomingData(BTMessage message) {
		try {
			incomingQueue.put(message);
		} catch (InterruptedException e) {
			throw new RuntimeException("Etwas mit der Incoming Queue ist schief gelaufen!", e);
		}
	}

	/**
	 * Moves the Shape one position to the left
	 * 
	 * @return true if moved, else false
	 * */
	private boolean moveLeft() {
		if (!isShapeColliding(myShape, Motion.LEFT)) {
			myShape.moveLeft();
			return true;
		}
		return false;
	}

	/**
	 * Moves the Shape one position to the right
	 * 
	 * @return true if moved, else false
	 * */
	private boolean moveRight() {
		if (!isShapeColliding(myShape, Motion.RIGHT)) {
			myShape.moveRight();
			return true;
		}
		return false;
	}

	/**
	 * Rotates the Shape one state clockwise if no collision
	 * 
	 * @return true if rotated, else false
	 * */
	private boolean rotate() {
		if (!isShapeColliding(myShape, Motion.ROTATE)) {
			myShape.rotate();
		}
		return true;
	}

	/**
	 * Sets speed of the player's Shape to default speed
	 * */
	private boolean speedUp() {
		myShape.setSpeed(Speed.FAST.getValue());
		return true;
	}

	/**
	 * Sets speed of the player's Shape to fast speed
	 * */
	private boolean slowDown() {
		myShape.setSpeed(Speed.SLOW.getValue());
		return true;
	}

	/**
	 * Checks if the Shape is colliding after either with the Wall or another Shape
	 * 
	 * @param myShape
	 *            Shape that has to be checked
	 * @param motion
	 *            Sort of Motion that wants to be done
	 * */
	private boolean isShapeColliding(Shape myShape, Motion motion) {
		if (wall.collidesWith(myShape, motion)) { // check collision with wall
			return true;
		}
		if (isMultiplayer) { // check if there's a collision with other Shapes
			for (Shape otherShape : otherShapes.values()) {
				if (!myShape.equals(otherShape)) {
					if (myShape.collidesWith(otherShape, motion)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Getter for gameOver State
	 * 
	 * @return true if gameOver, false else
	 * */
	public boolean getGameOver() {
		return gameOver.get();
	}

	/**
	 * Getter for wall (Called in the TetrisView)
	 * 
	 * @return Wall Object
	 */
	public Wall getWall() {
		return wall;
	}

	/**
	 * Getter for myShape
	 * 
	 * @return myShape
	 * */
	public Shape getMyShape() {
		return myShape;
	}

	/**
	 * Getter for all Shapes
	 * 
	 * @return List of Shapes
	 */
	public Collection<Shape> getOtherShapes() {
		return otherShapes.values();
	}

	/**
	 * Setter for myId
	 * 
	 * @param id
	 */
	public void setMyId(String id) {
		this.myId = id;
	}

	/**
	 * Setter for isServer
	 * 
	 * @param isServer
	 */
	public void setServer(boolean isServer) {
		this.isServer = isServer;
	}

	public boolean isMultiplayerMode() {
		return isMultiplayer;
	}

	/**
	 * Setter for Multiplayer Mode
	 * 
	 * @param isMultiplayer
	 */
	public void setMultiplayerMode(boolean isMultiplayer) {
		this.isMultiplayer = isMultiplayer;
	}

	/**
	 * Getter for paused status
	 * 
	 * @return paused
	 */
	public boolean isPaused() {
		return paused;
	}

	/**
	 * Setter for paused status
	 * 
	 * @param paused
	 *            - flag if the current game is paused or not
	 * @param notify
	 *            - flag if other devices should be sotified
	 */
	public void setPaused(boolean paused, boolean notify) {
		boolean oldPaused = this.paused;
		this.paused = paused;
		if (oldPaused != paused && notify) {
			notifyyy(myId);
		}
	}

	/**
	 * Setter for scoreChanged
	 * 
	 * @param scoreChanged
	 */
	public void setScoreChanged(boolean scoreChanged) {
		this.scoreChanged = scoreChanged;
	}

	/**
	 * Setter for scoreChanged
	 * 
	 * @return scoreChanged
	 */
	public boolean isScoreChanged() {
		return scoreChanged;
	}

	/**
	 * Getter for points
	 * 
	 * @return points
	 */
	public int getPoints() {
		return score.getPoints();
	}

	/**
	 * Notifies its Observers when GameOver State has Changed
	 * 
	 * @param pId
	 *            ID of the player who has released the gameover
	 * */
	private void notifyyy(String pId) {
		setChanged();
		notifyObservers(pId);
	}
}
