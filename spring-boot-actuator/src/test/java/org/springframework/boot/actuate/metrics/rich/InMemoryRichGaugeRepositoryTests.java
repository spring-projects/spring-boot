/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.actuate.metrics.rich;

import org.junit.Test;

import org.springframework.boot.actuate.metrics.Metric;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 */
public class InMemoryRichGaugeRepositoryTests {

	private final InMemoryRichGaugeRepository repository = new InMemoryRichGaugeRepository();

	@Test
	public void writeAndRead() {
		this.repository.set(new Metric<Double>("foo", 1d));
		this.repository.set(new Metric<Double>("foo", 2d));
		assertEquals(2L, this.repository.findOne("foo").getCount());
		assertEquals(2d, this.repository.findOne("foo").getValue(), 0.01);
	}

}
