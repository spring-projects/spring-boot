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
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link DefaultAtsdDataEncoder}.
 *
 * @author Alexander Tokarev.
 */
public class DefaultAtsdDataEncoderTests {
	private DefaultAtsdDataEncoder encoder = new DefaultAtsdDataEncoder();

	@Test
	public void testEncode() throws Exception {
		AtsdName name = new AtsdName("metric", "entity", null);
		AtsdData value = new AtsdData(name, 10);
		String encoded = this.encoder.encode(value);
		String expected = "series e:entity m:metric=10\n";
		assertEquals(expected, encoded);

		Map<String, String> tags = new LinkedHashMap<String, String>();
		tags.put("tag name", "tag value");
		tags.put("test", "test");
		name = new AtsdName("test\"metric", "test'entity", tags);
		value = new AtsdData(name, 20.5, 1000L);
		encoded = this.encoder.encode(value);
		expected = "series e:test_entity ms:1000 t:tag_name=\"tag value\" t:test=test m:test_metric=20.5\n";
		assertEquals(expected, encoded);
	}

	@Test
	public void testEncodeGrouping() throws Exception {
		Map<String, String> tags = Collections.singletonMap("name", "value");
		AtsdData valueOne = new AtsdData(new AtsdName("metric1", "entity", tags), 1);
		AtsdData valueTwo = new AtsdData(new AtsdName("metric2", "entity", tags), 2);
		AtsdData valueThree = new AtsdData(new AtsdName("metric3", "entity", tags), 3, 1000L);
		AtsdData valueFor = new AtsdData(new AtsdName("metric4", "entity", tags), 4, 1000L);
		AtsdData valueFive = new AtsdData(new AtsdName("metric5", "entity", tags), 5, 2000L);
		AtsdData valueSix = new AtsdData(new AtsdName("metric6", "entity", null), 6, 2000L);
		String encoded = this.encoder.encode(valueOne, valueTwo, valueThree, valueFor, valueFive, valueSix);
		String expected = "series e:entity t:name=value m:metric1=1 m:metric2=2\n" +
				"series e:entity ms:1000 t:name=value m:metric3=3 m:metric4=4\n" +
				"series e:entity ms:2000 t:name=value m:metric5=5\n" +
				"series e:entity ms:2000 m:metric6=6\n";
		assertEquals(expected, encoded);
	}

	@Test
	public void testClean() throws Exception {
		assertEquals("valid", this.encoder.clean("valid"));
		assertEquals("test_test_test_", this.encoder.clean("test test\"test'"));
		assertEquals("test_test", this.encoder.clean("test \"'test"));
	}

	@Test
	public void testQuoteIfNeeded() throws Exception {
		assertEquals("test", this.encoder.quoteIfNeeded("test"));
		assertEquals("\"test\"test\"", this.encoder.quoteIfNeeded("test\"test"));
		assertEquals("\"test test\"", this.encoder.quoteIfNeeded("test test"));
		assertEquals("\"test=test\"", this.encoder.quoteIfNeeded("test=test"));
	}
}
