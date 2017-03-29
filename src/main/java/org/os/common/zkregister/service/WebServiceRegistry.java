package org.os.common.zkregister.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.Endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务为WebService类型
 * @author chenwei 20170301
 *
 */
public class WebServiceRegistry {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(WebServiceRegistry.class);
	
	private WebServiceRegistry(){
	}
	
	public static String publishService(Object serviceObject, String host, int port) throws Exception {
		String url = String.format("http://%s:%d/%s?wsdl", host, port, serviceObject.getClass().getCanonicalName());
		Endpoint publish = Endpoint.publish(url, serviceObject);
		if (publish.isPublished()) {
			LOGGER.debug(String.format("服务 “%s”布成功", url));
			return url;
		}
		LOGGER.debug(String.format("服务 “%s”布失败", url));
		return null;
	}
	
	/**
	 * 访问服务，获取“targetNamespace”和“name”map
	 * @param url
	 * @return
	 */
	private static Map<String, String> getTargetNamespaceAndName(String url) {
		Map<String, String> m = new HashMap<>();
		URL u;
		HttpURLConnection openConnection = null;
		try {
			u = new URL(url);
			openConnection = (HttpURLConnection) u.openConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try (InputStream inputStream = openConnection.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));) {
			String readLine = br.readLine();
			while (readLine != null) {
				if (readLine.contains("targetNamespace")) {
					java.util.stream.Stream.of(readLine.split(" "))
					.filter(attr -> attr.contains("targetNamespace") | attr.contains("name"))
					.map(attr -> attr.replaceAll("\"", ""))
					.map(attr -> attr.replaceAll("<", ""))
					.map(attr -> attr.replaceAll(">", ""))
					.map(attr -> attr.split("="))
					.forEach(attr -> m.put(attr[0], attr[1]));
					break;
				}
				readLine = br.readLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return m;
	}
}
