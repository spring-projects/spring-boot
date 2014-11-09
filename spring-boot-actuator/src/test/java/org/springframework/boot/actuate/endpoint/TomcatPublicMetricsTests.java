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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.MBeanInfo;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.metrics.Metric;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TomcatPublicMetrics}
 *
 * @author Johannes Stelzer
 */
public class TomcatPublicMetricsTests {

	@Before
	public void setup() throws Exception {
		DynamicMBean mbean = mock(DynamicMBean.class);
		AttributeList attributeList = new AttributeList(Arrays.asList(new Attribute(
				TomcatPublicMetrics.MAX_ACTIVE_SESSIONS, -1), new Attribute(
				TomcatPublicMetrics.ACTIVE_SESSIONS, 5)));

		when(mbean.getMBeanInfo()).thenReturn(
				new MBeanInfo("mock", "", null, null, null, null));
		when(mbean.getAttributes(TomcatPublicMetrics.ALL_ATTRIBUTE_NAMES)).thenReturn(
				attributeList);

		ManagementFactory.getPlatformMBeanServer().registerMBean(mbean,
				new ObjectName("Tomcat:context=/,host=localhost,type=Manager"));
	}

	@Test
	public void testTomcatMetrics() throws Exception {
		TomcatPublicMetrics publicMetrics = new TomcatPublicMetrics();

		Map<String, Metric<?>> results = new HashMap<String, Metric<?>>();

		for (Metric<?> metric : publicMetrics.metrics()) {
			results.put(metric.getName(), metric);
		}

		assertEquals(-1, results.get("sessions.max").getValue());
		assertEquals(5, results.get("sessions.active").getValue());
	}
}
