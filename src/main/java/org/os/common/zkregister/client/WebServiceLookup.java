package org.os.common.zkregister.client;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.os.common.zkregister.ServiceLookup;

public class WebServiceLookup extends ServiceLookup {

	private String wsTargetNamespace;
	private String wsServiceName;

	/**
	 * 构造器
	 * @param servicePath zk目录（例：/webservice/order……）
	 * @param wsTargetNamespace webservice 命名空间
	 * @param wsServiceName webservice 服务名称
	 */
	public WebServiceLookup(String servicePath, String wsTargetNamespace, String wsServiceName) {
		super(servicePath);
		this.wsTargetNamespace = wsTargetNamespace;
		this.wsServiceName = wsServiceName;
	}

	@Override
	protected <T> T lookupService(String url, Class<T> clazz) {
		T remote = null;
		URL urlConn;
		try {
			urlConn = new URL(url);
			QName qname = new QName(wsTargetNamespace, wsServiceName);
			Service service = Service.create(urlConn, qname);
			remote = (T) service.getPort(clazz);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return remote;
	}

}
