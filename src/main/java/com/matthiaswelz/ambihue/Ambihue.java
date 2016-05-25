package com.matthiaswelz.ambihue;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Ambihue {
	static Logger logger = LogManager.getLogger();
	
	private final Timer ambilightReadTimer;
	private final AmbilightReader reader;
	private final HueController hueController;
	private final Thread hueSetThread;
	private final List<HueAmbilightMapping> associations;
	private final int intervalMs;

	private final AtomicReference<AmbilightData> data;
	private final Semaphore semaphore;
	private final AtomicBoolean running;
	
	public Ambihue(AmbilightReader ambilightReader, HueController hueController, int intervalMs) {
		logger.trace("Ambihue constructor");
		
		assert ambilightReader != null;
		assert hueController != null;
		assert intervalMs > 0;
		
		this.hueController = hueController;
		this.intervalMs = intervalMs;
		this.reader = ambilightReader;
		
		this.semaphore = new Semaphore(0);
		this.data = new AtomicReference<AmbilightData>(null);
		this.associations = new ArrayList<HueAmbilightMapping>();
		this.running = new AtomicBoolean(false);
		
		this.ambilightReadTimer = new Timer(true);
		
		this.hueSetThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Ambihue.this.refreshHue();
				} catch (Exception e) {
					System.err.println(e);
				}
			}
		});
		this.hueSetThread.setDaemon(true);
		
		logger.trace("Starting thread");
		this.hueSetThread.start();
	}
	
	public void start() {		
		logger.debug("Start request");
		
		if (this.running.compareAndSet(false, true)) {
			logger.info("Starting");
			
			for (HueAmbilightMapping association : this.associations) {
				association.prepareLight(hueController);
			}
			
			this.scheduleRefresh();
		}
	}
	private void scheduleRefresh() {
		logger.trace("Scheduling refresh");
		
		assert this.running.get();
		
		this.ambilightReadTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					Ambihue.this.readAmbilight();
				} catch (Exception e) {
					logger.error("Error reading ambilight value", e);
				}
			}
		}, intervalMs);
	}

	public void stop() throws Exception {
		logger.debug("Stop request");
		
		if (this.running.compareAndSet(true, false)) {
			logger.info("Stopping");
			
			logger.debug("Waiting to reset lights");
			//Wait until one refresh-Loop has completed
			this.semaphore.acquire();

			logger.debug("Resetting lights");
			for (HueAmbilightMapping association : this.associations) {
				association.unprepareLight(hueController);
			}
			logger.debug("Lights resetted");
			
			this.semaphore.release();
		}
	}
	
	public boolean isRunning() {
		return this.running.get();
	}

	public void addAssociation(HueAmbilightMapping association) {
		logger.debug("Adding association " + association);
		
		assert !this.running.get();
		
		this.associations.add(association);
	}
	
	private void refreshHue() throws Exception {
		logger.debug("refresh thread running");
		
		while (true) { 
			logger.trace("RefreshHue Loop - begin");
			
			//Allow stopping at this point
			this.semaphore.release();
			logger.trace("Semaphore released");
			this.semaphore.acquire(2);
			logger.trace("Semaphore acquired");
			
			AmbilightData data = this.data.getAndSet(null);
			logger.trace("Data read " + data);
			if (data == null)
				continue;

			if (!this.running.get())
				continue;
			
			logger.trace("Setting colors " + data);
			for (HueAmbilightMapping association : associations)
				association.apply(hueController, data);
		}
	}

	private void readAmbilight() throws Exception {
		logger.trace("readAmbilight");
		
		if (!this.running.get())
			return;
		
		AmbilightData data = this.reader.tryReadColors();
		logger.trace("Data read");
		
		if (data != null) {
			logger.trace("Data not null");
			
			this.data.set(data);
			this.semaphore.release();
		} else {
			logger.debug("Read null value");
		}
		
		if (this.running.get())
			this.scheduleRefresh();
	}
}
