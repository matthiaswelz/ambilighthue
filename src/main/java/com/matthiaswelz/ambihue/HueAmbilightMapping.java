package com.matthiaswelz.ambihue;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.matthiaswelz.ambihue.HueController.LightState;

public abstract class HueAmbilightMapping {
	static Logger logger = LogManager.getLogger();

	//TODO: Change to factory pattern if needed
	public static HueAmbilightMapping parse(String mapping) {
		if (SimpleHueAmbilightMapping.isFit(mapping))
			return SimpleHueAmbilightMapping.parse(mapping);
		
		assert AveragedAmbilightMapping.isFit(mapping);
		return AveragedAmbilightMapping.parse(mapping);
	}

	private String lightName;
	private int brightness;
	
	private LightState savedState;
	
	protected HueAmbilightMapping(String lightName, int brightness) {
		assert brightness > 0;
		assert lightName != null;
		
		this.lightName = lightName;
		this.brightness = brightness;
	}

	public void prepareLight(HueController hueController) {
		assert hueController != null;
		assert this.savedState == null;
		
		logger.debug("Preparing light " + this.lightName);
		this.savedState = hueController.saveLightState(this.lightName);
		hueController.setBrightness(this.lightName, this.brightness);
	}

	public void unprepareLight(HueController hueController) {
		assert hueController != null;
		assert this.savedState != null;
	
		logger.debug("Unpreparing light " + this.lightName);
		hueController.restoreLightState(this.savedState);
		this.savedState = null;
	}

	public void apply(HueController hueController, AmbilightData data) {
		assert hueController != null;
		assert data != null;
		assert this.savedState != null;
		
		logger.trace("Calculating new color value for " + this.lightName);
		
		Color color = this.calculateColor(data);
		assert color != null;
		hueController.setColor(lightName, color);

		logger.trace("Applied color: " + color);
	}
	
	protected abstract Color calculateColor(AmbilightData data);
	
	@Override
	public String toString() {
		return new ToStringBuilder(this)
			.append("lightName", lightName)
			.append("brightness", brightness)
			.toString();
	}
}