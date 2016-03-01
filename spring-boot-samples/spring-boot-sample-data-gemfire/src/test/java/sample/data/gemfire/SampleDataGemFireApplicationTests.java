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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import sample.data.gemfire.domain.Gemstone;
import sample.data.gemfire.service.GemstoneService;
import sample.data.gemfire.service.GemstoneServiceImpl.IllegalGemstoneException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The SampleDataGemFireApplicationTests class is a test suite with test cases testing the
 * SampleDataGemFireApplication in Spring Boot.
 *
 * @author John Blum
 */
@RunWith(SpringRunner.class)
@SpringApplicationConfiguration(SampleDataGemFireApplication.class)
public class SampleDataGemFireApplicationTests {

	@Autowired
	private GemstoneService gemstoneService;

	private final AtomicLong ID_GENERATOR = new AtomicLong(0l);

	@Before
	public void setup() {
		assertThat(this.gemstoneService).isNotNull();
	}

	@Test
	public void testGemstonesApp() {
		assertThat(this.gemstoneService.count()).isEqualTo(0);
		assertThat(this.gemstoneService.list()).isEmpty();

		this.gemstoneService.save(createGemstone("Diamond"));
		this.gemstoneService.save(createGemstone("Ruby"));

		assertThat(this.gemstoneService.count()).isEqualTo(2);
		assertThat(this.gemstoneService.list()).contains(getGemstones("Diamond", "Ruby"));

		try {
			this.gemstoneService.save(createGemstone("Coal"));
		}
		catch (IllegalGemstoneException ex) {
			// Expected
		}

		assertThat(this.gemstoneService.count()).isEqualTo(2);
		assertThat(this.gemstoneService.list()).contains(getGemstones("Diamond", "Ruby"));

		this.gemstoneService.save(createGemstone("Pearl"));
		this.gemstoneService.save(createGemstone("Sapphire"));

		assertThat(this.gemstoneService.count()).isEqualTo(4);
		assertThat(this.gemstoneService.list())
				.contains(getGemstones("Diamond", "Ruby", "Pearl", "Sapphire"));

		try {
			this.gemstoneService.save(createGemstone("Quartz"));
		}
		catch (IllegalGemstoneException expected) {
		}

		assertThat(this.gemstoneService.count()).isEqualTo(4);
		assertThat(this.gemstoneService.list())
				.contains(getGemstones("Diamond", "Ruby", "Pearl", "Sapphire"));
		assertThat(this.gemstoneService.get("Diamond"))
				.isEqualTo(createGemstone("Diamond"));
		assertThat(this.gemstoneService.get("Pearl")).isEqualTo(createGemstone("Pearl"));
	}

	private Gemstone[] getGemstones(String... names) {
		Gemstone[] gemstones = new Gemstone[names.length];
		for (int i = 0; i < names.length; i++) {
			gemstones[i] = createGemstone(null, names[i]);
		}
		return gemstones;
	}

	private Gemstone createGemstone(String name) {
		return createGemstone(this.ID_GENERATOR.incrementAndGet(), name);
	}

	private Gemstone createGemstone(Long id, String name) {
		return new Gemstone(id, name);
	}

}
