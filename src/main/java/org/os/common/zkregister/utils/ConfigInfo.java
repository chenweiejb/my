package org.os.common.zkregister.utils;

import java.util.ResourceBundle;

/**
 * 获取配置信息
 * @author chenwei
 *
 */
public class ConfigInfo {
	private static ResourceBundle config = ResourceBundle.getBundle("config");
	
	public static String getConfig(String key) {
		return config.getString(key);
	}
}
