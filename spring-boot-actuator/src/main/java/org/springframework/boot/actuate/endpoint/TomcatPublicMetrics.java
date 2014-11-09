/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.JMException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.actuate.metrics.Metric;

/**
 * @author Johannes Stelzer
 * @since 1.2
 */
public class TomcatPublicMetrics implements PublicMetrics {
	private static final Log LOGGER = LogFactory.getLog(TomcatPublicMetrics.class);
	static final String ACTIVE_SESSIONS = "activeSessions";
	static final String MAX_ACTIVE_SESSIONS = "maxActiveSessions";
	static final String[] ALL_ATTRIBUTE_NAMES = new String[] { MAX_ACTIVE_SESSIONS,
			ACTIVE_SESSIONS };

	@Override
	public Collection<Metric<?>> metrics() {
		Collection<Metric<?>> metrics = new ArrayList<Metric<?>>(2);

		try {
			AttributeList attributes = ManagementFactory
					.getPlatformMBeanServer()
					.getAttributes(
							new ObjectName("Tomcat:context=/,host=localhost,type=Manager"),
							ALL_ATTRIBUTE_NAMES);
			for (Attribute a : attributes.asList()) {
				if (MAX_ACTIVE_SESSIONS.equals(a.getName())) {
					metrics.add(new Metric<Integer>("sessions.max", (Integer) a
							.getValue()));
				}
				else if (ACTIVE_SESSIONS.equals(a.getName())) {
					metrics.add(new Metric<Integer>("sessions.active", (Integer) a
							.getValue()));
				}
			}
		}
		catch (JMException ex) {
			LOGGER.warn("Error collecting Tomcat metrics", ex);
		}

		return metrics;
	}

}
