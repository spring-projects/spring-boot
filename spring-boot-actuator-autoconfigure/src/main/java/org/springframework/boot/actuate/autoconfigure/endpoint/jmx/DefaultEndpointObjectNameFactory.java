/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.endpoint.jmx;

import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.boot.actuate.endpoint.jmx.EndpointMBean;
import org.springframework.boot.actuate.endpoint.jmx.EndpointObjectNameFactory;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link EndpointObjectNameFactory} that generates standard {@link ObjectName} for
 * Actuator's endpoints.
 *
 * @author Stephane Nicoll
 */
class DefaultEndpointObjectNameFactory implements EndpointObjectNameFactory {

	private final JmxEndpointExporterProperties properties;

	private final MBeanServer mBeanServer;

	private final String contextId;

	DefaultEndpointObjectNameFactory(JmxEndpointExporterProperties properties,
			MBeanServer mBeanServer, String contextId) {
		this.properties = properties;
		this.mBeanServer = mBeanServer;
		this.contextId = contextId;
	}

	@Override
	public ObjectName generate(EndpointMBean mBean) throws MalformedObjectNameException {
		String baseObjectName = this.properties.getDomain() + ":type=Endpoint" + ",name="
				+ StringUtils.capitalize(mBean.getEndpointId());
		StringBuilder builder = new StringBuilder(baseObjectName);
		if (this.mBeanServer != null && hasMBean(baseObjectName)) {
			builder.append(",context=").append(this.contextId);
		}
		if (this.properties.isUniqueNames()) {
			builder.append(",identity=").append(ObjectUtils.getIdentityHexString(mBean));
		}
		builder.append(getStaticNames());
		return ObjectNameManager.getInstance(builder.toString());
	}

	private boolean hasMBean(String baseObjectName) throws MalformedObjectNameException {
		ObjectName query = new ObjectName(baseObjectName + ",*");
		return this.mBeanServer.queryNames(query, null).size() > 0;
	}

	private String getStaticNames() {
		if (this.properties.getStaticNames().isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<Object, Object> name : this.properties.getStaticNames()
				.entrySet()) {
			builder.append(",").append(name.getKey()).append("=").append(name.getValue());
		}
		return builder.toString();
	}

}
