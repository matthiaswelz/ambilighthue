package com.matthiaswelz.ambihue;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.kohsuke.args4j.CmdLineParser;

public class Program {	
	public static void main(String[] args) throws Exception {
		Parameters parameters = new Parameters();
		CmdLineParser parser = new CmdLineParser(parameters);
		try {
			parser.parseArgument(args);
			
			Level logLevel = Level.getLevel(parameters.logLevel);
			Configurator.setRootLevel(logLevel);			
		} catch (Exception ex) {
			parser.printUsage(System.out);
			return;
		}

		Logger logger = LogManager.getLogger();
		logger.trace("Start");
		
		logger.debug("Parameters: " + parameters);
		
		if (parameters.connect) {
			logger.debug("Running SearchBridgeProgram");
			
			new SearchBridgeProgram(parameters).run();
		} else if (parameters.start) {
			logger.debug("Running StartProgram");
			
			new StartProgram(parameters).run();
		} else if (parameters.ambilightInfo) {
			logger.debug("Running InfoProgram");
			
			new AmbilightInfoProgram(parameters).run();
		} else {
			parser.printUsage(System.out);
		}
		
		logger.debug("End");
	}
}
