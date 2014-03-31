/*
 * Copyright 2010-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.data.gemfire;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import sample.data.gemfire.domain.Gemstone;
import sample.data.gemfire.service.GemstoneService;
import sample.data.gemfire.service.GemstoneServiceImpl.IllegalGemstoneException;

/**
 * The SampleDataGemFireApplicationTest class is a test suite with test cases testing the SampleDataGemFireApplication
 * in Spring Boot.
 * <p/>
 * @author John Blum
 * @see org.junit.Test
 * @see org.junit.runner.RunWith
 * @see org.springframework.boot.test.SpringApplicationConfiguration
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @see sample.data.gemfire.SampleDataGemFireApplication
 * @since 1.0.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleDataGemFireApplication.class)
@SuppressWarnings("unused")
public class SampleDataGemFireApplicationTest {

	@Autowired
	private GemstoneService gemstoneService;

	private final AtomicLong ID_GENERATOR = new AtomicLong(0l);

	protected List<Gemstone> asList(final Iterable<Gemstone> gemstones) {
		List<Gemstone> gemstoneList = new ArrayList<Gemstone>();

		if (gemstones != null) {
			for (Gemstone gemstone : gemstones) {
				gemstoneList.add(gemstone);
			}
		}

		return gemstoneList;
	}

	protected Gemstone createGemstone(final String name) {
		return createGemstone(ID_GENERATOR.incrementAndGet(), name);
	}

	protected Gemstone createGemstone(final Long id, final String name) {
		return new Gemstone(id, name);
	}

	protected List<Gemstone> getGemstones(final String... names) {
		List<Gemstone> gemstones = new ArrayList<Gemstone>(names.length);

		for (String name : names) {
			gemstones.add(createGemstone(null, name));
		}

		return gemstones;
	}

	@Before
	public void setup() {
		assertNotNull("A reference to the GemstoneService was not properly configured!", gemstoneService);
	}

	@Test
	public void testGemstonesApp() {
		assertEquals(0, gemstoneService.count());
		assertTrue(asList(gemstoneService.list()).isEmpty());

		gemstoneService.save(createGemstone("Diamond"));
		gemstoneService.save(createGemstone("Ruby"));

		assertEquals(2, gemstoneService.count());
		assertTrue(asList(gemstoneService.list()).containsAll(getGemstones("Diamond", "Ruby")));

		try {
			gemstoneService.save(createGemstone("Coal"));
		}
		catch (IllegalGemstoneException expected) {
		}

		assertEquals(2, gemstoneService.count());
		assertTrue(asList(gemstoneService.list()).containsAll(getGemstones("Diamond", "Ruby")));

		gemstoneService.save(createGemstone("Pearl"));
		gemstoneService.save(createGemstone("Sapphire"));

		assertEquals(4, gemstoneService.count());
		assertTrue(asList(gemstoneService.list()).containsAll(getGemstones("Diamond", "Ruby", "Pearl", "Sapphire")));

		try {
			gemstoneService.save(createGemstone("Quartz"));
		}
		catch (IllegalGemstoneException expected) {
		}

		assertEquals(4, gemstoneService.count());
		assertTrue(asList(gemstoneService.list()).containsAll(getGemstones("Diamond", "Ruby", "Pearl", "Sapphire")));
		assertEquals(createGemstone("Diamond"), gemstoneService.get("Diamond"));
		assertEquals(createGemstone("Pearl"), gemstoneService.get("Pearl"));
	}

}
