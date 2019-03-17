/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.configurationprocessor.metadata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonMarshaller}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class JsonMarshallerTests {

	@Test
	public void marshallAndUnmarshal() throws Exception {
		ConfigurationMetadata metadata = new ConfigurationMetadata();
		metadata.add(ItemMetadata.newProperty("a", "b", StringBuffer.class.getName(),
				InputStream.class.getName(), "sourceMethod", "desc", "x",
				new ItemDeprecation("Deprecation comment", "b.c.d")));
		metadata.add(ItemMetadata.newProperty("b.c.d", null, null, null, null, null, null,
				null));
		metadata.add(
				ItemMetadata.newProperty("c", null, null, null, null, null, 123, null));
		metadata.add(
				ItemMetadata.newProperty("d", null, null, null, null, null, true, null));
		metadata.add(ItemMetadata.newProperty("e", null, null, null, null, null,
				new String[] { "y", "n" }, null));
		metadata.add(ItemMetadata.newProperty("f", null, null, null, null, null,
				new Boolean[] { true, false }, null));
		metadata.add(ItemMetadata.newGroup("d", null, null, null));
		metadata.add(ItemHint.newHint("a.b"));
		metadata.add(ItemHint.newHint("c", new ItemHint.ValueHint(123, "hey"),
				new ItemHint.ValueHint(456, null)));
		metadata.add(new ItemHint("d", null,
				Arrays.asList(
						new ItemHint.ValueProvider("first",
								Collections.singletonMap("target", "foo")),
						new ItemHint.ValueProvider("second", null))));
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		JsonMarshaller marshaller = new JsonMarshaller();
		marshaller.write(metadata, outputStream);
		ConfigurationMetadata read = marshaller
				.read(new ByteArrayInputStream(outputStream.toByteArray()));
		assertThat(read).has(Metadata.withProperty("a.b", StringBuffer.class)
				.fromSource(InputStream.class).withDescription("desc")
				.withDefaultValue("x").withDeprecation("Deprecation comment", "b.c.d"));
		assertThat(read).has(Metadata.withProperty("b.c.d"));
		assertThat(read).has(Metadata.withProperty("c").withDefaultValue(123));
		assertThat(read).has(Metadata.withProperty("d").withDefaultValue(true));
		assertThat(read).has(
				Metadata.withProperty("e").withDefaultValue(new String[] { "y", "n" }));
		assertThat(read).has(Metadata.withProperty("f")
				.withDefaultValue(new Object[] { true, false }));
		assertThat(read).has(Metadata.withGroup("d"));
		assertThat(read).has(Metadata.withHint("a.b"));
		assertThat(read).has(
				Metadata.withHint("c").withValue(0, 123, "hey").withValue(1, 456, null));
		assertThat(read).has(Metadata.withHint("d").withProvider("first", "target", "foo")
				.withProvider("second"));
	}

	@Test
	public void marshallOrderItems() throws IOException {
		ConfigurationMetadata metadata = new ConfigurationMetadata();
		metadata.add(ItemHint.newHint("fff"));
		metadata.add(ItemHint.newHint("eee"));
		metadata.add(ItemMetadata.newProperty("com.example.bravo", "bbb", null, null,
				null, null, null, null));
		metadata.add(ItemMetadata.newProperty("com.example.bravo", "aaa", null, null,
				null, null, null, null));
		metadata.add(ItemMetadata.newProperty("com.example.alpha", "ddd", null, null,
				null, null, null, null));
		metadata.add(ItemMetadata.newProperty("com.example.alpha", "ccc", null, null,
				null, null, null, null));
		metadata.add(ItemMetadata.newGroup("com.acme.bravo",
				"com.example.AnotherTestProperties", null, null));
		metadata.add(ItemMetadata.newGroup("com.acme.alpha", "com.example.TestProperties",
				null, null));
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		JsonMarshaller marshaller = new JsonMarshaller();
		marshaller.write(metadata, outputStream);
		String json = new String(outputStream.toByteArray());
		assertThat(json).containsSubsequence("\"groups\"", "\"com.acme.alpha\"",
				"\"com.acme.bravo\"", "\"properties\"", "\"com.example.alpha.ccc\"",
				"\"com.example.alpha.ddd\"", "\"com.example.bravo.aaa\"",
				"\"com.example.bravo.bbb\"", "\"hints\"", "\"eee\"", "\"fff\"");
	}

	@Test
	public void marshallPutDeprecatedItemsAtTheEnd() throws IOException {
		ConfigurationMetadata metadata = new ConfigurationMetadata();
		metadata.add(ItemMetadata.newProperty("com.example.bravo", "bbb", null, null,
				null, null, null, null));
		metadata.add(ItemMetadata.newProperty("com.example.bravo", "aaa", null, null,
				null, null, null, new ItemDeprecation(null, null, "warning")));
		metadata.add(ItemMetadata.newProperty("com.example.alpha", "ddd", null, null,
				null, null, null, null));
		metadata.add(ItemMetadata.newProperty("com.example.alpha", "ccc", null, null,
				null, null, null, new ItemDeprecation(null, null, "warning")));
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		JsonMarshaller marshaller = new JsonMarshaller();
		marshaller.write(metadata, outputStream);
		String json = new String(outputStream.toByteArray());
		assertThat(json).containsSubsequence("\"properties\"",
				"\"com.example.alpha.ddd\"", "\"com.example.bravo.bbb\"",
				"\"com.example.alpha.ccc\"", "\"com.example.bravo.aaa\"");
	}

}
