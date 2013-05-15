package de.medieninf.mobcomp.multris.game.enums;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

/**
 * @author tina schedlbauer
 * 
 *         This enum contains all the ShapeType logic. It is very mighty!
 * */
public enum ShapeType implements Serializable {
	O_SHAPE(1), T_SHAPE(2), I_SHAPE(3), J_SHAPE(4), L_SHAPE(5), S_SHAPE(6), Z_SHAPE(7);

	private final int id;
	private ArrayList<Boolean[][]> models;

	/**
	 * Constructor that initializes the models of the ShapeType
	 * 
	 * @param id
	 *            ID of the ShapeType (used for bitmaps later)
	 */
	ShapeType(int id) {
		this.id = id;
		this.models = new ArrayList<Boolean[][]>();
		initModels();
	}

	/**
	 * Initializes the different models of the ShapeType and adds them to the models-List. This method is called in the
	 * constructor.
	 * */
	private void initModels() {
		switch (id) {
		case 1:
			models.add(new Boolean[][] { { true, true }, { true, true } });
			break;
		case 2:
			models.add(new Boolean[][] { { true, false }, { true, true }, { true, false } });
			models.add(new Boolean[][] { { true, true, true }, { false, true, false } });
			models.add(new Boolean[][] { { false, true }, { true, true }, { false, true } });
			models.add(new Boolean[][] { { false, true, false }, { true, true, true } });
			break;
		case 3:
			models.add(new Boolean[][] { { true, true, true, true } });
			models.add(new Boolean[][] { { true }, { true }, { true }, { true } });
			break;
		case 4:
			models.add(new Boolean[][] { { true, true, true }, { false, false, true } });
			models.add(new Boolean[][] { { false, true }, { false, true }, { true, true } });
			models.add(new Boolean[][] { { true, false, false }, { true, true, true } });
			models.add(new Boolean[][] { { true, true }, { true, false }, { true, false } });
			break;
		case 5:
			models.add(new Boolean[][] { { true, true, true }, { true, false, false } });
			models.add(new Boolean[][] { { true, true }, { false, true }, { false, true } });
			models.add(new Boolean[][] { { false, false, true }, { true, true, true } });
			models.add(new Boolean[][] { { true, false }, { true, false }, { true, true } });
			break;
		case 6:
			models.add(new Boolean[][] { { false, true, true }, { true, true, false } });
			models.add(new Boolean[][] { { true, false }, { true, true }, { false, true } });
			break;
		case 7:
			models.add(new Boolean[][] { { true, true, false }, { false, true, true } });
			models.add(new Boolean[][] { { false, true }, { true, true }, { true, false } });
			break;
		}
	}

	/**
	 * Getter for id
	 * 
	 * @return id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Generates a random ShapeType
	 * 
	 * @return ShapeType with value of random no
	 */
	public static ShapeType getRandom() {
		Random r = new Random();
		int i = r.nextInt(ShapeType.values().length);
		return ShapeType.values()[i];
	}

	/**
	 * Getter for the ShapeType model with a specific rotatio state
	 * 
	 * @param rotation
	 *            model state that is wanted
	 * @return model of the ShapeType according to rotation
	 */
	public Boolean[][] getModel(int rotation) {
		return models.get(rotation % models.size());
	}

}
