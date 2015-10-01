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

package org.springframework.boot.actuate.metrics.atsd;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;


/**
 * Tests for {@link DefaultAtsdNamingStrategy}.
 *
 * @author Alexander Tokarev.
 */
public class DefaultAtsdNamingStrategyTests {
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	private DefaultAtsdNamingStrategy namingStrategy;

	@Before
	public void setUp() throws Exception {
		this.namingStrategy = new DefaultAtsdNamingStrategy();
	}

	@Test
	public void testGetName() throws Exception {
		this.namingStrategy.setEntity("my_entity");
		this.namingStrategy.setMetricPrefix("prefix_");
		Map<String, String> tags = new HashMap<String, String>();
		tags.put("tag_name_1", "tagValue1");
		tags.put("tag_name_2", "tagValue2");
		this.namingStrategy.setTags(tags);
		this.namingStrategy.afterPropertiesSet();

		AtsdName name = this.namingStrategy.getName("test");
		AtsdName expected = new AtsdName("prefix_test", "my_entity", tags);
		assertEquals(expected, name);
	}

	@Test
	public void testGetNameDefault() throws Exception {
		this.namingStrategy.afterPropertiesSet();
		AtsdName name = this.namingStrategy.getName("test");
		AtsdName expected = new AtsdName("test", DefaultAtsdNamingStrategy.DEFAULT_ENTITY, null);
		assertEquals(expected, name);
	}

	@Test
	public void testGetNameCache() throws Exception {
		this.namingStrategy.setEntity("my_entity");
		this.namingStrategy.setMetricPrefix("prefix_");
		this.namingStrategy.setTags(null);
		this.namingStrategy.afterPropertiesSet();
		AtsdName nameTestOne = this.namingStrategy.getName("test1");
		AtsdName nameTestTwo = this.namingStrategy.getName("test2");
		assertSame(nameTestOne, this.namingStrategy.getName("test1"));
		assertSame(nameTestTwo, this.namingStrategy.getName("test2"));
	}

	@Test
	public void testAfterPropertiesSetEmptyEntity() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Entity is required");
		this.namingStrategy.setEntity("");
		this.namingStrategy.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSetNullEntity() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Entity is required");
		this.namingStrategy.setEntity(null);
		this.namingStrategy.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSetEmptyTagName() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Empty tag key");
		this.namingStrategy.setTags(Collections.singletonMap("", "test"));
		this.namingStrategy.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSetEmptyTagValue() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Empty tag value");
		this.namingStrategy.setTags(Collections.singletonMap("test", ""));
		this.namingStrategy.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSetNullTagName() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Empty tag key");
		this.namingStrategy.setTags(Collections.singletonMap((String) null, "test"));
		this.namingStrategy.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSetNullTagValue() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Empty tag value");
		this.namingStrategy.setTags(Collections.singletonMap("test", (String) null));
		this.namingStrategy.afterPropertiesSet();
	}
}
