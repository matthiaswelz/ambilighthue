package com.matthiaswelz.ambihue;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.matthiaswelz.ambihue.AmbilightReader.TVOffListenr;
import com.matthiaswelz.ambihue.HueController.HueConnectedCallback;

public class StartProgram {
	static Logger logger = LogManager.getLogger();

	private Timer tvPollTimer;
	private AmbilightReader reader;
	private HueController hueController;
	private Ambihue ambihue;

	private Parameters parameters;
	
	public StartProgram(Parameters parameters) {
		this.parameters = parameters;
	}

	private void scheduleTVCheck() {
		logger.trace("Scheduling TV check");
		
		this.tvPollTimer.schedule(new TimerTask() {	
			@Override
			public void run() {
				try {
					StartProgram.this.checkTV();
				} catch (Exception e) {
					System.err.println(e);
				}
			}
		}, this.parameters.tvCheckIntervalMs);
	}
	
	private void checkTV() throws Exception {
		logger.trace("Checking TV");
		
		if (this.reader.tryReadColors() == null) {
			logger.trace("TV still off");
			
			this.scheduleTVCheck();
			return;
		}
		
		logger.info("TV on - starting Ambihue");
		this.ambihue.start();
	}

	public void run() throws Exception {		
		logger.debug("Creating tvPollTimer");
		this.tvPollTimer = new Timer(true);

		logger.debug("Creating hueController");
		this.hueController = new HueController();

		logger.debug("Creating ambilightReader");
		this.reader = new AmbilightReader(parameters.tvIP, parameters.ambilightTimeoutMS, parameters.tvOffDelay);
		reader.addTVProbablyOffListenr(new TVOffListenr() {
			@Override
			public void tvProbablyOff() {
				logger.debug("Received tvProbablyOff notification");
				if (!ambihue.isRunning())
					return;
				
				logger.info("TV off - stopping Ambihue");
				try {
					ambihue.stop();
				} catch (Exception e) {
					logger.catching(e);
				}

				logger.info("Waiting for TV to be turned on again");
				scheduleTVCheck();
			}
		});

		logger.debug("Creating Ambihue");
		this.ambihue = new Ambihue(reader, hueController, this.parameters.interval);

		logger.debug("Parsing mappings");
		for (String mapping : parameters.mappings) {
			logger.debug("Parsing mapping " + mapping);
			
			HueAmbilightMapping association = HueAmbilightMapping.parse(mapping);
			this.ambihue.addAssociation(association);
			
			logger.debug("Mapping added: " + association);
		}

		logger.debug("Connecting to hue");
		hueController.connect(parameters.hueIP, parameters.hueUser, new HueConnectedCallback() {
			@Override
			public void needsPushButton() {
				logger.error("Needs PushButton - please reconnect!");
			}			
			@Override
			public void hueConnected(String ip, String username) {
				logger.info("Connected to Bridge " + ip);

				logger.info("Waiting for TV to be turned on");
				scheduleTVCheck();
			}
		});
		
		logger.debug("Entering infinite wait loop");
		Object obj = new Object();
		synchronized (obj) {
			obj.wait();
		}
	}
}
