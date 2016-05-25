package com.matthiaswelz.ambihue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.matthiaswelz.ambihue.AmbilightData.Position;

public class AmbilightInfoProgram {
	static Logger logger = LogManager.getLogger();

	private final Parameters parameters;
	
	public AmbilightInfoProgram(Parameters parameters) throws Exception {
		this.parameters = parameters;
	
	}
	
	private void printAmbilightPosition(AmbilightData data, Position position) {
		if(!data.hasPosition(position)) {
			logger.trace("Position not available: " + position);
			return;
		}

		logger.trace("Position available: " + position);
		String result = String.valueOf(position);
		result += ": ";
		for (int i = 0; i < data.getDimension(position); i++) {
			if (i > 0)
				result += ", ";
			
			result += String.valueOf(i);
		}
		System.out.println(result);
	}

	public void run() throws Exception {
		try (AmbilightReader reader = new AmbilightReader(parameters.tvIP, parameters.ambilightTimeoutMS, parameters.tvOffDelay)) {
			AmbilightData data = reader.tryReadColors();
			if (data == null) {
				logger.debug("Received null output");
				System.out.println("Could not read Ambilight data - maybe the TV is off?");				
				return;
			}
			
			System.out.println("Available Ambilight positions:");
			printAmbilightPosition(data, Position.Left);
			printAmbilightPosition(data, Position.Right);
			printAmbilightPosition(data, Position.Top);
			printAmbilightPosition(data, Position.Bottom);
		}
	}
}
