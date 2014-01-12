/*
 * Copyright 2010-2012 the original author or authors.
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

package org.springframework.boot.context.listener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationEnvironmentAvailableEvent;
import org.springframework.boot.config.PropertySourceLoader;
import org.springframework.boot.context.listener.ConfigFileApplicationListener.PropertySourceLoaderFactory;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ConfigFileApplicationListener}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public class ConfigFileApplicationListenerTests {

	private StandardEnvironment environment = new StandardEnvironment();

	private SpringApplicationEnvironmentAvailableEvent event = new SpringApplicationEnvironmentAvailableEvent(
			new SpringApplication(), this.environment, new String[0]);

	private ConfigFileApplicationListener initializer = new ConfigFileApplicationListener();

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@After
	public void cleanup() {
		System.clearProperty("my.property");
		System.clearProperty("spring.config.location");
	}

	@Test
	public void loadPropertiesFile() throws Exception {
		this.initializer.setNames("testproperties");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(property, equalTo("frompropertiesfile"));
	}

	@Test
	public void randomValue() throws Exception {
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("random.value");
		assertThat(property, notNullValue());
	}

	@Test
	public void loadTwoPropertiesFiles() throws Exception {
		this.initializer.setNames("testproperties,moreproperties");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(property, equalTo("frommorepropertiesfile"));
	}

	@Test
	public void loadYamlFile() throws Exception {
		this.initializer.setNames("testyaml");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(property, equalTo("fromyamlfile"));
		assertThat(this.environment.getProperty("my.array[0]"), equalTo("1"));
		assertThat(this.environment.getProperty("my.array"), nullValue(String.class));
	}

	@Test
	public void commandLineWins() throws Exception {
		this.environment.getPropertySources().addFirst(
				new SimpleCommandLinePropertySource("--my.property=fromcommandline"));
		this.initializer.setNames("testproperties");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(property, equalTo("fromcommandline"));
	}

	@Test
	public void systemPropertyWins() throws Exception {
		System.setProperty("my.property", "fromsystem");
		this.initializer.setNames("testproperties");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(property, equalTo("fromsystem"));
	}

	@Test
	public void loadPropertiesThenProfileProperties() throws Exception {
		this.initializer.setNames("enableprofile");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(property, equalTo("fromprofilepropertiesfile"));
	}

	@Test
	public void profilePropertiesUsedInPlaceholders() throws Exception {
		this.initializer.setNames("enableprofile");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("one.more");
		assertThat(property, equalTo("fromprofilepropertiesfile"));
	}

	@Test
	public void yamlProfiles() throws Exception {
		this.initializer.setNames("testprofiles");
		this.environment.setActiveProfiles("dev");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(property, equalTo("fromdevprofile"));
		property = this.environment.getProperty("my.other");
		assertThat(property, equalTo("notempty"));
	}

	@Test
	public void yamlSetsProfiles() throws Exception {
		this.initializer.setNames("testsetprofiles");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(Arrays.asList(this.environment.getActiveProfiles()), contains("dev"));
		assertThat(property, equalTo("fromdevprofile"));
	}

	@Test
	public void yamlProfileCanBeChanged() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.environment,
				"spring.profiles.active:prod");
		this.initializer.setNames("testsetprofiles");
		this.initializer.onApplicationEvent(this.event);
		assertThat(Arrays.asList(this.environment.getActiveProfiles()).toString(),
				equalTo("[prod]"));
	}

	@Test
	public void specificNameAndProfileFromExistingSource() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("spring.profiles.active", "specificprofile");
		map.put("spring.config.name", "specificfile");
		MapPropertySource source = new MapPropertySource("map", map);
		this.environment.getPropertySources().addFirst(source);
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(property, equalTo("fromspecificpropertiesfile"));
	}

	@Test
	public void specificResource() throws Exception {
		String location = "classpath:specificlocation.properties";
		EnvironmentTestUtils.addEnvironment(this.environment, "spring.config.location:"
				+ location);
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(property, equalTo("fromspecificlocation"));
		assertThat(this.environment.getPropertySources().contains(location), is(true));
		// The default property source is still there
		assertThat(
				this.environment.getPropertySources().contains(
						"classpath:application.properties"), is(true));
		assertThat(this.environment.getProperty("foo"), equalTo("bucket"));
	}

	@Test
	public void specificResourceAsFile() throws Exception {
		String location = "file:src/test/resources/specificlocation.properties";
		EnvironmentTestUtils.addEnvironment(this.environment, "spring.config.location:"
				+ location);
		this.initializer.onApplicationEvent(this.event);
		assertThat(this.environment.getPropertySources().contains(location), is(true));
	}

	@Test
	public void unsupportedResource() throws Exception {
		this.initializer
				.setPropertySourceLoaderFactory(new PropertySourceLoaderFactory() {
					@Override
					public List<PropertySourceLoader> getLoaders(Environment environment) {
						return Arrays
								.<PropertySourceLoader> asList(new PropertySourceLoader() {

									@Override
									public boolean supports(Resource resource) {
										return false;
									}

									@Override
									public org.springframework.core.env.PropertySource<?> load(
											String name, Resource resource) {
										return null;
									}

								});
					}
				});
		this.expected.expect(IllegalStateException.class);
		this.expected.expectMessage("No supported loader");
		this.initializer.onApplicationEvent(this.event);
	}

	@Test
	public void specificResourceDefaultsToFile() throws Exception {
		String location = "src/test/resources/specificlocation.properties";
		EnvironmentTestUtils.addEnvironment(this.environment, "spring.config.location:"
				+ location);
		this.initializer.onApplicationEvent(this.event);
		assertThat(this.environment.getPropertySources().contains("file:" + location),
				is(true));
	}

	@Test
	public void propertySourceAnnotation() throws Exception {
		SpringApplication application = new SpringApplication(WithPropertySource.class);
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("fromspecificlocation"));
		assertNotNull(context.getEnvironment().getPropertySources()
				.get("classpath:/specificlocation.properties"));
		context.close();
	}

	@Test
	public void propertySourceAnnotationWithName() throws Exception {
		SpringApplication application = new SpringApplication(
				WithPropertySourceAndName.class);
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("fromspecificlocation"));
		// In this case "foo" should be the specificlocation.properties source, but Spring
		// will have shifted it to the back of the line.
		assertNotNull(context.getEnvironment().getPropertySources().get("boot.foo"));
		context.close();
	}

	@Test
	public void propertySourceAnnotationInProfile() throws Exception {
		SpringApplication application = new SpringApplication(
				WithPropertySourceInProfile.class);
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application
				.run("--spring.profiles.active=myprofile");
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("frompropertiesfile"));
		assertNotNull(context.getEnvironment().getPropertySources()
				.get("classpath:/enableprofile.properties"));
		assertNull(context.getEnvironment().getPropertySources()
				.get("classpath:/enableprofile-myprofile.properties"));
		context.close();
	}

	@Test
	public void propertySourceAnnotationAndProfile() throws Exception {
		SpringApplication application = new SpringApplication(
				WithPropertySourceAndProfile.class);
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo(null));
		assertNull(context.getEnvironment().getPropertySources()
				.get("classpath:/enableprofile-myprofile.properties"));
		context.close();
	}

	@Test
	public void propertySourceAnnotationMultipleLocations() throws Exception {
		SpringApplication application = new SpringApplication(
				WithPropertySourceMultipleLocations.class);
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("frommorepropertiesfile"));
		assertNotNull(context.getEnvironment().getPropertySources()
				.get("classpath:/specificlocation.properties"));
		context.close();
	}

	@Test
	public void propertySourceAnnotationMultipleLocationsAndName() throws Exception {
		SpringApplication application = new SpringApplication(
				WithPropertySourceMultipleLocationsAndName.class);
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("frommorepropertiesfile"));
		// foo is there but it is a dead rubber because the individual sources get higher
		// priority (and are named after the resource locations)
		assertNotNull(context.getEnvironment().getPropertySources().get("foo"));
		assertNotNull(context.getEnvironment().getPropertySources()
				.get("classpath:/specificlocation.properties"));
		context.close();
	}

	@Test
	public void defaultApplicationProperties() throws Exception {

	}

	@Configuration
	@PropertySource("classpath:/specificlocation.properties")
	protected static class WithPropertySource {

	}

	@Configuration
	@PropertySource(value = "classpath:/specificlocation.properties", name = "foo")
	protected static class WithPropertySourceAndName {

	}

	@Configuration
	@PropertySource("classpath:/enableprofile.properties")
	protected static class WithPropertySourceInProfile {

	}

	@Configuration
	@PropertySource("classpath:/enableprofile-myprofile.properties")
	@Profile("myprofile")
	protected static class WithPropertySourceAndProfile {

	}

	@Configuration
	@PropertySource({ "classpath:/specificlocation.properties",
			"classpath:/moreproperties.properties" })
	protected static class WithPropertySourceMultipleLocations {

	}

	@Configuration
	@PropertySource(value = { "classpath:/specificlocation.properties",
			"classpath:/moreproperties.properties" }, name = "foo")
	protected static class WithPropertySourceMultipleLocationsAndName {

	}

}
