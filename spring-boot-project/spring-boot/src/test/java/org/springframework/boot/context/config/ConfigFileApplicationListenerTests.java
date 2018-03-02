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

package org.springframework.boot.context.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.testsupport.rule.OutputCapture;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigFileApplicationListener}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Eddú Meléndez
 */
public class ConfigFileApplicationListenerTests {

	private final StandardEnvironment environment = new StandardEnvironment();

	private final SpringApplication application = new SpringApplication();

	private final ConfigFileApplicationListener initializer = new ConfigFileApplicationListener();

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Rule
	public OutputCapture out = new OutputCapture();

	private ConfigurableApplicationContext context;

	@After
	public void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
		System.clearProperty("the.property");
		System.clearProperty("spring.config.location");
	}

	@Test
	public void loadCustomResource() {
		this.application.setResourceLoader(new ResourceLoader() {
			@Override
			public Resource getResource(String location) {
				if (location.equals("classpath:/custom.properties")) {
					return new ByteArrayResource("the.property: fromcustom".getBytes(),
							location) {
						@Override
						public String getFilename() {
							return location;
						}
					};
				}
				return null;
			}

			@Override
			public ClassLoader getClassLoader() {
				return getClass().getClassLoader();
			}
		});
		this.initializer.setSearchNames("custom");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("fromcustom");
	}

	@Test
	public void loadPropertiesFile() {
		this.initializer.setSearchNames("testproperties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	public void loadDefaultPropertiesFile() {
		this.environment.setDefaultProfiles("thedefault");
		this.initializer.setSearchNames("testprofiles");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("fromdefaultpropertiesfile");
	}

	@Test
	public void loadTwoPropertiesFile() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location="
						+ "classpath:application.properties,classpath:testproperties.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	public void loadTwoPropertiesFilesWithProfiles() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=classpath:enableprofile.properties,"
						+ "classpath:enableother.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("other");
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromenableotherpropertiesfile");
	}

	@Test
	public void loadTwoPropertiesFilesWithProfilesUsingAdditionalLocation() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.additional-location=classpath:enableprofile.properties,"
						+ "classpath:enableother.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("other");
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromotherpropertiesfile");
	}

	@Test
	public void loadTwoPropertiesFilesWithProfilesAndSwitchOneOff() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=classpath:enabletwoprofiles.properties,"
						+ "classpath:enableprofile.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("myprofile");
		String property = this.environment.getProperty("the.property");
		// The value from the second file wins (no profile-specific configuration is
		// actually loaded)
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	public void loadTwoPropertiesFilesWithProfilesAndSwitchOneOffFromSpecificLocation() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.name=enabletwoprofiles",
				"spring.config.location=classpath:enableprofile.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("myprofile");
		String property = this.environment.getProperty("the.property");
		// The value from the second file wins (no profile-specific configuration is
		// actually loaded)
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	public void localFileTakesPrecedenceOverClasspath() throws Exception {
		File localFile = new File(new File("."), "application.properties");
		assertThat(localFile.exists()).isFalse();
		try {
			Properties properties = new Properties();
			properties.put("the.property", "fromlocalfile");
			try (OutputStream outputStream = new FileOutputStream(localFile)) {
				properties.store(outputStream, "");
			}
			this.initializer.postProcessEnvironment(this.environment, this.application);
			String property = this.environment.getProperty("the.property");
			assertThat(property).isEqualTo("fromlocalfile");
		}
		finally {
			localFile.delete();
		}
	}

	@Test
	public void moreSpecificLocationTakesPrecedenceOverRoot() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.name=specific");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("specific");
	}

	@Test
	public void loadTwoOfThreePropertiesFile() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=classpath:application.properties,"
						+ "classpath:testproperties.properties,"
						+ "classpath:nonexistent.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	public void randomValue() {
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("random.value");
		assertThat(property).isNotNull();
	}

	@Test
	public void loadTwoPropertiesFiles() {
		this.initializer.setSearchNames("moreproperties,testproperties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		// The search order has highest precedence last (like merging a map)
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	public void loadYamlFile() {
		this.initializer.setSearchNames("testyaml");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromyamlfile");
		assertThat(this.environment.getProperty("my.array[0]")).isEqualTo("1");
		assertThat(this.environment.getProperty("my.array")).isNull();
	}

	@Test
	public void loadProfileEmptySameAsNotSpecified() {
		this.initializer.setSearchNames("testprofilesempty");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromemptyprofile");
	}

	@Test
	public void loadDefaultYamlDocument() {
		this.environment.setDefaultProfiles("thedefault");
		this.initializer.setSearchNames("testprofilesdocument");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromdefaultprofile");
	}

	@Test
	public void loadDefaultYamlDocumentNotActivated() {
		this.environment.setDefaultProfiles("thedefault");
		this.environment.setActiveProfiles("other");
		this.initializer.setSearchNames("testprofilesdocument");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromotherprofile");
	}

	@Test
	public void commandLineWins() {
		this.environment.getPropertySources().addFirst(
				new SimpleCommandLinePropertySource("--the.property=fromcommandline"));
		this.initializer.setSearchNames("testproperties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("fromcommandline");
	}

	@Test
	public void systemPropertyWins() {
		System.setProperty("the.property", "fromsystem");
		this.initializer.setSearchNames("testproperties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("fromsystem");
	}

	@Test
	public void defaultPropertyAsFallback() {
		this.environment.getPropertySources()
				.addLast(new MapPropertySource("defaultProperties",
						Collections.singletonMap("my.fallback", (Object) "foo")));
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.fallback");
		assertThat(property).isEqualTo("foo");
	}

	@Test
	public void defaultPropertyAsFallbackDuringFileParsing() {
		this.environment.getPropertySources()
				.addLast(new MapPropertySource("defaultProperties", Collections
						.singletonMap("spring.config.name", (Object) "testproperties")));
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	public void loadPropertiesThenProfilePropertiesActivatedInSpringApplication() {
		// This should be the effect of calling
		// SpringApplication.setAdditionalProfiles("other")
		this.environment.setActiveProfiles("other");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		// The "other" profile is activated in SpringApplication so it should take
		// precedence over the default profile
		assertThat(property).isEqualTo("fromotherpropertiesfile");
	}

	@Test
	public void twoProfilesFromProperties() {
		// This should be the effect of calling
		// SpringApplication.setAdditionalProfiles("other", "dev")
		this.environment.setActiveProfiles("other", "dev");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		// The "dev" profile is activated in SpringApplication so it should take
		// precedence over the default profile
		assertThat(property).isEqualTo("fromdevpropertiesfile");
	}

	@Test
	public void loadPropertiesThenProfilePropertiesActivatedInFirst() {
		this.initializer.setSearchNames("enableprofile");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		// The "myprofile" profile is activated in enableprofile.properties so its value
		// should show up here
		assertThat(property).isEqualTo("fromprofilepropertiesfile");
	}

	@Test
	public void loadPropertiesThenProfilePropertiesWithOverride() {
		this.environment.setActiveProfiles("other");
		this.initializer.setSearchNames("enableprofile");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("other.property");
		// The "other" profile is activated before any processing starts
		assertThat(property).isEqualTo("fromotherpropertiesfile");
		property = this.environment.getProperty("the.property");
		// The "myprofile" profile is activated in enableprofile.properties and "other"
		// was not activated by setting spring.profiles.active so "myprofile" should still
		// be activated
		assertThat(property).isEqualTo("fromprofilepropertiesfile");
	}

	@Test
	public void profilePropertiesUsedInPlaceholders() {
		this.initializer.setSearchNames("enableprofile");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("one.more");
		assertThat(property).isEqualTo("fromprofilepropertiesfile");
	}

	@Test
	public void profilesAddedToEnvironmentAndViaProperty() {
		// External profile takes precedence over profile added via the environment
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.profiles.active=other");
		this.environment.addActiveProfile("dev");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).contains("dev", "other");
		assertThat(this.environment.getProperty("my.property"))
				.isEqualTo("fromotherpropertiesfile");
		validateProfilePrecedence(null, "dev", "other");
	}

	@Test
	public void profilesAddedToEnvironmentAndViaPropertyDuplicate() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.profiles.active=dev,other");
		this.environment.addActiveProfile("dev");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).contains("dev", "other");
		assertThat(this.environment.getProperty("my.property"))
				.isEqualTo("fromotherpropertiesfile");
		validateProfilePrecedence(null, "dev", "other");
	}

	@Test
	public void profilesAddedToEnvironmentAndViaPropertyDuplicateEnvironmentWins() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.profiles.active=other,dev");
		this.environment.addActiveProfile("other");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).contains("dev", "other");
		assertThat(this.environment.getProperty("my.property"))
				.isEqualTo("fromdevpropertiesfile");
		validateProfilePrecedence(null, "other", "dev");
	}

	@Test
	public void postProcessorsAreOrderedCorrectly() {
		TestConfigFileApplicationListener testListener = new TestConfigFileApplicationListener();
		testListener.onApplicationEvent(new ApplicationEnvironmentPreparedEvent(
				this.application, new String[0], this.environment));
	}

	private void validateProfilePrecedence(String... profiles) {
		ApplicationPreparedEvent event = new ApplicationPreparedEvent(
				new SpringApplication(), new String[0],
				new AnnotationConfigApplicationContext());
		withDebugLogging(() -> this.initializer.onApplicationEvent(event));
		String log = this.out.toString();

		// First make sure that each profile got processed only once
		for (String profile : profiles) {
			String reason = "Wrong number of occurrences for profile '" + profile
					+ "' --> " + log;
			assertThat(StringUtils.countOccurrencesOf(log, createLogForProfile(profile)))
					.as(reason).isEqualTo(1);
		}
		// Make sure the order of loading is the right one
		for (String profile : profiles) {
			String line = createLogForProfile(profile);
			int index = log.indexOf(line);
			assertThat(index)
					.as("Loading profile '" + profile + "' not found in '" + log + "'")
					.isNotEqualTo(-1);
			log = log.substring(index + line.length());
		}
	}

	private void withDebugLogging(Runnable runnable) {
		LoggerContext loggingContext = (LoggerContext) LogManager.getContext(false);
		org.apache.logging.log4j.core.config.Configuration configuration = loggingContext
				.getConfiguration();
		configuration.addLogger(ConfigFileApplicationListener.class.getName(),
				new LoggerConfig(ConfigFileApplicationListener.class.getName(),
						Level.DEBUG, true));
		loggingContext.updateLoggers();
		try {
			runnable.run();
		}
		finally {
			configuration.removeLogger(ConfigFileApplicationListener.class.getName());
			loggingContext.updateLoggers();
		}
	}

	private String createLogForProfile(String profile) {
		String suffix = profile != null ? "-" + profile : "";
		String string = ".properties)";
		return "Loaded config file '"
				+ new File("target/test-classes/application" + suffix + ".properties")
						.getAbsoluteFile().toURI().toString()
				+ "' (classpath:/application" + suffix + string;
	}

	@Test
	public void yamlProfiles() {
		this.initializer.setSearchNames("testprofiles");
		this.environment.setActiveProfiles("dev");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromdevprofile");
		property = this.environment.getProperty("my.other");
		assertThat(property).isEqualTo("notempty");
	}

	@Test
	public void yamlTwoProfiles() {
		this.initializer.setSearchNames("testprofiles");
		this.environment.setActiveProfiles("other", "dev");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromdevprofile");
		property = this.environment.getProperty("my.other");
		assertThat(property).isEqualTo("notempty");
	}

	@Test
	public void yamlNegatedProfiles() {
		// gh-8011
		this.initializer.setSearchNames("testnegatedprofiles");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromnototherprofile");
		property = this.environment.getProperty("my.notother");
		assertThat(property).isEqualTo("foo");
	}

	@Test
	public void yamlNegatedProfilesWithProfile() {
		// gh-8011
		this.initializer.setSearchNames("testnegatedprofiles");
		this.environment.setActiveProfiles("other");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromotherprofile");
		property = this.environment.getProperty("my.notother");
		assertThat(property).isNull();
	}

	@Test
	public void yamlSetsProfiles() {
		this.initializer.setSearchNames("testsetprofiles");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("dev");
		String property = this.environment.getProperty("my.property");
		assertThat(this.environment.getActiveProfiles()).contains("dev");
		assertThat(property).isEqualTo("fromdevprofile");
		List<String> names = StreamSupport
				.stream(this.environment.getPropertySources().spliterator(), false)
				.map(org.springframework.core.env.PropertySource::getName)
				.collect(Collectors.toList());
		assertThat(names).contains(
				"applicationConfig: [classpath:/testsetprofiles.yml] (document #0)",
				"applicationConfig: [classpath:/testsetprofiles.yml] (document #1)");
	}

	@Test
	public void yamlSetsMultiProfiles() {
		this.initializer.setSearchNames("testsetmultiprofiles");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("dev",
				"healthcheck");
	}

	@Test
	public void yamlSetsMultiProfilesWhenListProvided() {
		this.initializer.setSearchNames("testsetmultiprofileslist");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("dev",
				"healthcheck");
	}

	@Test
	public void yamlSetsMultiProfilesWithWhitespace() {
		this.initializer.setSearchNames("testsetmultiprofileswhitespace");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("dev",
				"healthcheck");
	}

	@Test
	public void yamlProfileCanBeChanged() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.profiles.active=prod");
		this.initializer.setSearchNames("testsetprofiles");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("prod");
	}

	@Test
	public void specificNameAndProfileFromExistingSource() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.profiles.active=specificprofile",
				"spring.config.name=specificfile");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromspecificpropertiesfile");
	}

	@Test
	public void specificResource() {
		String location = "classpath:specificlocation.properties";
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=" + location);
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("fromspecificlocation");
		assertThat(this.environment).has(matchingPropertySource(
				"applicationConfig: " + "[classpath:specificlocation.properties]"));
		// The default property source is not there
		assertThat(this.environment).doesNotHave(matchingPropertySource(
				"applicationConfig: " + "[classpath:/application.properties]"));
		assertThat(this.environment.getProperty("foo")).isNull();
	}

	@Test
	public void specificResourceFromAdditionalLocation() {
		String additionalLocation = "classpath:specificlocation.properties";
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.additional-location=" + additionalLocation);
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("fromspecificlocation");
		assertThat(this.environment).has(matchingPropertySource(
				"applicationConfig: " + "[classpath:specificlocation.properties]"));
		// The default property source is still there
		assertThat(this.environment).has(matchingPropertySource(
				"applicationConfig: " + "[classpath:/application.properties]"));
		assertThat(this.environment.getProperty("foo")).isEqualTo("bucket");
	}

	@Test
	public void specificResourceAsFile() {
		String location = "file:src/test/resources/specificlocation.properties";
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=" + location);
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment)
				.has(matchingPropertySource("applicationConfig: [" + location + "]"));
	}

	@Test
	public void specificResourceDefaultsToFile() {
		String location = "src/test/resources/specificlocation.properties";
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=" + location);
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment).has(
				matchingPropertySource("applicationConfig: [file:" + location + "]"));
	}

	@Test
	public void absoluteResourceDefaultsToFile() {
		String location = new File("src/test/resources/specificlocation.properties")
				.getAbsolutePath().replace("\\", "/");
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=" + location);
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment)
				.has(matchingPropertySource("applicationConfig: [file:"
						+ location.replace(File.separatorChar, '/') + "]"));
	}

	@Test
	public void propertySourceAnnotation() {
		SpringApplication application = new SpringApplication(WithPropertySource.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("fromspecificlocation");
		property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromapplicationproperties");
		assertThat(context.getEnvironment()).has(matchingPropertySource(
				"class path resource " + "[specificlocation.properties]"));
		context.close();
	}

	@Test
	public void propertySourceAnnotationWithPlaceholder() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"source.location=specificlocation");
		SpringApplication application = new SpringApplication(
				WithPropertySourcePlaceholders.class);
		application.setEnvironment(this.environment);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("fromspecificlocation");
		assertThat(context.getEnvironment()).has(matchingPropertySource(
				"class path resource " + "[specificlocation.properties]"));
		context.close();
	}

	@Test
	public void propertySourceAnnotationWithName() {
		SpringApplication application = new SpringApplication(
				WithPropertySourceAndName.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("fromspecificlocation");
		assertThat(context.getEnvironment()).has(matchingPropertySource("foo"));
		context.close();
	}

	@Test
	public void propertySourceAnnotationInProfile() {
		SpringApplication application = new SpringApplication(
				WithPropertySourceInProfile.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext context = application
				.run("--spring.profiles.active=myprofile");
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
		assertThat(context.getEnvironment()).has(matchingPropertySource(
				"class path resource " + "[enableprofile.properties]"));
		assertThat(context.getEnvironment()).doesNotHave(matchingPropertySource(
				"classpath:/" + "enableprofile-myprofile.properties"));
		context.close();
	}

	@Test
	public void propertySourceAnnotationAndNonActiveProfile() {
		SpringApplication application = new SpringApplication(
				WithPropertySourceAndProfile.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromapplicationproperties");
		assertThat(context.getEnvironment()).doesNotHave(matchingPropertySource(
				"classpath:" + "/enableprofile-myprofile.properties"));
		context.close();
	}

	@Test
	public void propertySourceAnnotationMultipleLocations() {
		SpringApplication application = new SpringApplication(
				WithPropertySourceMultipleLocations.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("frommorepropertiesfile");
		assertThat(context.getEnvironment()).has(matchingPropertySource(
				"class path resource " + "[specificlocation.properties]"));
		context.close();
	}

	@Test
	public void propertySourceAnnotationMultipleLocationsAndName() {
		SpringApplication application = new SpringApplication(
				WithPropertySourceMultipleLocationsAndName.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("frommorepropertiesfile");
		assertThat(context.getEnvironment()).has(matchingPropertySource("foo"));
		context.close();
	}

	@Test
	public void activateProfileFromProfileSpecificProperties() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.profiles.active=includeprofile");
		ConfigurableEnvironment environment = this.context.getEnvironment();
		assertThat(environment).has(matchingProfile("includeprofile"));
		assertThat(environment).has(matchingProfile("specific"));
		assertThat(environment).has(matchingProfile("morespecific"));
		assertThat(environment).has(matchingProfile("yetmorespecific"));
		assertThat(environment).doesNotHave(matchingProfile("missing"));
	}

	@Test
	public void profileSubDocumentInSameProfileSpecificFile() {
		// gh-340
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application
				.run("--spring.profiles.active=activeprofilewithsubdoc");
		String property = this.context.getEnvironment().getProperty("foobar");
		assertThat(property).isEqualTo("baz");
	}

	@Test
	public void profileSubDocumentInDifferentProfileSpecificFile() {
		// gh-4132
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run(
				"--spring.profiles.active=activeprofilewithdifferentsubdoc,activeprofilewithdifferentsubdoc2");
		String property = this.context.getEnvironment().getProperty("foobar");
		assertThat(property).isEqualTo("baz");
	}

	@Test
	public void addBeforeDefaultProperties() {
		MapPropertySource defaultSource = new MapPropertySource("defaultProperties",
				Collections.singletonMap("the.property", "fromdefaultproperties"));
		this.environment.getPropertySources().addFirst(defaultSource);
		this.initializer.setSearchNames("testproperties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	public void customDefaultProfile() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.profiles.default=customdefault");
		String property = this.context.getEnvironment().getProperty("customdefault");
		assertThat(property).isEqualTo("true");
	}

	@Test
	public void customDefaultProfileAndActive() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.profiles.default=customdefault",
				"--spring.profiles.active=dev");
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromdevpropertiesfile");
		assertThat(this.context.getEnvironment().containsProperty("customdefault"))
				.isFalse();
	}

	@Test
	public void customDefaultProfileAndActiveFromFile() {
		// gh-5998
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.config.name=customprofile",
				"--spring.profiles.default=customdefault");
		ConfigurableEnvironment environment = this.context.getEnvironment();
		assertThat(environment.containsProperty("customprofile")).isTrue();
		assertThat(environment.containsProperty("customprofile-specific")).isTrue();
		assertThat(environment.containsProperty("customprofile-customdefault")).isTrue();
		assertThat(environment.acceptsProfiles("customdefault")).isTrue();
	}

	@Test
	public void additionalProfilesCanBeIncludedFromAnyPropertySource() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.profiles.active=myprofile",
				"--spring.profiles.include=dev");
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromdevpropertiesfile");
		assertThat(this.context.getEnvironment().containsProperty("customdefault"))
				.isFalse();
	}

	@Test
	public void profileCanBeIncludedWithoutAnyBeingActive() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.profiles.include=dev");
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromdevpropertiesfile");
	}

	@Test
	public void activeProfilesCanBeConfiguredUsingPlaceholdersResolvedAgainstTheEnvironment() {
		Map<String, Object> source = new HashMap<>();
		source.put("activeProfile", "testPropertySource");
		org.springframework.core.env.PropertySource<?> propertySource = new MapPropertySource(
				"test", source);
		this.environment.getPropertySources().addLast(propertySource);
		this.initializer.setSearchNames("testactiveprofiles");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles())
				.containsExactly("testPropertySource");
	}

	@Test
	public void additionalLocationTakesPrecedenceOverDefaultLocation() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.additional-location=classpath:override.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("foo")).isEqualTo("bar");
		assertThat(this.environment.getProperty("value")).isEqualTo("1234");
	}

	@Test
	public void lastAdditionalLocationWins() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.additional-location=classpath:override.properties,"
						+ "classpath:some.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("foo")).isEqualTo("spam");
		assertThat(this.environment.getProperty("value")).isEqualTo("1234");
	}

	@Test
	public void locationReplaceDefaultLocation() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=classpath:override.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("foo")).isEqualTo("bar");
		assertThat(this.environment.getProperty("value")).isNull();
	}

	private Condition<ConfigurableEnvironment> matchingPropertySource(
			final String sourceName) {
		return new Condition<ConfigurableEnvironment>(
				"environment containing property source " + sourceName) {

			@Override
			public boolean matches(ConfigurableEnvironment value) {
				return value.getPropertySources().contains(sourceName);
			}

		};
	}

	private Condition<ConfigurableEnvironment> matchingProfile(String profile) {
		return new Condition<ConfigurableEnvironment>("accepts profile " + profile) {

			@Override
			public boolean matches(ConfigurableEnvironment value) {
				return value.acceptsProfiles(profile);
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

	private static class TestConfigFileApplicationListener
			extends ConfigFileApplicationListener {

		@Override
		List<EnvironmentPostProcessor> loadPostProcessors() {
			return new ArrayList<>(
					Arrays.asList(new LowestPrecedenceEnvironmentPostProcessor()));
		}

	}

	@Order(Ordered.LOWEST_PRECEDENCE)
	private static class LowestPrecedenceEnvironmentPostProcessor
			implements EnvironmentPostProcessor {

		@Override
		public void postProcessEnvironment(ConfigurableEnvironment environment,
				SpringApplication application) {
			assertThat(environment.getPropertySources()).hasSize(4);
		}

	}

}
