package de.medieninf.mobcomp.multris.game.enums;

import java.io.Serializable;

public enum Speed implements Serializable{
	FAST(20), SLOW(2);
	
	private float speed;
	
	/**
	 * Private Constructor
	 * 
	 * @param speed - meaning how many steps a Shape moves down per second
	 */
	private Speed(int speed){
		this.speed = speed;
	}
	
	/**
	 * 
	 * @return speed
	 */
	public float getValue(){
		return speed;
	}
}
