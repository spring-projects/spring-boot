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

package sample.data.gemfire;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.junit.runner.RunWith;
import sample.data.gemfire.domain.Gemstone;
import sample.data.gemfire.service.GemstoneService;
import sample.data.gemfire.service.IllegalGemstoneException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleDataGemFireApplication}.
 *
 * @author John Blum
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SampleDataGemFireApplicationTests {

	@Autowired
	private GemstoneService service;

	private final AtomicLong idGenerator = new AtomicLong(0L);

	@Test
	public void gemstonesAppServiceEndpoints() {
		assertThat(this.service.count()).isEqualTo(0);
		assertThat(this.service.list()).isEmpty();
		this.service.save(createGemstone("Diamond"));
		this.service.save(createGemstone("Ruby"));
		assertThat(this.service.count()).isEqualTo(2);
		assertThat(this.service.list()).contains(getGemstones("Diamond", "Ruby"));
		try {
			this.service.save(createGemstone("Coal"));
		}
		catch (IllegalGemstoneException ignore) {
			// expected
		}
		assertThat(this.service.count()).isEqualTo(2);
		assertThat(this.service.list()).contains(getGemstones("Diamond", "Ruby"));
		this.service.save(createGemstone("Pearl"));
		this.service.save(createGemstone("Sapphire"));
		assertThat(this.service.count()).isEqualTo(4);
		assertThat(this.service.list())
				.contains(getGemstones("Diamond", "Ruby", "Pearl", "Sapphire"));
		try {
			this.service.save(createGemstone("Quartz"));
		}
		catch (IllegalGemstoneException ignore) {
			// expected
		}
		assertThat(this.service.count()).isEqualTo(4);
		assertThat(this.service.list())
				.contains(getGemstones("Diamond", "Ruby", "Pearl", "Sapphire"));
		assertThat(this.service.get("Diamond")).isEqualTo(createGemstone("Diamond"));
		assertThat(this.service.get("Pearl")).isEqualTo(createGemstone("Pearl"));
	}

	private Gemstone[] getGemstones(String... names) {
		Gemstone[] gemstones = new Gemstone[names.length];
		for (int i = 0; i < names.length; i++) {
			gemstones[i] = createGemstone(null, names[i]);
		}
		return gemstones;
	}

	private Gemstone createGemstone(String name) {
		return createGemstone(this.idGenerator.incrementAndGet(), name);
	}

	private Gemstone createGemstone(Long id, String name) {
		return new Gemstone(id, name);
	}

}
