package com.matthiaswelz.ambihue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public final class AmbilightReader implements AutoCloseable {
	static Logger logger = LogManager.getLogger();
	
	public static interface TVOffListenr {
		void tvProbablyOff();
	}
	
	private final static int AMBILIGHT_PORT = 1925;
	private final static String AMBILIGHT_PATH = "/1/ambilight/processed";
	
	private final CloseableHttpClient httpClient;
	private final RequestConfig requestConfig;
	private final URI uri;
	private final int tvOffDelayMs;
	private final List<TVOffListenr> tvOffListeners;
	
	private Long firstFailedRequest;
	
	public AmbilightReader(String host, int timeoutInMs, int tvOffDelayMs) throws Exception {	
		logger.trace("AmbilightReader()");	
		
		this.tvOffDelayMs = tvOffDelayMs;
		this.tvOffListeners = new ArrayList<>();
		this.httpClient = HttpClients.createMinimal();
		
		// Timeout - see http://stackoverflow.com/a/29592232
		this.requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(timeoutInMs)
				.setConnectTimeout(timeoutInMs)
				.setSocketTimeout(timeoutInMs)
				.build();
		
		this.uri = new URIBuilder()
				.setScheme("http")
				.setHost(host)
				.setPort(AMBILIGHT_PORT)
				.setPath(AMBILIGHT_PATH)
				.build();
	}
	
	public void addTVProbablyOffListenr(TVOffListenr listener) {
		logger.trace("addTVProbablyOffListenr()");
		
		this.tvOffListeners.add(listener);
	}
	
	public AmbilightData tryReadColors() throws Exception {
		logger.trace("tryReadColors()");
		
		HttpGet request = new HttpGet(this.uri);
		request.setConfig(this.requestConfig);
		
		logger.trace("Executing HTTP-Request");
		try (CloseableHttpResponse response = this.httpClient.execute(request)) {
			logger.trace("Connection created");
			
			String responseJSON = EntityUtils.toString(response.getEntity());
			logger.trace("Response read");
			
			JSONObject json = new JSONObject(responseJSON);
			logger.trace("JSON parsed");
			JSONObject layer1 = json.getJSONObject("layer1");
			logger.trace("layer1 parsed");
			
			AmbilightData result = parseData(layer1);
			logger.trace("AmbilightData read");
			
			firstFailedRequest = null;
			return result;
		} catch (Exception e) {	
			logger.trace("Exception during webrequest", e);
			
			long currentMillis = System.currentTimeMillis();
			if (firstFailedRequest == null) {
				logger.trace("First failed request");
				
				firstFailedRequest = currentMillis;				
			} else {
				logger.trace("Not first failed request");
				
				if (currentMillis - firstFailedRequest.longValue() >= this.tvOffDelayMs) {
					logger.debug("Delay exceeded - sending notification");
					
					for (TVOffListenr listener : this.tvOffListeners)
						listener.tvProbablyOff();
				} 
			}
			
			return null;
		}
	}

	public void close() throws Exception {
		logger.trace("close()");
		
		this.httpClient.close();
	}	

	private AmbilightData parseData(JSONObject data) {
		assert data != null;
		
		Color[] left = parsePosition(data.getJSONObject("left"));
		Color[] top = parsePosition(data.getJSONObject("top"));
		Color[] right = parsePosition(data.getJSONObject("right"));
		Color[] bottom = parsePosition(data.getJSONObject("bottom"));
		
		return new AmbilightData(left, top, right, bottom);
	}
	
	private Color[] parsePosition(JSONObject position) {
		assert position != null;
		
		String[] names = JSONObject.getNames(position);
		if (names == null)
			return new Color[0];
		
		Color[] result = new Color[names.length];
		for (int i = 0; i < result.length; i++) {
			assert position.has(String.valueOf(i));
			
			JSONObject colorJSON = position.getJSONObject(String.valueOf(i));
			result[i] = parseColor(colorJSON);
		}
		
		return result;
	}

	private Color parseColor(JSONObject colorJSON) {
		assert colorJSON != null;
		assert colorJSON.has("r") && colorJSON.has("g") && colorJSON.has("b");
		
		byte r = (byte) colorJSON.getInt("r");
		byte g = (byte) colorJSON.getInt("g");
		byte b = (byte) colorJSON.getInt("b");
		
		return new Color(r, g, b);
	}
}
