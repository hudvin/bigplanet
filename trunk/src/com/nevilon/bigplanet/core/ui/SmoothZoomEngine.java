package com.nevilon.bigplanet.core.ui;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import com.nevilon.bigplanet.core.AbstractCommand;
import com.nevilon.bigplanet.core.PhysicMap;

public class SmoothZoomEngine {

	private static SmoothZoomEngine sze;
	
	private LinkedList<Integer> scaleQueue = new LinkedList<Integer>();

	private Float scaleFactor = 1000f;

	private AbstractCommand updateScreen;

	private AbstractCommand reloadMap;

	public static SmoothZoomEngine getInstance() {
		
			
		
		if (sze == null) {
			sze = new SmoothZoomEngine();
		}
		return sze;
	}

	public void setUpdateScreenCommand(final AbstractCommand updateScreen) {
		this.updateScreen = updateScreen;
	}

	public void setReloadMapCommand(final AbstractCommand reloadMap) {
		this.reloadMap = reloadMap;
	}

	public void nullScaleFactor() {
		synchronized (scaleFactor) {
			scaleFactor = 1000f;
		}
	}

	private SmoothZoomEngine() {
		System.out.println("create queue");
		createQueue();
	}

	private void createQueue() {
		new Thread() {

			@Override
			public void run() {
				boolean isEmpty = true;
				if (updateScreen != null) {
					updateScreen.execute();

				}
				double endScaleFactor;
				int PAUSE = 15;
				while (true) {
					if (scaleQueue.size() > 0) {
						isEmpty = false;
						int scaleDirection = scaleQueue.removeFirst();
						endScaleFactor = scaleDirection == -1 ? scaleFactor / 2
								: scaleFactor * 2;
						int z = PhysicMap.getZoomLevel();
						if ((scaleDirection == -1 && z < 16)
								|| (scaleDirection == 1 && z > 0)) {
							if (!(endScaleFactor > 8000 || endScaleFactor < 125)) {
								System.out.println("smooth scaling");
								synchronized (sze) {
									// synchronized (scaleFactor) {
									do {
										try {
											Thread.sleep(PAUSE);
											scaleFactor = scaleFactor
													+ (scaleDirection) * 25;
											// обновить экран

											updateScreen.execute(new Float(
													scaleFactor / 1000));
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
									} while (!(scaleFactor == (endScaleFactor)));
								}
							}
							// }

						}
						

							if (!isEmpty && scaleQueue.size() == 0) {
								System.out.println("reload");
								isEmpty = true;
								try {
									Thread.sleep(100);
									reloadMap.execute(new Float(
											scaleFactor / 1000));
									//semaphore.release();
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							
						}

					}
					// }

				}
			}

		}.start();
	}

	public void addToScaleQ(int direction) {
		synchronized (scaleQueue) {
			System.out.println("add to scale " + direction);
			scaleQueue.addLast(direction);
		}
	}

}
