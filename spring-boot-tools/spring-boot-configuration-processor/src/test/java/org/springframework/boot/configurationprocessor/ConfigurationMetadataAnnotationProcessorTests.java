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

package org.springframework.boot.configurationprocessor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationsample.incremental.BarProperties;
import org.springframework.boot.configurationsample.incremental.FooProperties;
import org.springframework.boot.configurationsample.incremental.RenamedBarProperties;
import org.springframework.boot.configurationsample.lombok.LombokExplicitProperties;
import org.springframework.boot.configurationsample.lombok.LombokSimpleDataProperties;
import org.springframework.boot.configurationsample.lombok.LombokSimpleProperties;
import org.springframework.boot.configurationsample.method.EmptyTypeMethodConfig;
import org.springframework.boot.configurationsample.method.InvalidMethodConfig;
import org.springframework.boot.configurationsample.method.MethodAndClassConfig;
import org.springframework.boot.configurationsample.method.SimpleMethodConfig;
import org.springframework.boot.configurationsample.simple.HierarchicalProperties;
import org.springframework.boot.configurationsample.simple.NotAnnotated;
import org.springframework.boot.configurationsample.simple.SimpleCollectionProperties;
import org.springframework.boot.configurationsample.simple.SimplePrefixValueProperties;
import org.springframework.boot.configurationsample.simple.SimpleProperties;
import org.springframework.boot.configurationsample.simple.SimpleTypeProperties;
import org.springframework.boot.configurationsample.specific.BuilderPojo;
import org.springframework.boot.configurationsample.specific.ExcludedTypesPojo;
import org.springframework.boot.configurationsample.specific.InnerClassAnnotatedGetterConfig;
import org.springframework.boot.configurationsample.specific.InnerClassProperties;
import org.springframework.boot.configurationsample.specific.InnerClassRootConfig;
import org.springframework.boot.configurationsample.specific.SimplePojo;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.configurationprocessor.ConfigurationMetadataMatchers.containsGroup;
import static org.springframework.boot.configurationprocessor.ConfigurationMetadataMatchers.containsHint;
import static org.springframework.boot.configurationprocessor.ConfigurationMetadataMatchers.containsProperty;
import static org.springframework.boot.configurationprocessor.MetadataStore.METADATA_PATH;

/**
 * Tests for {@link ConfigurationMetadataAnnotationProcessor}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kris De Volder
 */
public class ConfigurationMetadataAnnotationProcessorTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private TestCompiler compiler;

	@Before
	public void createCompiler() throws IOException {
		this.compiler = new TestCompiler(this.temporaryFolder);
	}

	@Test
	public void notAnnotated() throws Exception {
		ConfigurationMetadata metadata = compile(NotAnnotated.class);
		assertThat("No config metadata file should have been generated when "
				+ "no metadata is discovered", metadata.getItems(), empty());
	}

	@Test
	public void simpleProperties() throws Exception {
		ConfigurationMetadata metadata = compile(SimpleProperties.class);
		assertThat(metadata, containsGroup("simple").fromSource(SimpleProperties.class));
		assertThat(
				metadata,
				containsProperty("simple.the-name", String.class)
						.fromSource(SimpleProperties.class)
						.withDescription("The name of this simple properties.")
						.withDefaultValue(is("boot")).withDeprecated());
		assertThat(
				metadata,
				containsProperty("simple.flag", Boolean.class)
						.fromSource(SimpleProperties.class)
						.withDescription("A simple flag.").withDeprecated());
		assertThat(metadata, containsProperty("simple.comparator"));
		assertThat(metadata, not(containsProperty("simple.counter")));
		assertThat(metadata, not(containsProperty("simple.size")));
	}

	@Test
	public void simplePrefixValueProperties() throws Exception {
		ConfigurationMetadata metadata = compile(SimplePrefixValueProperties.class);
		assertThat(metadata,
				containsGroup("simple").fromSource(SimplePrefixValueProperties.class));
		assertThat(
				metadata,
				containsProperty("simple.name", String.class).fromSource(
						SimplePrefixValueProperties.class));
	}

	@Test
	public void simpleTypeProperties() throws Exception {
		ConfigurationMetadata metadata = compile(SimpleTypeProperties.class);
		assertThat(metadata,
				containsGroup("simple.type").fromSource(SimpleTypeProperties.class));
		assertThat(metadata, containsProperty("simple.type.my-string", String.class));
		assertThat(metadata, containsProperty("simple.type.my-byte", Byte.class));
		assertThat(metadata,
				containsProperty("simple.type.my-primitive-byte", Byte.class));
		assertThat(metadata, containsProperty("simple.type.my-char", Character.class));
		assertThat(metadata,
				containsProperty("simple.type.my-primitive-char", Character.class));
		assertThat(metadata, containsProperty("simple.type.my-boolean", Boolean.class));
		assertThat(metadata,
				containsProperty("simple.type.my-primitive-boolean", Boolean.class));
		assertThat(metadata, containsProperty("simple.type.my-short", Short.class));
		assertThat(metadata,
				containsProperty("simple.type.my-primitive-short", Short.class));
		assertThat(metadata, containsProperty("simple.type.my-integer", Integer.class));
		assertThat(metadata,
				containsProperty("simple.type.my-primitive-integer", Integer.class));
		assertThat(metadata, containsProperty("simple.type.my-long", Long.class));
		assertThat(metadata,
				containsProperty("simple.type.my-primitive-long", Long.class));
		assertThat(metadata, containsProperty("simple.type.my-double", Double.class));
		assertThat(metadata,
				containsProperty("simple.type.my-primitive-double", Double.class));
		assertThat(metadata, containsProperty("simple.type.my-float", Float.class));
		assertThat(metadata,
				containsProperty("simple.type.my-primitive-float", Float.class));
		assertThat(metadata.getItems().size(), equalTo(18));
	}

	@Test
	public void hierarchicalProperties() throws Exception {
		ConfigurationMetadata metadata = compile(HierarchicalProperties.class);
		assertThat(metadata,
				containsGroup("hierarchical").fromSource(HierarchicalProperties.class));
		assertThat(metadata, containsProperty("hierarchical.first", String.class)
				.fromSource(HierarchicalProperties.class));
		assertThat(metadata, containsProperty("hierarchical.second", String.class)
				.fromSource(HierarchicalProperties.class));
		assertThat(metadata, containsProperty("hierarchical.third", String.class)
				.fromSource(HierarchicalProperties.class));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void deprecatedProperties() throws Exception {
		Class<?> type = org.springframework.boot.configurationsample.simple.DeprecatedProperties.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata, containsGroup("deprecated").fromSource(type));
		assertThat(metadata, containsProperty("deprecated.name", String.class)
				.fromSource(type).withDeprecated());
		assertThat(metadata, containsProperty("deprecated.description", String.class)
				.fromSource(type).withDeprecated());
	}

	@Test
	public void parseCollectionConfig() throws Exception {
		ConfigurationMetadata metadata = compile(SimpleCollectionProperties.class);
		// getter and setter
		assertThat(
				metadata,
				containsProperty("collection.integers-to-names",
						"java.util.Map<java.lang.Integer,java.lang.String>"));
		assertThat(
				metadata,
				containsProperty("collection.longs",
						"java.util.Collection<java.lang.Long>"));
		assertThat(metadata,
				containsProperty("collection.floats", "java.util.List<java.lang.Float>"));
		// getter only
		assertThat(
				metadata,
				containsProperty("collection.names-to-integers",
						"java.util.Map<java.lang.String,java.lang.Integer>"));
		assertThat(
				metadata,
				containsProperty("collection.bytes",
						"java.util.Collection<java.lang.Byte>"));
		assertThat(
				metadata,
				containsProperty("collection.doubles", "java.util.List<java.lang.Double>"));
	}

	@Test
	public void simpleMethodConfig() throws Exception {
		ConfigurationMetadata metadata = compile(SimpleMethodConfig.class);
		assertThat(metadata, containsGroup("foo").fromSource(SimpleMethodConfig.class));
		assertThat(
				metadata,
				containsProperty("foo.name", String.class).fromSource(
						SimpleMethodConfig.Foo.class));
		assertThat(
				metadata,
				containsProperty("foo.flag", Boolean.class).fromSource(
						SimpleMethodConfig.Foo.class));
	}

	@Test
	public void invalidMethodConfig() throws Exception {
		ConfigurationMetadata metadata = compile(InvalidMethodConfig.class);
		assertThat(
				metadata,
				containsProperty("something.name", String.class).fromSource(
						InvalidMethodConfig.class));
		assertThat(metadata, not(containsProperty("invalid.name")));
	}

	@Test
	public void methodAndClassConfig() throws Exception {
		ConfigurationMetadata metadata = compile(MethodAndClassConfig.class);
		assertThat(
				metadata,
				containsProperty("conflict.name", String.class).fromSource(
						MethodAndClassConfig.Foo.class));
		assertThat(
				metadata,
				containsProperty("conflict.flag", Boolean.class).fromSource(
						MethodAndClassConfig.Foo.class));
		assertThat(
				metadata,
				containsProperty("conflict.value", String.class).fromSource(
						MethodAndClassConfig.class));
	}

	@Test
	public void emptyTypeMethodConfig() throws Exception {
		ConfigurationMetadata metadata = compile(EmptyTypeMethodConfig.class);
		assertThat(metadata, not(containsProperty("something.foo")));
	}

	@Test
	public void innerClassRootConfig() throws Exception {
		ConfigurationMetadata metadata = compile(InnerClassRootConfig.class);
		assertThat(metadata, containsProperty("config.name"));
	}

	@Test
	public void innerClassProperties() throws Exception {
		ConfigurationMetadata metadata = compile(InnerClassProperties.class);
		assertThat(metadata,
				containsGroup("config").fromSource(InnerClassProperties.class));
		assertThat(metadata,
				containsGroup("config.first").ofType(InnerClassProperties.Foo.class)
						.fromSource(InnerClassProperties.class));
		assertThat(metadata, containsProperty("config.first.name"));
		assertThat(metadata, containsProperty("config.first.bar.name"));
		assertThat(metadata,
				containsGroup("config.the-second", InnerClassProperties.Foo.class)
						.fromSource(InnerClassProperties.class));
		assertThat(metadata, containsProperty("config.the-second.name"));
		assertThat(metadata, containsProperty("config.the-second.bar.name"));
		assertThat(metadata, containsGroup("config.third").ofType(SimplePojo.class)
				.fromSource(InnerClassProperties.class));
		assertThat(metadata, containsProperty("config.third.value"));
		assertThat(metadata, containsProperty("config.fourth"));
		assertThat(metadata, not(containsGroup("config.fourth")));
	}

	@Test
	public void innerClassAnnotatedGetterConfig() throws Exception {
		ConfigurationMetadata metadata = compile(InnerClassAnnotatedGetterConfig.class);
		assertThat(metadata, containsProperty("specific.value"));
		assertThat(metadata, containsProperty("foo.name"));
		assertThat(metadata, not(containsProperty("specific.foo")));
	}

	@Test
	public void builderPojo() throws IOException {
		ConfigurationMetadata metadata = compile(BuilderPojo.class);
		assertThat(metadata, containsProperty("builder.name"));
	}

	@Test
	public void excludedTypesPojo() throws IOException {
		ConfigurationMetadata metadata = compile(ExcludedTypesPojo.class);
		assertThat(metadata, containsProperty("excluded.name"));
		assertThat(metadata, not(containsProperty("excluded.class-loader")));
		assertThat(metadata, not(containsProperty("excluded.data-source")));
		assertThat(metadata, not(containsProperty("excluded.print-writer")));
		assertThat(metadata, not(containsProperty("excluded.writer")));
		assertThat(metadata, not(containsProperty("excluded.writer-array")));
	}

	@Test
	public void lombokDataProperties() throws Exception {
		ConfigurationMetadata metadata = compile(LombokSimpleDataProperties.class);
		assertSimpleLombokProperties(metadata, LombokSimpleDataProperties.class, "data");
	}

	@Test
	public void lombokSimpleProperties() throws Exception {
		ConfigurationMetadata metadata = compile(LombokSimpleProperties.class);
		assertSimpleLombokProperties(metadata, LombokSimpleProperties.class, "simple");
	}

	@Test
	public void lombokExplicitProperties() throws Exception {
		ConfigurationMetadata metadata = compile(LombokExplicitProperties.class);
		assertSimpleLombokProperties(metadata, LombokExplicitProperties.class, "explicit");
	}

	@Test
	public void mergingOfAdditionalProperty() throws Exception {
		File additionalMetadataFile = createAdditionalMetadataFile();

		JSONObject property = new JSONObject();
		property.put("name", "foo");
		property.put("type", "java.lang.String");
		property.put("sourceType", AdditionalMetadata.class.getName());
		JSONArray properties = new JSONArray();
		properties.put(property);
		JSONObject additionalMetadata = new JSONObject();
		additionalMetadata.put("properties", properties);

		writeMetadata(additionalMetadataFile, additionalMetadata);
		ConfigurationMetadata metadata = compile(SimpleProperties.class);

		assertThat(metadata, containsProperty("simple.comparator"));

		assertThat(metadata,
				containsProperty("foo", String.class)
						.fromSource(AdditionalMetadata.class));
	}

	@Test
	public void mergingOfSimpleHint() throws Exception {
		writeAdditionalHints(ItemHint.newHint("simple.the-name", new ItemHint.ValueHint(
				"boot", "Bla bla"), new ItemHint.ValueHint("spring", null)));

		ConfigurationMetadata metadata = compile(SimpleProperties.class);
		assertThat(metadata,
				containsHint("simple.the-name").withValue(0, "boot", "Bla bla")
						.withValue(1, "spring", null));
	}

	@Test
	public void mergingOfHintWithNonCanonicalName() throws Exception {
		writeAdditionalHints(ItemHint.newHint("simple.theName", new ItemHint.ValueHint(
				"boot", "Bla bla")));

		ConfigurationMetadata metadata = compile(SimpleProperties.class);
		assertThat(metadata,
				containsHint("simple.the-name").withValue(0, "boot", "Bla bla"));
	}

	@Test
	public void mergingOfHintWithProvider() throws Exception {
		writeAdditionalHints(
				new ItemHint("simple.theName", Collections.<ItemHint.ValueHint>emptyList(), Arrays.asList(
						new ItemHint.ProviderHint("first", Collections.<String,Object>singletonMap("target", "org.foo")),
						new ItemHint.ProviderHint("second", null))
				));

		ConfigurationMetadata metadata = compile(SimpleProperties.class);
		assertThat(metadata, containsHint("simple.the-name")
				.withProvider("first", "target", "org.foo")
				.withProvider("second"));
	}

	@Test
	public void incrementalBuild() throws Exception {
		TestProject project = new TestProject(this.temporaryFolder, FooProperties.class,
				BarProperties.class);
		assertFalse(project.getOutputFile(METADATA_PATH).exists());

		ConfigurationMetadata metadata = project.fullBuild();
		assertTrue(project.getOutputFile(METADATA_PATH).exists());

		assertThat(metadata,
				containsProperty("foo.counter").fromSource(FooProperties.class));
		assertThat(metadata,
				containsProperty("bar.counter").fromSource(BarProperties.class));

		metadata = project.incrementalBuild(BarProperties.class);

		assertThat(metadata,
				containsProperty("foo.counter").fromSource(FooProperties.class));
		assertThat(metadata,
				containsProperty("bar.counter").fromSource(BarProperties.class));

		project.addSourceCode(BarProperties.class,
				BarProperties.class.getResourceAsStream("BarProperties.snippet"));
		metadata = project.incrementalBuild(BarProperties.class);
		assertThat(metadata, containsProperty("bar.extra"));
		assertThat(metadata, containsProperty("foo.counter"));
		assertThat(metadata, containsProperty("bar.counter"));

		project.revert(BarProperties.class);
		metadata = project.incrementalBuild(BarProperties.class);
		assertThat(metadata, not(containsProperty("bar.extra")));
		assertThat(metadata, containsProperty("foo.counter"));
		assertThat(metadata, containsProperty("bar.counter"));
	}

	@Test
	public void incrementalBuildAnnotationRemoved() throws Exception {
		TestProject project = new TestProject(this.temporaryFolder, FooProperties.class,
				BarProperties.class);
		ConfigurationMetadata metadata = project.fullBuild();
		assertThat(metadata, containsProperty("foo.counter"));
		assertThat(metadata, containsProperty("bar.counter"));

		project.replaceText(BarProperties.class, "@ConfigurationProperties",
				"//@ConfigurationProperties");
		metadata = project.incrementalBuild(BarProperties.class);
		assertThat(metadata, containsProperty("foo.counter"));
		assertThat(metadata, not(containsProperty("bar.counter")));
	}

	@Test
	public void incrementalBuildTypeRenamed() throws Exception {
		TestProject project = new TestProject(this.temporaryFolder, FooProperties.class,
				BarProperties.class);
		ConfigurationMetadata metadata = project.fullBuild();
		assertThat(metadata,
				containsProperty("foo.counter").fromSource(FooProperties.class));
		assertThat(metadata,
				containsProperty("bar.counter").fromSource(BarProperties.class));
		assertThat(metadata,
				not(containsProperty("bar.counter")
						.fromSource(RenamedBarProperties.class)));

		project.delete(BarProperties.class);
		project.add(RenamedBarProperties.class);
		metadata = project.incrementalBuild(RenamedBarProperties.class);
		assertThat(metadata,
				containsProperty("foo.counter").fromSource(FooProperties.class));
		assertThat(metadata,
				not(containsProperty("bar.counter").fromSource(BarProperties.class)));
		assertThat(metadata,
				containsProperty("bar.counter").fromSource(RenamedBarProperties.class));
	}

	private void assertSimpleLombokProperties(ConfigurationMetadata metadata,
			Class<?> source, String prefix) {
		assertThat(metadata, containsGroup(prefix).fromSource(source));
		assertThat(metadata, not(containsProperty(prefix + ".id")));
		assertThat(metadata,
				containsProperty(prefix + ".name", String.class).fromSource(source)
						.withDescription("Name description."));
		assertThat(metadata, containsProperty(prefix + ".description"));
		assertThat(metadata, containsProperty(prefix + ".counter"));
		assertThat(metadata, containsProperty(prefix + ".number").fromSource(source)
				.withDefaultValue(is(0)).withDeprecated());
		assertThat(metadata, containsProperty(prefix + ".items"));
		assertThat(metadata, not(containsProperty(prefix + ".ignored")));
	}

	private ConfigurationMetadata compile(Class<?>... types) throws IOException {
		TestConfigurationMetadataAnnotationProcessor processor = new TestConfigurationMetadataAnnotationProcessor(
				this.compiler.getOutputLocation());
		this.compiler.getTask(types).call(processor);
		return processor.getMetadata();
	}

	private void writeAdditionalHints(ItemHint... hints) throws IOException {
		File additionalMetadataFile = createAdditionalMetadataFile();

		JSONArray hintsArray = new JSONArray();
		for (ItemHint hint : hints) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("name", hint.getName());
			JSONArray valuesArray = new JSONArray();
			for (ItemHint.ValueHint valueHint : hint.getValues()) {
				JSONObject valueJsonObject = new JSONObject();
				valueJsonObject.put("value", valueHint.getValue());
				String description = valueHint.getDescription();
				if (description != null) {
					valueJsonObject.put("description", description);
				}
				valuesArray.put(valueJsonObject);
			}
			jsonObject.put("values", valuesArray);
			hintsArray.put(jsonObject);
		}
		JSONObject additionalMetadata = new JSONObject();
		additionalMetadata.put("hints", hints);
		writeMetadata(additionalMetadataFile, additionalMetadata);
	}

	private File createAdditionalMetadataFile() throws IOException {
		File metaInfFolder = new File(this.compiler.getOutputLocation(), "META-INF");
		metaInfFolder.mkdirs();
		File additionalMetadataFile = new File(metaInfFolder,
				"additional-spring-configuration-metadata.json");
		additionalMetadataFile.createNewFile();
		return additionalMetadataFile;
	}

	private void writeMetadata(File metadataFile, JSONObject metadata) throws IOException {
		FileWriter writer = new FileWriter(metadataFile);
		try {
			metadata.write(writer);
		}
		finally {
			writer.close();
		}
	}

	private static class AdditionalMetadata {

	}

}
