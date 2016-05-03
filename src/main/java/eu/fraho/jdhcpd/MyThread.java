package eu.fraho.jdhcpd;

/*
This file is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This file is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this file. If not, see <http://www.gnu.org/licenses/>.
*/

import java.util.Observable;
import java.util.Observer;

/**
 * Abstract class which is an alternative to Thread, providing the ability
 * to notify an observer and an easy way to pause and stop the thread.
 * 
 * @author sfrankenberger
 */
public abstract class MyThread extends Observable implements Runnable {
	protected Thread thread = null;
	private Object lock = null;
	
	private boolean aborted = false;
	
	private boolean paused = false;
	
	public MyThread() {
		lock = new Object();
	}
	
	public MyThread(Observer o) {
		this();
		addObserver(o);
	}
	
	public void abort() {
		aborted = true;
	}
	
	protected boolean doContinueWork() {
		while (paused) {
			try {
				Thread.sleep(50);
			}
			catch (InterruptedException e) {
				setChanged();
				notifyObservers(e);
			}
		}
		
		if (aborted) {
			return false;
		}
		
		return true;
	}
	
	public boolean isAborted() {
		return aborted;
	}
	
	public boolean isPaused() {
		return paused;
	}
	
	public boolean isRunnning() {
		return thread != null && thread.isAlive();
	}
	
	public void pause(boolean p) {
		paused = p;
	}
	
	@Override
	public abstract void run();
	
	public void setName(String n) {
		if (n != null && isRunnning())
			thread.setName(n);
	}
	
	public void start() {
		if (isRunnning()) {
			return;
		}
		
		synchronized (lock) {
			thread = new Thread(this);
			thread.start();
		}
	}
	
	public void waitTillDone() {
		waitTillDone(-1);
	}
	
	public void waitTillDone(int max) {
		int zeit = 0;
		
		if (max == 0)
			zeit = -1;
		
		while (isRunnning() && zeit < max) {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				setChanged();
				notifyObservers(e);
			}
			;
		}
	}
}
