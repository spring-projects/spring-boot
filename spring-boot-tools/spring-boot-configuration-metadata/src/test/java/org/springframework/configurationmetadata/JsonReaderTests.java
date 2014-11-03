/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.configurationmetadata;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link JsonReader}
 *
 * @author Stephane Nicoll
 */
public class JsonReaderTests extends AbstractConfigurationMetadataTests {

	private final JsonReader reader = new JsonReader();

	@Test
	public void emptyMetadata() throws IOException {
		RawConfigurationMetadata rawMetadata = reader.read(getInputStreamFor("empty"));
		assertEquals(0, rawMetadata.getSources().size());
		assertEquals(0, rawMetadata.getItems().size());
	}

	@Test
	public void invalidMetadata() throws IOException {
		thrown.expect(JSONException.class);
		reader.read(getInputStreamFor("invalid"));
	}

	@Test
	public void simpleMetadata() throws IOException {
		RawConfigurationMetadata rawMetadata = reader.read(getInputStreamFor("foo"));
		List<ConfigurationMetadataSource> sources = rawMetadata.getSources();
		assertEquals(2, sources.size());
		List<ConfigurationMetadataItem> items = rawMetadata.getItems();
		assertEquals(4, items.size());


		ConfigurationMetadataSource source = sources.get(0);
		assertSource(source, "spring.foo", "org.acme.Foo", "org.acme.config.FooApp");
		assertEquals("foo()", source.getSourceMethod());
		assertEquals("This is Foo.", source.getDescription());
		ConfigurationMetadataItem item = items.get(0);
		assertProperty(item, "spring.foo.name", "name", String.class, null);
		assertItem(item, "org.acme.Foo");
		ConfigurationMetadataItem item2 = items.get(1);
		assertProperty(item2, "spring.foo.description", "description", String.class, "FooBar");
		assertEquals("Foo description.", item2.getDescription());
		assertNull(item2.getSourceMethod());
		assertItem(item2, "org.acme.Foo");
	}

	@Test
	public void rootMetadata() throws IOException {
		RawConfigurationMetadata rawMetadata = reader.read(getInputStreamFor("root"));
		List<ConfigurationMetadataSource> sources = rawMetadata.getSources();
		assertEquals(0, sources.size());
		List<ConfigurationMetadataItem> items = rawMetadata.getItems();
		assertEquals(2, items.size());

		ConfigurationMetadataItem item = items.get(0);
		assertProperty(item, "spring.root.name", "spring.root.name", String.class, null);

	}

}
