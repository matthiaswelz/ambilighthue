package com.matthiaswelz.ambihue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

public class Parameters {
	@Option(name="-tvIP",usage="Sets the IP address of the TV")
	public String tvIP;
	@Option(name="-hueIP",usage="Sets the IP address of the Hue bridge")
	public String hueIP;
	@Option(name="-hueUser",usage="Sets the username for the hue bridge")
	public String hueUser;
	
	@Option(name="-ambilightTimeout", depends={"-start"},usage="Sets the timeout (in ms) for reading ambilight data. Note: Failure results in retry, so keep this value small")
	public int ambilightTimeoutMS = 100;
	@Option(name="-tvOffDelay", depends={"-start"},usage="Sets the time (in ms) after which the system should assume that the TV has been turned off")
	public int tvOffDelay = 3000;
	@Option(name = "-map", depends={"-start"}, handler=StringArrayOptionHandler.class, usage="Mappings between ambilight positions and light names. Syntax: [Light name]:[Ambilight Position][Ambilight Index]:[Brightness] for simple or [Light name]:[Ambilights]:[Brightness] where Ambilights multiple of (separated by comma): [Weight][Ambilight Position][Ambilight Index]")
	public List<String> mappings = new ArrayList<String>();
	@Option(name="-tvCheckInterval", depends={"-start"},usage="Sets the interval (in ms) to check the TV after it has been turned off")
	public int tvCheckIntervalMs = 5000;
	@Option(name="-interval", depends={"-start"}, usage="Sets the interval (in ms) to read from the Ambilight")
	public int interval = 250;
	
	@Option(name = "-connect", forbids = {"-tvIP", "-hueIP", "-hueUser", "-ambilightTimeout", "-tvOffDelay", "-map", "-start", "tvCheckInterval", "-interval"}, usage="Searches for a bridge")
	public boolean connect;
	@Option(name = "-start", forbids = {"-connect"}, depends={"-tvIP", "-hueIP", "-hueUser"}, usage="Searches for a bridge")
	public boolean start;
	@Option(name = "-ambilightInfo", forbids = {"-connect", "-hueIP", "-hueUser", "-map", "start", "tvCheckInterval", "-interval"}, depends={"-tvIP"}, usage="Displays information about the installed Ambilight")
	public boolean ambilightInfo;
	
	@Option(name = "-logLevel", usage = "Log Level")
	public String logLevel = "INFO";
	
	
	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("tvIP", tvIP)
				.append("hueIP", hueIP)
				.append("hueUser", hueUser)
				.append("ambilightTimeout", ambilightTimeoutMS)
				.append("tvOffDelay", tvOffDelay)
				.append("tvCheckInterval", tvCheckIntervalMs)
				.append("interval", interval)
				.append("map", mappings)
				.append("connect", connect)
				.append("start", start)
				.append("ambilightInfo", ambilightInfo)
				.append("logLevel", logLevel)
				.build();
	}
}