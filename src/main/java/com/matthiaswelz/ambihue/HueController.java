package com.matthiaswelz.ambihue;

import java.net.InetAddress;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.hue.sdk.utilities.PHUtilities;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueParsingError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

public class HueController implements AutoCloseable {
	static Logger logger = LogManager.getLogger();
	
	public interface HueConnectedCallback {
		void needsPushButton();
		void hueConnected(String ip, String username);
	}
	public static class LightState {
		private PHLightState state;
		private String lightName;
		
		protected LightState(HueController controller, String lightName) {
			logger.trace("LightState()");
			
			assert controller != null;
			assert controller.verifyLight(lightName);

			this.lightName = lightName;

			logger.trace("Saving state of light: " + lightName);
			PHLight light = controller.findLight(lightName);
			this.state = new PHLightState(light.getLastKnownLightState());
		}
		
		protected void restore(HueController controller) {
			logger.trace("restore()");
			
			assert controller != null;
			assert this.state != null;

			logger.trace("Restoring state of light: " + lightName);
			PHLight light = controller.findLight(lightName);
			controller.bridge.updateLightState(light, this.state);
			logger.trace("State restored");
		}
	}
	
	private PHHueSDK hue;
	private PHBridge bridge;
	private HueConnectedCallback callback;
	
	public HueController() throws Exception {
		logger.trace("HueController()");
		
		this.hue = PHHueSDK.getInstance();
		logger.trace("HUE-SDK instance received");
		
		this.hue.setAppName("com.matthiaswelz.ambilighthue");
		this.hue.setDeviceName(InetAddress.getLocalHost().getHostName());

		logger.trace("Registering notification manager");
		this.hue.getNotificationManager().registerSDKListener(new PHSDKListener() {
			
			@Override
			public void onParsingErrors(List<PHHueParsingError> arg0) {
				logger.warn("Received hue parsing error");
			}
			
			@Override
			public void onError(int code, String message) {
				logger.debug("Hue error: " + message);
			}
			
			@Override
			public void onConnectionResumed(PHBridge arg0) {
				logger.debug("Hue connection resumed");
			}
			
			@Override
			public void onConnectionLost(PHAccessPoint arg0) {
				logger.warn("Hue connection lost");
			}
			
			@Override
			public void onCacheUpdated(List<Integer> arg0, PHBridge arg1) {
				logger.trace("Hue cache updated");
			}
			
			@Override
			public void onBridgeConnected(PHBridge bridge, String username) {
				logger.debug("Hue bridge connected");
				
				assert callback != null;
				
				HueController.this.bridge = bridge;
				hue.setSelectedBridge(bridge);
				
				logger.trace("Enabling heartbeat");
				hue.enableHeartbeat(bridge, PHHueSDK.HB_INTERVAL);
	            
				logger.trace("Raising notification");
				String ip = bridge.getResourceCache().getBridgeConfiguration().getIpAddress();
				callback.hueConnected(ip, username);
			}
			
			@Override
			public void onAuthenticationRequired(PHAccessPoint accessPoint) {
				logger.debug("Hue authentification required");
				
				assert callback != null;

				hue.startPushlinkAuthentication(accessPoint);
				
				logger.debug("Hue authentification started - push button now!");
				callback.needsPushButton();
			}
			
			@Override
			public void onAccessPointsFound(List<PHAccessPoint> arg0) {
				logger.info("Bridges found: " + arg0.size());
				
				PHAccessPoint accessPoint = arg0.get(0);
				
				logger.debug("Connecting to bridge");
				hue.connect(accessPoint);
			}
		});	    
	}

	public void connect(HueConnectedCallback callback) {
		logger.trace("connect(callback)");
		
		assert this.callback == null;
		this.callback = callback;
		
		logger.info("Starting bridge search");
		PHBridgeSearchManager sm = (PHBridgeSearchManager) hue.getSDKService(PHHueSDK.SEARCH_BRIDGE);
		sm.search(true, true);
		logger.trace("Searching bridge...");
	}
	
	public void connect(final String ip, final String username, HueConnectedCallback callback) {
		logger.trace("connect(ip, username)");
		
		assert this.callback == null;
		this.callback = callback;
		
		PHAccessPoint accessPoint = new PHAccessPoint();
	    accessPoint.setIpAddress(ip);
	    accessPoint.setUsername(username);

		logger.trace("Requesting to connect");
	    hue.connect(accessPoint);
	    logger.trace("Requested to connect");
	}
	
	public LightState saveLightState(String lightName) {
		logger.trace("saveLightState");
		
		assert verifyLight(lightName);
		
		return new LightState(this, lightName);
	}
	public void restoreLightState(LightState lightState) {
		logger.trace("restoreLightState");
		
		lightState.restore(this);
	}
	
	public void setColor(String lightName, Color color) {
		logger.trace("setColor for light: " + lightName);
		
		assert verifyLight(lightName);
		
		PHLight light = this.findLight(lightName);
		PHLightState state = new PHLightState();
		
		logger.trace("Calculating color");
		float[] xy = PHUtilities.calculateXYFromRGB(color.getR() & 0xFF, color.getG() & 0xFF, color.getB() & 0xFF, light.getModelNumber());
		state.setX(xy[0], true);
		state.setY(xy[1], true);

		logger.trace("Updating light to " + xy[0] + " - " +  xy[1]);
		this.bridge.updateLightState(light, state);
	}
	public void setBrightness(String lightName, int brightness) {
		logger.trace("setBrightness for light: " + lightName);
		
		assert verifyLight(lightName);
		
		PHLight light = this.findLight(lightName);
		PHLightState state = new PHLightState();
		state.setOn(true);
		state.setBrightness(brightness, true);

		logger.trace("updating lightstate");
		this.bridge.updateLightState(light, state);
		logger.trace("updated lightstate");
	}
	
	public boolean verifyLight(String lightName) {
		assert lightName != null;
		assert this.callback != null;
		assert this.bridge != null;
		
		PHLight light = this.findLight(lightName);
		return light != null;
	}
	
	@Override
	public void close() throws Exception {
		logger.info("close()");
		
		if (this.bridge != null) {
			hue.disableAllHeartbeat();
			hue.disconnect(bridge);
		}
	}
	
	private PHLight findLight(String name) {
		assert name != null;
		
		for (PHLight light : this.bridge.getResourceCache().getLights().values()) {
			if (name.equals(light.getName()))
				return light;
		}
		
		return null;
	}
}
