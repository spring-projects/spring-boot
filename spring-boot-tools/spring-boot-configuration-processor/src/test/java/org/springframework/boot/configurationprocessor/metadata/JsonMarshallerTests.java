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

package org.springframework.boot.configurationprocessor.metadata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.springframework.boot.configurationprocessor.ConfigurationMetadataMatchers.containsGroup;
import static org.springframework.boot.configurationprocessor.ConfigurationMetadataMatchers.containsProperty;

/**
 * Tests for {@link JsonMarshaller}.
 *
 * @author Phillip Webb
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
		metadata.add(ItemMetadata.newGroup("d", null, null, null));
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		JsonMarshaller marshaller = new JsonMarshaller();
		marshaller.write(metadata, outputStream);
		System.out.println(outputStream);
		ConfigurationMetadata read = marshaller.read(new ByteArrayInputStream(
				outputStream.toByteArray()));
		assertThat(read,
				containsProperty("a.b", StringBuffer.class).fromSource(InputStream.class)
						.withDescription("desc").withDefaultValue("x").withDeprecated());
		assertThat(read, containsProperty("b.c.d"));
		assertThat(read, containsProperty("c").withDefaultValue(123));
		assertThat(read, containsProperty("d").withDefaultValue(true));
		assertThat(read, containsGroup("d"));
	}

}
