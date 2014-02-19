/*
 * Copyright 2010-2014 the original author or authors.
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

package org.springframework.boot.context.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener.ConfigurationPropertySources;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ReflectionUtils;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ConfigFileApplicationListener}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public class ConfigFileApplicationListenerTests {

	private final StandardEnvironment environment = new StandardEnvironment();

	private final ApplicationEnvironmentPreparedEvent event = new ApplicationEnvironmentPreparedEvent(
			new SpringApplication(), new String[0], this.environment);

	private final ConfigFileApplicationListener initializer = new ConfigFileApplicationListener();

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@After
	public void cleanup() {
		System.clearProperty("my.property");
		System.clearProperty("spring.config.location");
	}

	@Test
	public void loadPropertiesFile() throws Exception {
		this.initializer.setSearchNames("testproperties");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(property, equalTo("frompropertiesfile"));
	}

	@Test
	public void loadTwoPropertiesFile() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.environment, "spring.config.location:"
				+ "classpath:testproperties.properties,"
				+ "classpath:application.properties");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(property, equalTo("frompropertiesfile"));
	}

	@Test
	public void localFileTakesPrecedenceOverClasspath() throws Exception {
		File localFile = new File(new File("."), "application.properties");
		assertThat(localFile.exists(), equalTo(false));
		try {
			Properties properties = new Properties();
			properties.put("my.property", "fromlocalfile");
			OutputStream out = new FileOutputStream(localFile);
			try {
				properties.store(out, "");
			}
			finally {
				out.close();
			}
			this.initializer.onApplicationEvent(this.event);
			String property = this.environment.getProperty("my.property");
			assertThat(property, equalTo("fromlocalfile"));
		}
		finally {
			localFile.delete();
		}
	}

	@Test
	public void loadTwoOfThreePropertiesFile() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.environment, "spring.config.location:"
				+ "classpath:testproperties.properties,"
				+ "classpath:application.properties,"
				+ "classpath:nonexistent.properties");
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
		this.initializer.setSearchNames("moreproperties,testproperties");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(property, equalTo("frommorepropertiesfile"));
	}

	@Test
	public void loadYamlFile() throws Exception {
		this.initializer.setSearchNames("testyaml");
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
		this.initializer.setSearchNames("testproperties");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(property, equalTo("fromcommandline"));
	}

	@Test
	public void systemPropertyWins() throws Exception {
		System.setProperty("my.property", "fromsystem");
		this.initializer.setSearchNames("testproperties");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(property, equalTo("fromsystem"));
	}

	@Test
	public void loadPropertiesThenProfileProperties() throws Exception {
		this.initializer.setSearchNames("enableprofile");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(property, equalTo("fromprofilepropertiesfile"));
	}

	@Test
	public void profilePropertiesUsedInPlaceholders() throws Exception {
		this.initializer.setSearchNames("enableprofile");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("one.more");
		assertThat(property, equalTo("fromprofilepropertiesfile"));
	}

	@Test
	public void yamlProfiles() throws Exception {
		this.initializer.setSearchNames("testprofiles");
		this.environment.setActiveProfiles("dev");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(property, equalTo("fromdevprofile"));
		property = this.environment.getProperty("my.other");
		assertThat(property, equalTo("notempty"));
	}

	@Test
	public void yamlSetsProfiles() throws Exception {
		this.initializer.setSearchNames("testsetprofiles");
		this.initializer.onApplicationEvent(this.event);
		String property = this.environment.getProperty("my.property");
		assertThat(Arrays.asList(this.environment.getActiveProfiles()), contains("dev"));
		assertThat(property, equalTo("fromdevprofile"));
		ConfigurationPropertySources propertySource = (ConfigurationPropertySources) this.environment
				.getPropertySources().get("applicationConfigurationProperties");
		Collection<org.springframework.core.env.PropertySource<?>> sources = propertySource
				.getSource();
		assertEquals(2, sources.size());
		List<String> names = new ArrayList<String>();
		for (org.springframework.core.env.PropertySource<?> source : sources) {
			names.add(source.getName());
		}
		assertThat(
				names,
				contains(
						"applicationConfig: class path resource [testsetprofiles.yml]#dev",
						"applicationConfig: class path resource [testsetprofiles.yml]"));
	}

	@Test
	public void yamlProfileCanBeChanged() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.environment,
				"spring.profiles.active:prod");
		this.initializer.setSearchNames("testsetprofiles");
		this.initializer.onApplicationEvent(this.event);
		assertThat(this.environment.getActiveProfiles(), equalTo(new String[] { "prod" }));
	}

	@Test
	public void specificNameAndProfileFromExistingSource() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.environment,
				"spring.profiles.active=specificprofile",
				"spring.config.name=specificfile");
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
		assertThat(this.environment, containsProperySource("applicationConfig: "
				+ "class path resource [specificlocation.properties]"));
		// The default property source is still there
		assertThat(this.environment, containsProperySource("applicationConfig: "
				+ "class path resource [application.properties]"));
		assertThat(this.environment.getProperty("foo"), equalTo("bucket"));
	}

	@Test
	public void specificResourceAsFile() throws Exception {
		String location = "file:src/test/resources/specificlocation.properties";
		EnvironmentTestUtils.addEnvironment(this.environment, "spring.config.location:"
				+ location);
		this.initializer.onApplicationEvent(this.event);
		assertThat(this.environment, containsProperySource("applicationConfig: "
				+ "URL [" + location + "]"));
	}

	@Test
	public void specificResourceDefaultsToFile() throws Exception {
		String location = "src/test/resources/specificlocation.properties";
		EnvironmentTestUtils.addEnvironment(this.environment, "spring.config.location:"
				+ location);
		this.initializer.onApplicationEvent(this.event);
		assertThat(this.environment, containsProperySource("applicationConfig: "
				+ "URL [file:" + location + "]"));
	}

	@Test
	public void propertySourceAnnotation() throws Exception {
		SpringApplication application = new SpringApplication(WithPropertySource.class);
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("fromspecificlocation"));
		assertThat(context.getEnvironment(), containsProperySource("class path resource "
				+ "[specificlocation.properties]"));
		context.close();
	}

	@Test
	public void propertySourceAnnotationWithPlaceholder() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.environment,
				"source.location:specificlocation");
		SpringApplication application = new SpringApplication(
				WithPropertySourcePlaceholders.class);
		application.setEnvironment(this.environment);
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("fromspecificlocation"));
		assertThat(context.getEnvironment(), containsProperySource("class path resource "
				+ "[specificlocation.properties]"));
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
		assertThat(context.getEnvironment(), containsProperySource("foo"));
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
		assertThat(context.getEnvironment(), containsProperySource("class path resource "
				+ "[enableprofile.properties]"));
		assertThat(context.getEnvironment(), not(containsProperySource("classpath:/"
				+ "enableprofile-myprofile.properties")));
		context.close();
	}

	@Test
	public void propertySourceAnnotationAndNonActiveProfile() throws Exception {
		SpringApplication application = new SpringApplication(
				WithPropertySourceAndProfile.class);
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("fromapplicationproperties"));
		assertThat(context.getEnvironment(), not(containsProperySource("classpath:"
				+ "/enableprofile-myprofile.properties")));
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
		assertThat(context.getEnvironment(), containsProperySource("class path resource "
				+ "[specificlocation.properties]"));
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
		assertThat(context.getEnvironment(), containsProperySource("foo"));
		context.close();
	}

	@Test
	public void activateProfileFromProfileSpecificProperties() throws Exception {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application
				.run("--spring.profiles.active=activateprofile");
		assertThat(context.getEnvironment(), acceptsProfiles("activateprofile"));
		assertThat(context.getEnvironment(), acceptsProfiles("specific"));
		assertThat(context.getEnvironment(), acceptsProfiles("morespecific"));
		assertThat(context.getEnvironment(), acceptsProfiles("yetmorespecific"));
		assertThat(context.getEnvironment(), not(acceptsProfiles("missing")));
	}

	@Test
	public void profileSubDocumentInProfileSpecificFile() throws Exception {
		// gh-340
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application
				.run("--spring.profiles.active=activeprofilewithsubdoc");
		String property = context.getEnvironment().getProperty("foobar");
		assertThat(property, equalTo("baz"));
	}

	@Test
	public void bindsToSpringApplication() throws Exception {
		// gh-346
		this.initializer.setSearchNames("bindtoapplication");
		this.initializer.onApplicationEvent(this.event);
		SpringApplication application = this.event.getSpringApplication();
		Field field = ReflectionUtils.findField(SpringApplication.class, "showBanner");
		field.setAccessible(true);
		assertThat((Boolean) field.get(application), equalTo(false));
	}

	private static Matcher<? super ConfigurableEnvironment> containsProperySource(
			final String sourceName) {
		return new TypeSafeDiagnosingMatcher<ConfigurableEnvironment>() {
			@Override
			public void describeTo(Description description) {
				description.appendText("environment containing property source ")
						.appendValue(sourceName);
			}

			@Override
			protected boolean matchesSafely(ConfigurableEnvironment item,
					Description mismatchDescription) {
				MutablePropertySources sources = new MutablePropertySources(
						item.getPropertySources());
				ConfigurationPropertySources.finishAndRelocate(sources);
				mismatchDescription.appendText("Not matched against: ").appendValue(
						sources);
				return sources.contains(sourceName);
			}
		};
	}

	private static Matcher<? super ConfigurableEnvironment> acceptsProfiles(
			final String... profiles) {
		return new TypeSafeDiagnosingMatcher<ConfigurableEnvironment>() {
			@Override
			public void describeTo(Description description) {
				description.appendText("environment accepting profiles ").appendValue(
						profiles);
			}

			@Override
			protected boolean matchesSafely(ConfigurableEnvironment item,
					Description mismatchDescription) {
				mismatchDescription.appendText("Not matched against: ").appendValue(
						item.getActiveProfiles());
				return item.acceptsProfiles(profiles);
			}
		};
	}

	@Configuration
	protected static class Config {

	}

	@Configuration
	@PropertySource("classpath:/specificlocation.properties")
	protected static class WithPropertySource {

	}

	@Configuration
	@PropertySource("classpath:/${source.location}.properties")
	protected static class WithPropertySourcePlaceholders {

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
