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

package org.springframework.boot.configurationmetadata;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.json.JSONException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonReader}
 *
 * @author Stephane Nicoll
 */
public class JsonReaderTests extends AbstractConfigurationMetadataTests {

	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private final JsonReader reader = new JsonReader();

	@Test
	public void emptyMetadata() throws IOException {
		RawConfigurationMetadata rawMetadata = readFor("empty");
		assertThat(rawMetadata.getSources()).isEmpty();
		assertThat(rawMetadata.getItems()).isEmpty();
	}

	@Test
	public void invalidMetadata() throws IOException {
		this.thrown.expect(JSONException.class);
		readFor("invalid");
	}

	@Test
	public void emptyGroupName() throws IOException {
		RawConfigurationMetadata rawMetadata = readFor("empty-groups");
		List<ConfigurationMetadataItem> items = rawMetadata.getItems();
		assertThat(items).hasSize(2);

		ConfigurationMetadataItem name = items.get(0);
		assertProperty(name, "name", "name", String.class, null);
		ConfigurationMetadataItem dotTitle = items.get(1);
		assertProperty(dotTitle, "title", "title", String.class, null);
	}

	@Test
	public void simpleMetadata() throws IOException {
		RawConfigurationMetadata rawMetadata = readFor("foo");
		List<ConfigurationMetadataSource> sources = rawMetadata.getSources();
		assertThat(sources).hasSize(2);
		List<ConfigurationMetadataItem> items = rawMetadata.getItems();
		assertThat(items).hasSize(4);
		List<ConfigurationMetadataHint> hints = rawMetadata.getHints();
		assertThat(hints).hasSize(1);

		ConfigurationMetadataSource source = sources.get(0);
		assertSource(source, "spring.foo", "org.acme.Foo", "org.acme.config.FooApp");
		assertThat(source.getSourceMethod()).isEqualTo("foo()");
		assertThat(source.getDescription()).isEqualTo("This is Foo.");
		assertThat(source.getShortDescription()).isEqualTo("This is Foo.");

		ConfigurationMetadataItem item = items.get(0);
		assertProperty(item, "spring.foo.name", "name", String.class, null);
		assertItem(item, "org.acme.Foo");
		ConfigurationMetadataItem item2 = items.get(1);
		assertProperty(item2, "spring.foo.description", "description", String.class,
				"FooBar");
		assertThat(item2.getDescription()).isEqualTo("Foo description.");
		assertThat(item2.getShortDescription()).isEqualTo("Foo description.");
		assertThat(item2.getSourceMethod()).isNull();
		assertItem(item2, "org.acme.Foo");

		ConfigurationMetadataHint hint = hints.get(0);
		assertThat(hint.getId()).isEqualTo("spring.foo.counter");
		assertThat(hint.getValueHints()).hasSize(1);
		ValueHint valueHint = hint.getValueHints().get(0);
		assertThat(valueHint.getValue()).isEqualTo(42);
		assertThat(valueHint.getDescription()).isEqualTo(
				"Because that's the answer to any question, choose it. \nReally.");
		assertThat(valueHint.getShortDescription())
				.isEqualTo("Because that's the answer to any question, choose it.");
		assertThat(hint.getValueProviders()).hasSize(1);
		ValueProvider valueProvider = hint.getValueProviders().get(0);
		assertThat(valueProvider.getName()).isEqualTo("handle-as");
		assertThat(valueProvider.getParameters()).hasSize(1);
		assertThat(valueProvider.getParameters().get("target"))
				.isEqualTo(Integer.class.getName());
	}

	@Test
	public void metadataHints() throws IOException {
		RawConfigurationMetadata rawMetadata = readFor("bar");
		List<ConfigurationMetadataHint> hints = rawMetadata.getHints();
		assertThat(hints).hasSize(1);

		ConfigurationMetadataHint hint = hints.get(0);
		assertThat(hint.getId()).isEqualTo("spring.bar.description");
		assertThat(hint.getValueHints()).hasSize(2);
		ValueHint valueHint = hint.getValueHints().get(0);
		assertThat(valueHint.getValue()).isEqualTo("one");
		assertThat(valueHint.getDescription()).isEqualTo("One.");
		ValueHint valueHint2 = hint.getValueHints().get(1);
		assertThat(valueHint2.getValue()).isEqualTo("two");
		assertThat(valueHint2.getDescription()).isEqualTo(null);

		assertThat(hint.getValueProviders()).hasSize(2);
		ValueProvider valueProvider = hint.getValueProviders().get(0);
		assertThat(valueProvider.getName()).isEqualTo("handle-as");
		assertThat(valueProvider.getParameters()).hasSize(1);
		assertThat(valueProvider.getParameters().get("target"))
				.isEqualTo(String.class.getName());
		ValueProvider valueProvider2 = hint.getValueProviders().get(1);
		assertThat(valueProvider2.getName()).isEqualTo("any");
		assertThat(valueProvider2.getParameters()).isEmpty();
	}

	@Test
	public void rootMetadata() throws IOException {
		RawConfigurationMetadata rawMetadata = readFor("root");
		List<ConfigurationMetadataSource> sources = rawMetadata.getSources();
		assertThat(sources).isEmpty();
		List<ConfigurationMetadataItem> items = rawMetadata.getItems();
		assertThat(items).hasSize(2);
		ConfigurationMetadataItem item = items.get(0);
		assertProperty(item, "spring.root.name", "spring.root.name", String.class, null);
	}

	@Test
	public void deprecatedMetadata() throws IOException {
		RawConfigurationMetadata rawMetadata = readFor("deprecated");
		List<ConfigurationMetadataItem> items = rawMetadata.getItems();
		assertThat(items).hasSize(3);

		ConfigurationMetadataItem item = items.get(0);
		assertProperty(item, "server.port", "server.port", Integer.class, null);
		assertThat(item.isDeprecated()).isTrue();
		assertThat(item.getDeprecation().getReason())
				.isEqualTo("Server namespace has moved to spring.server");
		assertThat(item.getDeprecation().getReplacement())
				.isEqualTo("server.spring.port");

		ConfigurationMetadataItem item2 = items.get(1);
		assertProperty(item2, "server.cluster-name", "server.cluster-name", String.class,
				null);
		assertThat(item2.isDeprecated()).isTrue();
		assertThat(item2.getDeprecation().getReason()).isEqualTo(null);
		assertThat(item2.getDeprecation().getReplacement()).isEqualTo(null);

		ConfigurationMetadataItem item3 = items.get(2);
		assertProperty(item3, "spring.server.name", "spring.server.name", String.class,
				null);
		assertThat(item3.isDeprecated()).isFalse();
		assertThat(item3.getDeprecation()).isEqualTo(null);
	}

	RawConfigurationMetadata readFor(String path) throws IOException {
		return this.reader.read(getInputStreamFor(path), DEFAULT_CHARSET);
	}

}
