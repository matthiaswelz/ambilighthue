package com.matthiaswelz.ambihue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.matthiaswelz.ambihue.AmbilightData.Position;

public class SimpleHueAmbilightMapping extends HueAmbilightMapping {
	static Logger logger = LogManager.getLogger();
	
	static Pattern pattern = Pattern.compile("^(?<lightName>[^:]+):(?<position>[A-Za-z]+)(?<index>[0-9])(:(?<brightness>[0-9]{0,3}))?$");
	
	public static boolean isFit(String mapping) {
		return pattern.matcher(mapping).matches();
	}
	public static SimpleHueAmbilightMapping parse(String mapping) {
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
		
		return new SimpleHueAmbilightMapping(lightName, Position.valueOf(position), Integer.parseInt(index), Integer.parseInt(brightness));
	}
	
	private Position position;
	private int index;
	
	public SimpleHueAmbilightMapping(String lightName, Position position, int index, int brightness) {
		super(lightName, brightness);
		
		this.position = position;
		this.index = index;
	}
	
	protected Color calculateColor(AmbilightData data) {	
		return data.getColor(this.position, this.index);
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.appendSuper(super.toString())
				.append("position", position)
				.append("index", index)
				.build();
	}
}
