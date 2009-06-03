package com.nevilon.moow.core;

import java.util.Stack;

import android.graphics.Point;

public class InertionEngine {

	private long interval;
	
	private Point a;
	
	private Point b;
	
	public double x;
	
	public double y;

	public double dx;
	
	public double dy;
	
	// величины ускорения
	public double ax;
	
	public double ay;
	
	private Stack<Point> moveHistory;
	
	
	public InertionEngine(Stack<Point> moveHistory, long interval){
		this.interval = interval;
		this.moveHistory = moveHistory;
		findAB();
		
		dx = b.x - a.x;
		dy = b.y - a.y;
		ax = (dx/interval);
		ay = (dy/interval);
	}
	
	
	public double getInterval(){
		return this.interval;
	}
	
	private void findAB(){
		Point tmpPoint = new Point();
		for (Point pp : moveHistory){
			if(!pp.equals(tmpPoint) && tmpPoint.x!=0 && tmpPoint.y!=0){
				a = tmpPoint;
				b = pp;
				break;
			} else {
				tmpPoint = pp;
			}
		}
	}
	
	
}
