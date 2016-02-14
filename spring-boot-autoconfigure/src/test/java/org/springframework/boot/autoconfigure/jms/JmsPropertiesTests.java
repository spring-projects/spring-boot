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

package org.springframework.boot.autoconfigure.jms;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link JmsProperties}.
 *
 * @author Stephane Nicoll
 */
public class JmsPropertiesTests {

	@Test
	public void formatConcurrencyNull() {
		JmsProperties properties = new JmsProperties();
		assertNull(properties.getListener().formatConcurrency());
	}

	@Test
	public void formatConcurrencyOnlyLowerBound() {
		JmsProperties properties = new JmsProperties();
		properties.getListener().setConcurrency(2);
		assertEquals("2", properties.getListener().formatConcurrency());
	}

	@Test
	public void formatConcurrencyOnlyHigherBound() {
		JmsProperties properties = new JmsProperties();
		properties.getListener().setMaxConcurrency(5);
		assertEquals("1-5", properties.getListener().formatConcurrency());
	}

	@Test
	public void formatConcurrencyBothBounds() {
		JmsProperties properties = new JmsProperties();
		properties.getListener().setConcurrency(2);
		properties.getListener().setMaxConcurrency(10);
		assertEquals("2-10", properties.getListener().formatConcurrency());
	}

}
