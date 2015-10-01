/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.metrics.jmx;

import javax.management.ObjectName;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link DefaultMetricNamingStrategy}.
 *
 * @author Dave Syer
 */
public class DefaultMetricNamingStrategyTests {

	private DefaultMetricNamingStrategy strategy = new DefaultMetricNamingStrategy();

	@Test
	public void simpleName() throws Exception {
		ObjectName name = this.strategy.getObjectName(null,
				"domain:type=MetricValue,name=foo");
		assertEquals("domain", name.getDomain());
		assertEquals("foo", name.getKeyProperty("type"));
	}

	@Test
	public void onePeriod() throws Exception {
		ObjectName name = this.strategy.getObjectName(null,
				"domain:type=MetricValue,name=foo.bar");
		assertEquals("domain", name.getDomain());
		assertEquals("foo", name.getKeyProperty("type"));
		assertEquals("Wrong name: " + name, "bar", name.getKeyProperty("value"));
	}

	@Test
	public void twoPeriods() throws Exception {
		ObjectName name = this.strategy.getObjectName(null,
				"domain:type=MetricValue,name=foo.bar.spam");
		assertEquals("domain", name.getDomain());
		assertEquals("foo", name.getKeyProperty("type"));
		assertEquals("Wrong name: " + name, "bar", name.getKeyProperty("name"));
		assertEquals("Wrong name: " + name, "spam", name.getKeyProperty("value"));
	}

	@Test
	public void threePeriods() throws Exception {
		ObjectName name = this.strategy.getObjectName(null,
				"domain:type=MetricValue,name=foo.bar.spam.bucket");
		assertEquals("domain", name.getDomain());
		assertEquals("foo", name.getKeyProperty("type"));
		assertEquals("Wrong name: " + name, "bar", name.getKeyProperty("name"));
		assertEquals("Wrong name: " + name, "spam.bucket", name.getKeyProperty("value"));
	}

}
