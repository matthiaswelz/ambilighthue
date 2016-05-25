package com.matthiaswelz.ambihue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.matthiaswelz.ambihue.HueController.HueConnectedCallback;

public class SearchBridgeProgram {	
	static Logger logger = LogManager.getLogger();
	
	private final Object lockObject = new Object();
	
	public SearchBridgeProgram(Parameters parameters) {
		
	}

	public void run() throws Exception {
		logger.debug("Creating HueController");		
		
		try (HueController hueController = new HueController()) {
			logger.debug("Acquiring Lock");
			synchronized (lockObject) {
				logger.debug("Connecting");
				hueController.connect(new HueConnectedCallback() {
					@Override
					public void needsPushButton() {
						System.out.println("Push button now!");
					}
					
					@Override
					public void hueConnected(String ip, String username) {
						System.out.println("Connected to hue:");
						System.out.println("  IP: " + ip);
						System.out.println("  Username: " + username);
						
						synchronized (lockObject) {
							lockObject.notify();
						}
					}
				});

				logger.debug("Waiting");
				lockObject.wait();
				logger.debug("Done");
			}
		}
	}
}
