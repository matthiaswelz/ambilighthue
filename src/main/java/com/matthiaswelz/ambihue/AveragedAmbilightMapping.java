package com.matthiaswelz.ambihue;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.util.Strings;

import com.matthiaswelz.ambihue.AmbilightData.Position;

public class AveragedAmbilightMapping extends HueAmbilightMapping {
	static class LightPart {
		private Position position;
		private int index;
		private double weight;
		
		public LightPart(Position position, int index, double weight) {
			assert weight > 0;
			
			this.position = position;
			this.index = index;
			this.weight = weight;
		}
	}
	
	static Pattern mainPattern = Pattern.compile("^(?<lightName>[^:]+):(?<lights>([^:,]+,?){2,})(:(?<brightness>[0-9]{0,3}))?$");
	static Pattern partPattern = Pattern.compile("^(?<weight>[0-9]*(\\.[0-9]+)?)?(?<position>[A-Za-z]+)(?<index>[0-9]),?$"); 
	
	public static boolean isFit(String mapping) {
		return mainPattern.matcher(mapping).matches();
	}
	public static AveragedAmbilightMapping parse(String mapping) {
		logger.trace("Matching: " + mapping);
		
		Matcher mainMatcher = mainPattern.matcher(mapping);
		if (!mainMatcher.matches()) {
			logger.trace("No match");
			return null;
		}

		logger.trace("Matched");
		
		String lightName = mainMatcher.group("lightName");
		logger.trace("lightName: " + lightName);
		
		String brightness = mainMatcher.group("brightness");
		logger.trace("brightness: " + brightness);
		if (brightness == null) {
			brightness = "100";
			logger.trace("Defaulting to brightness: " + brightness);
		}
		
		ArrayList<LightPart> lightParts = new ArrayList<>();
		String lights = mainMatcher.group("lights");
		logger.trace("lights: " + lights);
		
		String[] parts = lights.split(",");
		logger.trace("number of parts: " + parts.length);
		
		for (String part : parts) {
			logger.trace("  matching part: " + part);
			Matcher partMatcher = partPattern.matcher(part);
			if (!partMatcher.matches()) {
				logger.trace("    No part match");
				return null;
			}
			
			String position = partMatcher.group("position");
			logger.trace("    position: " + position);
			
			String index = partMatcher.group("index");
			logger.trace("    index: " + index);

			String weight = partMatcher.group("weight");
			logger.trace("    weight: " + weight);
			
			if (Strings.isEmpty(weight))
				weight = "1";
			if (weight.startsWith("."))
				weight = "0" + weight;
			
			LightPart lightPart = new LightPart(Position.valueOf(position), Integer.parseInt(index), Double.parseDouble(weight));
			lightParts.add(lightPart);
			
			logger.trace("    added part");
		}

		
		return new AveragedAmbilightMapping(lightName, Integer.parseInt(brightness), lightParts);
	}

	private final Iterable<LightPart> lightParts;
	
	protected AveragedAmbilightMapping(String lightName, int brightness, Iterable<LightPart> lightParts) {
		super(lightName, brightness);
		
		this.lightParts = lightParts;
	}
	
	@Override
	protected Color calculateColor(AmbilightData data) {
		ColorAverager averager = new ColorAverager();
		
		for (LightPart lightPart : lightParts) {
			Color lightColor = data.getColor(lightPart.position, lightPart.index);
			averager.addColor(lightColor, lightPart.weight);
		}
		
		return averager.calculateResult();
	}
	
}
