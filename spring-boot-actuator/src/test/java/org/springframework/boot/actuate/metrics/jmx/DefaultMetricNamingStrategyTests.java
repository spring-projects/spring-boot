/*
 * Copyright 2012-2016 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(name.getDomain()).isEqualTo("domain");
		assertThat(name.getKeyProperty("type")).isEqualTo("foo");
	}

	@Test
	public void onePeriod() throws Exception {
		ObjectName name = this.strategy.getObjectName(null,
				"domain:type=MetricValue,name=foo.bar");
		assertThat(name.getDomain()).isEqualTo("domain");
		assertThat(name.getKeyProperty("type")).isEqualTo("foo");
		assertThat(name.getKeyProperty("value")).isEqualTo("bar");
	}

	@Test
	public void twoPeriods() throws Exception {
		ObjectName name = this.strategy.getObjectName(null,
				"domain:type=MetricValue,name=foo.bar.spam");
		assertThat(name.getDomain()).isEqualTo("domain");
		assertThat(name.getKeyProperty("type")).isEqualTo("foo");
		assertThat(name.getKeyProperty("name")).isEqualTo("bar");
		assertThat(name.getKeyProperty("value")).isEqualTo("spam");
	}

	@Test
	public void threePeriods() throws Exception {
		ObjectName name = this.strategy.getObjectName(null,
				"domain:type=MetricValue,name=foo.bar.spam.bucket");
		assertThat(name.getDomain()).isEqualTo("domain");
		assertThat(name.getKeyProperty("type")).isEqualTo("foo");
		assertThat(name.getKeyProperty("name")).isEqualTo("bar");
		assertThat(name.getKeyProperty("value")).isEqualTo("spam.bucket");
	}

}
