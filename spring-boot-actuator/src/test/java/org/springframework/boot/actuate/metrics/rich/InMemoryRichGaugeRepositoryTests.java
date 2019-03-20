/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.boot.actuate.metrics.writer.Delta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Tests for {@link InMemoryRichGaugeRepository}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class InMemoryRichGaugeRepositoryTests {

	private final InMemoryRichGaugeRepository repository = new InMemoryRichGaugeRepository();

	@Test
	public void writeAndRead() {
		this.repository.set(new Metric<Double>("foo", 1d));
		this.repository.set(new Metric<Double>("foo", 2d));
		assertThat(this.repository.findOne("foo").getCount()).isEqualTo(2L);
		assertThat(this.repository.findOne("foo").getValue()).isEqualTo(2d, offset(0.01));
	}

	@Test
	public void incrementExisting() {
		this.repository.set(new Metric<Double>("foo", 1d));
		this.repository.increment(new Delta<Double>("foo", 2d));
		assertThat(this.repository.findOne("foo").getCount()).isEqualTo(2L);
		assertThat(this.repository.findOne("foo").getValue()).isEqualTo(3d, offset(0.01));
	}

	@Test
	public void incrementNew() {
		this.repository.increment(new Delta<Double>("foo", 2d));
		assertThat(this.repository.findOne("foo").getCount()).isEqualTo(1L);
		assertThat(this.repository.findOne("foo").getValue()).isEqualTo(2d, offset(0.01));
	}

}
