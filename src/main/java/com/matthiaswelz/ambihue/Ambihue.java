package com.matthiaswelz.ambihue;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.matthiaswelz.ambihue.AmbilightData.Position;
import com.matthiaswelz.ambihue.HueController.LightState;

public class Ambihue {
	static Logger logger = LogManager.getLogger();
	
	public static class Association {
		static Logger logger = LogManager.getLogger();
		
		static Pattern pattern = Pattern.compile("^(?<lightName>[^:]+):(?<position>[A-Za-z]+)(?<index>[0-9])(:(?<brightness>[0-9]{0,3}))?$");
		
		public static Association parse(String mapping) {
			logger.trace("Matching: " + mapping);
			
			Matcher matcher = pattern.matcher(mapping);
			if (!matcher.matches()) {
				logger.trace("No match");
				return null;
			}

			logger.trace("Matched");
			
			String lightName = matcher.group("lightName");
			logger.trace("lightName: " + lightName);
			
			String position = matcher.group("position");
			logger.trace("position: " + position);
			
			String index = matcher.group("index");
			logger.trace("index: " + index);
			
			String brightness = matcher.group("brightness");
			logger.trace("brightness: " + brightness);
			if (brightness == null) {
				brightness = "100";
				logger.trace("Defaulting to brightness: " + brightness);
			}
			
			return new Association(lightName, Position.valueOf(position), Integer.parseInt(index), Integer.parseInt(brightness));
		}
		
		private String lightName;
		private Position position;
		private int index;
		private int brightness;
		private LightState savedState;
		
		public Association(String lightName, Position position, int index, int brightness) {
			this.lightName = lightName;
			this.position = position;
			this.index = index;
			this.brightness = brightness;
		}
		
		protected void prepareLight(HueController hueController) {
			assert this.savedState == null;
			
			logger.debug("Preparing light " + this.lightName);
			this.savedState = hueController.saveLightState(this.lightName);
			hueController.setBrightness(this.lightName, this.brightness);
		}
		protected void unprepareLight(HueController hueController) {
			assert this.savedState != null;

			logger.debug("Unpreparing light " + this.lightName);
			hueController.restoreLightState(this.savedState);
			this.savedState = null;
		}
		protected void apply(HueController hueController, AmbilightData data) {	
			logger.trace("Applying new color value to " + this.lightName);
			
			Color color = data.getColor(this.position, this.index);
			hueController.setColor(lightName, color);
		}
		
		@Override
		public String toString() {
			return new ToStringBuilder(this)
					.append("lightName", lightName)
					.append("position", position)
					.append("index", index)
					.append("brightness", brightness)
					.build();
		}
	}
	
	private final Timer ambilightReadTimer;
	private final AmbilightReader reader;
	private final HueController hueController;
	private final Thread hueSetThread;
	private final List<Association> associations;
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
		this.associations = new ArrayList<Association>();
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
			
			for (Association association : this.associations) {
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
			for (Association association : this.associations) {
				association.unprepareLight(hueController);
			}
			logger.debug("Lights resetted");
			
			this.semaphore.release();
		}
	}
	
	public boolean isRunning() {
		return this.running.get();
	}

	public void addAssociation(Association association) {
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
			for (Association association : associations)
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
