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

package org.springframework.boot.configurationprocessor.metadata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.boot.configurationprocessor.ConfigurationMetadataMatchers.containsGroup;
import static org.springframework.boot.configurationprocessor.ConfigurationMetadataMatchers.containsHint;
import static org.springframework.boot.configurationprocessor.ConfigurationMetadataMatchers.containsProperty;

/**
 * Tests for {@link JsonMarshaller}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class JsonMarshallerTests {

	@Test
	public void marshallAndUnmarshal() throws IOException {
		ConfigurationMetadata metadata = new ConfigurationMetadata();
		metadata.add(ItemMetadata.newProperty("a", "b", StringBuffer.class.getName(),
				InputStream.class.getName(), "sourceMethod", "desc", "x", true));
		metadata.add(ItemMetadata.newProperty("b.c.d", null, null, null, null, null,
				null, false));
		metadata.add(ItemMetadata.newProperty("c", null, null, null, null, null, 123,
				false));
		metadata.add(ItemMetadata.newProperty("d", null, null, null, null, null, true,
				false));
		metadata.add(ItemMetadata.newProperty("e", null, null, null, null, null,
				new String[] { "y", "n" }, false));
		metadata.add(ItemMetadata.newProperty("f", null, null, null, null, null,
				new Boolean[] { true, false }, false));
		metadata.add(ItemMetadata.newGroup("d", null, null, null));
		metadata.add(ItemHint.newHint("a.b"));
		metadata.add(ItemHint.newHint("c", new ItemHint.ValueHint(123, "hey"),
				new ItemHint.ValueHint(456, null)));
		metadata.add(new ItemHint("d", null, Arrays.asList(new ItemHint.ProviderHint(
				"first", Collections.<String, Object> singletonMap("target", "foo")),
				new ItemHint.ProviderHint("second", null))));
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		JsonMarshaller marshaller = new JsonMarshaller();
		marshaller.write(metadata, outputStream);
		ConfigurationMetadata read = marshaller.read(new ByteArrayInputStream(
				outputStream.toByteArray()));
		assertThat(read,
				containsProperty("a.b", StringBuffer.class).fromSource(InputStream.class)
						.withDescription("desc").withDefaultValue(is("x"))
						.withDeprecated());
		assertThat(read, containsProperty("b.c.d"));
		assertThat(read, containsProperty("c").withDefaultValue(is(123)));
		assertThat(read, containsProperty("d").withDefaultValue(is(true)));
		assertThat(read,
				containsProperty("e").withDefaultValue(is(new String[] { "y", "n" })));
		assertThat(read,
				containsProperty("f").withDefaultValue(is(new boolean[] { true, false })));
		assertThat(read, containsGroup("d"));
		assertThat(read, containsHint("a.b"));
		assertThat(read,
				containsHint("c").withValue(0, 123, "hey").withValue(1, 456, null));
		assertThat(read, containsHint("d").withProvider("first", "target", "foo")
				.withProvider("second"));
	}

}
