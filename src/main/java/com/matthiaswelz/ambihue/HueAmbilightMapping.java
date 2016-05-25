package com.matthiaswelz.ambihue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.matthiaswelz.ambihue.AmbilightData.Position;
import com.matthiaswelz.ambihue.HueController.LightState;

public class HueAmbilightMapping {
	static Logger logger = LogManager.getLogger();
	
	static Pattern pattern = Pattern.compile("^(?<lightName>[^:]+):(?<position>[A-Za-z]+)(?<index>[0-9])(:(?<brightness>[0-9]{0,3}))?$");
	
	public static HueAmbilightMapping parse(String mapping) {
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
		
		return new HueAmbilightMapping(lightName, Position.valueOf(position), Integer.parseInt(index), Integer.parseInt(brightness));
	}
	
	private String lightName;
	private Position position;
	private int index;
	private int brightness;
	private LightState savedState;
	
	public HueAmbilightMapping(String lightName, Position position, int index, int brightness) {
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
