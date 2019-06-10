/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;
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
@ExtendWith(OutputCaptureExtension.class)
class ConfigFileApplicationListenerTests {

	private final BuildOutput buildOutput = new BuildOutput(getClass());

	private final StandardEnvironment environment = new StandardEnvironment();

	private final SpringApplication application = new SpringApplication();

	private final ConfigFileApplicationListener initializer = new ConfigFileApplicationListener();

	private ConfigurableApplicationContext context;

	@AfterEach
	public void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
		System.clearProperty("the.property");
		System.clearProperty("spring.config.location");
	}

	@Test
	void loadCustomResource() {
		this.application.setResourceLoader(new ResourceLoader() {
			@Override
			public Resource getResource(String location) {
				if (location.equals("classpath:/custom.properties")) {
					return new ByteArrayResource("the.property: fromcustom".getBytes(), location) {
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
	void loadPropertiesFile() {
		this.initializer.setSearchNames("testproperties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	void loadDefaultPropertiesFile() {
		this.environment.setDefaultProfiles("thedefault");
		this.initializer.setSearchNames("testprofiles");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("fromdefaultpropertiesfile");
	}

	@Test
	void loadTwoPropertiesFile() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=" + "classpath:application.properties,classpath:testproperties.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	void loadTwoPropertiesFilesWithProfiles() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=classpath:enableprofile.properties," + "classpath:enableother.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("other");
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromenableotherpropertiesfile");
	}

	@Test
	void loadTwoPropertiesFilesWithProfilesUsingAdditionalLocation() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.additional-location=classpath:enableprofile.properties,"
						+ "classpath:enableother.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("other");
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromotherpropertiesfile");
	}

	@Test
	void loadTwoPropertiesFilesWithProfilesAndSwitchOneOff() {
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
	void loadTwoPropertiesFilesWithProfilesAndSwitchOneOffFromSpecificLocation() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.name=enabletwoprofiles", "spring.config.location=classpath:enableprofile.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("myprofile");
		String property = this.environment.getProperty("the.property");
		// The value from the second file wins (no profile-specific configuration is
		// actually loaded)
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	void localFileTakesPrecedenceOverClasspath() throws Exception {
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
	void moreSpecificLocationTakesPrecedenceOverRoot() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, "spring.config.name=specific");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("specific");
	}

	@Test
	void loadTwoOfThreePropertiesFile() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=classpath:application.properties," + "classpath:testproperties.properties,"
						+ "classpath:nonexistent.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	void randomValue() {
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("random.value");
		assertThat(property).isNotNull();
	}

	@Test
	void loadTwoPropertiesFiles() {
		this.initializer.setSearchNames("moreproperties,testproperties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		// The search order has highest precedence last (like merging a map)
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	void loadYamlFile() {
		this.initializer.setSearchNames("testyaml");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromyamlfile");
		assertThat(this.environment.getProperty("my.array[0]")).isEqualTo("1");
		assertThat(this.environment.getProperty("my.array")).isNull();
	}

	@Test
	void loadProfileEmptySameAsNotSpecified() {
		this.initializer.setSearchNames("testprofilesempty");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromemptyprofile");
	}

	@Test
	void loadDefaultYamlDocument() {
		this.environment.setDefaultProfiles("thedefault");
		this.initializer.setSearchNames("testprofilesdocument");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromdefaultprofile");
	}

	@Test
	void loadDefaultYamlDocumentNotActivated() {
		this.environment.setDefaultProfiles("thedefault");
		this.environment.setActiveProfiles("other");
		this.initializer.setSearchNames("testprofilesdocument");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromotherprofile");
	}

	@Test
	void commandLineWins() {
		this.environment.getPropertySources()
				.addFirst(new SimpleCommandLinePropertySource("--the.property=fromcommandline"));
		this.initializer.setSearchNames("testproperties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("fromcommandline");
	}

	@Test
	void systemPropertyWins() {
		System.setProperty("the.property", "fromsystem");
		this.initializer.setSearchNames("testproperties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("fromsystem");
	}

	@Test
	void defaultPropertyAsFallback() {
		this.environment.getPropertySources().addLast(
				new MapPropertySource("defaultProperties", Collections.singletonMap("my.fallback", (Object) "foo")));
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.fallback");
		assertThat(property).isEqualTo("foo");
	}

	@Test
	void defaultPropertyAsFallbackDuringFileParsing() {
		this.environment.getPropertySources().addLast(new MapPropertySource("defaultProperties",
				Collections.singletonMap("spring.config.name", (Object) "testproperties")));
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	void activeProfilesFromDefaultPropertiesShouldNotTakePrecedence() {
		this.initializer.setSearchNames("enableprofile");
		this.environment.getPropertySources().addLast(
				new MapPropertySource("defaultProperties", Collections.singletonMap("spring.profiles.active", "dev")));
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("myprofile");
	}

	@Test
	void includedProfilesFromDefaultPropertiesShouldNotTakePrecedence() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.profiles.active=morespecific");
		this.environment.getPropertySources().addLast(
				new MapPropertySource("defaultProperties", Collections.singletonMap("spring.profiles.include", "dev")));
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("dev", "morespecific", "yetmorespecific");
	}

	@Test
	void activeAndIncludedProfilesFromDefaultProperties() {
		Map<String, Object> source = new HashMap<>();
		source.put("spring.profiles.include", "other");
		source.put("spring.profiles.active", "dev");
		this.environment.getPropertySources().addLast(new MapPropertySource("defaultProperties", source));
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("other", "dev");
	}

	@Test
	void activeFromDefaultPropertiesShouldNotApplyIfProfilesHaveBeenActivatedBefore() {
		Map<String, Object> source = new HashMap<>();
		source.put("spring.profiles.active", "dev");
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, "spring.profiles.active=other");
		this.environment.getPropertySources().addLast(new MapPropertySource("defaultProperties", source));
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("other");
	}

	@Test
	void loadPropertiesThenProfilePropertiesActivatedInSpringApplication() {
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
	void twoProfilesFromProperties() {
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
	void loadPropertiesThenProfilePropertiesActivatedInFirst() {
		this.initializer.setSearchNames("enableprofile");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		// The "myprofile" profile is activated in enableprofile.properties so its value
		// should show up here
		assertThat(property).isEqualTo("fromprofilepropertiesfile");
	}

	@Test
	void loadPropertiesThenProfilePropertiesWithOverride() {
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
	void profilePropertiesUsedInPlaceholders() {
		this.initializer.setSearchNames("enableprofile");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("one.more");
		assertThat(property).isEqualTo("fromprofilepropertiesfile");
	}

	@Test
	void profilesAddedToEnvironmentAndViaProperty(CapturedOutput capturedOutput) {
		// External profile takes precedence over profile added via the environment
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, "spring.profiles.active=other");
		this.environment.addActiveProfile("dev");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).contains("dev", "other");
		assertThat(this.environment.getProperty("my.property")).isEqualTo("fromotherpropertiesfile");
		validateProfilePreference(capturedOutput, null, "dev", "other");
	}

	@Test
	void profilesAddedToEnvironmentViaActiveAndIncludeProperty(CapturedOutput capturedOutput) {
		// Active profile property takes precedence
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, "spring.profiles.active=dev",
				"spring.profiles.include=other");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("other", "dev");
		assertThat(this.environment.getProperty("my.property")).isEqualTo("fromdevpropertiesfile");
		validateProfilePreference(capturedOutput, null, "other", "dev");
	}

	@Test
	void profilesAddedViaIncludePropertyAndActivatedViaAnotherPropertySource(CapturedOutput capturedOutput) {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.profiles.include=dev,simple");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("dev", "simple", "other");
		validateProfilePreference(capturedOutput, "dev", "simple", "other");
	}

	@Test
	void profilesAddedToEnvironmentAndViaPropertyDuplicate(CapturedOutput capturedOutput) {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, "spring.profiles.active=dev,other");
		this.environment.addActiveProfile("dev");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).contains("dev", "other");
		assertThat(this.environment.getProperty("my.property")).isEqualTo("fromotherpropertiesfile");
		validateProfilePreference(capturedOutput, null, "dev", "other");
	}

	@Test
	void profilesAddedToEnvironmentAndViaPropertyDuplicateEnvironmentWins(CapturedOutput capturedOutput) {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, "spring.profiles.active=other,dev");
		this.environment.addActiveProfile("other");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).contains("dev", "other");
		assertThat(this.environment.getProperty("my.property")).isEqualTo("fromdevpropertiesfile");
		validateProfilePreference(capturedOutput, null, "other", "dev");
	}

	@Test
	void postProcessorsAreOrderedCorrectly() {
		TestConfigFileApplicationListener testListener = new TestConfigFileApplicationListener();
		testListener.onApplicationEvent(
				new ApplicationEnvironmentPreparedEvent(this.application, new String[0], this.environment));
	}

	private void validateProfilePreference(CapturedOutput capturedOutput, String... profiles) {
		ApplicationPreparedEvent event = new ApplicationPreparedEvent(new SpringApplication(), new String[0],
				new AnnotationConfigApplicationContext());
		withDebugLogging(() -> this.initializer.onApplicationEvent(event));
		String log = capturedOutput.toString();

		// First make sure that each profile got processed only once
		for (String profile : profiles) {
			String reason = "Wrong number of occurrences for profile '" + profile + "' --> " + log;
			assertThat(StringUtils.countOccurrencesOf(log, createLogForProfile(profile))).as(reason).isEqualTo(1);
		}
		// Make sure the order of loading is the right one
		for (String profile : profiles) {
			String line = createLogForProfile(profile);
			int index = log.indexOf(line);
			assertThat(index).as("Loading profile '" + profile + "' not found in '" + log + "'").isNotEqualTo(-1);
			log = log.substring(index + line.length());
		}
	}

	private void withDebugLogging(Runnable runnable) {
		LoggerContext loggingContext = (LoggerContext) LogManager.getContext(false);
		org.apache.logging.log4j.core.config.Configuration configuration = loggingContext.getConfiguration();
		configuration.addLogger(ConfigFileApplicationListener.class.getName(),
				new LoggerConfig(ConfigFileApplicationListener.class.getName(), Level.DEBUG, true));
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
		String suffix = (profile != null) ? "-" + profile : "";
		String string = ".properties)";
		return "Loaded config file '"
				+ new File(this.buildOutput.getTestResourcesLocation(), "application" + suffix + ".properties")
						.getAbsoluteFile().toURI().toString()
				+ "' (classpath:/application" + suffix + string;
	}

	@Test
	void yamlProfiles() {
		this.initializer.setSearchNames("testprofiles");
		this.environment.setActiveProfiles("dev");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromdevprofile");
		property = this.environment.getProperty("my.other");
		assertThat(property).isEqualTo("notempty");
	}

	@Test
	void yamlTwoProfiles() {
		this.initializer.setSearchNames("testprofiles");
		this.environment.setActiveProfiles("other", "dev");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromdevprofile");
		property = this.environment.getProperty("my.other");
		assertThat(property).isEqualTo("notempty");
	}

	@Test
	void yamlProfileExpressionsAnd() {
		assertProfileExpression("devandother", "dev", "other");
	}

	@Test
	void yamlProfileExpressionsComplex() {
		assertProfileExpression("devorotherandanother", "dev", "another");
	}

	@Test
	void yamlProfileExpressionsNoMatch() {
		assertProfileExpression("fromyamlfile", "dev");
	}

	private void assertProfileExpression(String value, String... activeProfiles) {
		this.environment.setActiveProfiles(activeProfiles);
		this.initializer.setSearchNames("testprofileexpression");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo(value);
	}

	@Test
	void yamlNegatedProfiles() {
		// gh-8011
		this.initializer.setSearchNames("testnegatedprofiles");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromnototherprofile");
		property = this.environment.getProperty("my.notother");
		assertThat(property).isEqualTo("foo");
	}

	@Test
	void yamlNegatedProfilesWithProfile() {
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
	void yamlSetsProfiles() {
		this.initializer.setSearchNames("testsetprofiles");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("dev");
		String property = this.environment.getProperty("my.property");
		assertThat(this.environment.getActiveProfiles()).contains("dev");
		assertThat(property).isEqualTo("fromdevprofile");
		List<String> names = StreamSupport.stream(this.environment.getPropertySources().spliterator(), false)
				.map(org.springframework.core.env.PropertySource::getName).collect(Collectors.toList());
		assertThat(names).contains("applicationConfig: [classpath:/testsetprofiles.yml] (document #0)",
				"applicationConfig: [classpath:/testsetprofiles.yml] (document #1)");
	}

	@Test
	void yamlSetsMultiProfiles() {
		this.initializer.setSearchNames("testsetmultiprofiles");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("dev", "healthcheck");
	}

	@Test
	void yamlSetsMultiProfilesWhenListProvided() {
		this.initializer.setSearchNames("testsetmultiprofileslist");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("dev", "healthcheck");
	}

	@Test
	void yamlSetsMultiProfilesWithWhitespace() {
		this.initializer.setSearchNames("testsetmultiprofileswhitespace");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("dev", "healthcheck");
	}

	@Test
	void yamlProfileCanBeChanged() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, "spring.profiles.active=prod");
		this.initializer.setSearchNames("testsetprofiles");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("prod");
	}

	@Test
	void specificNameAndProfileFromExistingSource() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.profiles.active=specificprofile", "spring.config.name=specificfile");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromspecificpropertiesfile");
	}

	@Test
	void specificResource() {
		String location = "classpath:specificlocation.properties";
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=" + location);
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("fromspecificlocation");
		assertThat(this.environment)
				.has(matchingPropertySource("applicationConfig: " + "[classpath:specificlocation.properties]"));
		// The default property source is not there
		assertThat(this.environment)
				.doesNotHave(matchingPropertySource("applicationConfig: " + "[classpath:/application.properties]"));
		assertThat(this.environment.getProperty("foo")).isNull();
	}

	@Test
	void specificResourceFromAdditionalLocation() {
		String additionalLocation = "classpath:specificlocation.properties";
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.additional-location=" + additionalLocation);
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("fromspecificlocation");
		assertThat(this.environment)
				.has(matchingPropertySource("applicationConfig: " + "[classpath:specificlocation.properties]"));
		// The default property source is still there
		assertThat(this.environment)
				.has(matchingPropertySource("applicationConfig: " + "[classpath:/application.properties]"));
		assertThat(this.environment.getProperty("foo")).isEqualTo("bucket");
	}

	@Test
	void specificResourceAsFile() {
		String location = "file:src/test/resources/specificlocation.properties";
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=" + location);
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment).has(matchingPropertySource("applicationConfig: [" + location + "]"));
	}

	@Test
	void specificResourceDefaultsToFile() {
		String location = "src/test/resources/specificlocation.properties";
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=" + location);
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment).has(matchingPropertySource("applicationConfig: [file:" + location + "]"));
	}

	@Test
	void absoluteResourceDefaultsToFile() {
		String location = new File("src/test/resources/specificlocation.properties").getAbsolutePath().replace("\\",
				"/");
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=" + location);
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment).has(
				matchingPropertySource("applicationConfig: [file:" + location.replace(File.separatorChar, '/') + "]"));
	}

	@Test
	void propertySourceAnnotation() {
		SpringApplication application = new SpringApplication(WithPropertySource.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("fromspecificlocation");
		property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromapplicationproperties");
		assertThat(context.getEnvironment())
				.has(matchingPropertySource("class path resource " + "[specificlocation.properties]"));
		context.close();
	}

	@Test
	void propertySourceAnnotationWithPlaceholder() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, "source.location=specificlocation");
		SpringApplication application = new SpringApplication(WithPropertySourcePlaceholders.class);
		application.setEnvironment(this.environment);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("fromspecificlocation");
		assertThat(context.getEnvironment())
				.has(matchingPropertySource("class path resource " + "[specificlocation.properties]"));
		context.close();
	}

	@Test
	void propertySourceAnnotationWithName() {
		SpringApplication application = new SpringApplication(WithPropertySourceAndName.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("fromspecificlocation");
		assertThat(context.getEnvironment()).has(matchingPropertySource("foo"));
		context.close();
	}

	@Test
	void propertySourceAnnotationInProfile() {
		SpringApplication application = new SpringApplication(WithPropertySourceInProfile.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext context = application.run("--spring.profiles.active=myprofile");
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
		assertThat(context.getEnvironment())
				.has(matchingPropertySource("class path resource " + "[enableprofile.properties]"));
		assertThat(context.getEnvironment())
				.doesNotHave(matchingPropertySource("classpath:/" + "enableprofile-myprofile.properties"));
		context.close();
	}

	@Test
	void propertySourceAnnotationAndNonActiveProfile() {
		SpringApplication application = new SpringApplication(WithPropertySourceAndProfile.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromapplicationproperties");
		assertThat(context.getEnvironment())
				.doesNotHave(matchingPropertySource("classpath:" + "/enableprofile-myprofile.properties"));
		context.close();
	}

	@Test
	void propertySourceAnnotationMultipleLocations() {
		SpringApplication application = new SpringApplication(WithPropertySourceMultipleLocations.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("frommorepropertiesfile");
		assertThat(context.getEnvironment())
				.has(matchingPropertySource("class path resource " + "[specificlocation.properties]"));
		context.close();
	}

	@Test
	void propertySourceAnnotationMultipleLocationsAndName() {
		SpringApplication application = new SpringApplication(WithPropertySourceMultipleLocationsAndName.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("frommorepropertiesfile");
		assertThat(context.getEnvironment()).has(matchingPropertySource("foo"));
		context.close();
	}

	@Test
	void activateProfileFromProfileSpecificProperties(CapturedOutput capturedOutput) {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.profiles.active=includeprofile");
		ConfigurableEnvironment environment = this.context.getEnvironment();
		assertThat(environment).has(matchingProfile("includeprofile"));
		assertThat(environment).has(matchingProfile("specific"));
		assertThat(environment).has(matchingProfile("morespecific"));
		assertThat(environment).has(matchingProfile("yetmorespecific"));
		assertThat(environment).doesNotHave(matchingProfile("missing"));
		assertThat(capturedOutput)
				.contains("The following profiles are active: includeprofile,specific,morespecific,yetmorespecific");
	}

	@Test
	void profileSubDocumentInSameProfileSpecificFile() {
		// gh-340
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.profiles.active=activeprofilewithsubdoc");
		String property = this.context.getEnvironment().getProperty("foobar");
		assertThat(property).isEqualTo("baz");
	}

	@Test
	void profileSubDocumentInDifferentProfileSpecificFile() {
		// gh-4132
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application
				.run("--spring.profiles.active=activeprofilewithdifferentsubdoc,activeprofilewithdifferentsubdoc2");
		String property = this.context.getEnvironment().getProperty("foobar");
		assertThat(property).isEqualTo("baz");
	}

	@Test
	void addBeforeDefaultProperties() {
		MapPropertySource defaultSource = new MapPropertySource("defaultProperties",
				Collections.singletonMap("the.property", "fromdefaultproperties"));
		this.environment.getPropertySources().addFirst(defaultSource);
		this.initializer.setSearchNames("testproperties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	void customDefaultProfile() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.profiles.default=customdefault");
		String property = this.context.getEnvironment().getProperty("customdefault");
		assertThat(property).isEqualTo("true");
	}

	@Test
	void customDefaultProfileAndActive() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.profiles.default=customdefault", "--spring.profiles.active=dev");
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromdevpropertiesfile");
		assertThat(this.context.getEnvironment().containsProperty("customdefault")).isFalse();
	}

	@Test
	void customDefaultProfileAndActiveFromFile() {
		// gh-5998
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.config.name=customprofile", "--spring.profiles.default=customdefault");
		ConfigurableEnvironment environment = this.context.getEnvironment();
		assertThat(environment.containsProperty("customprofile")).isTrue();
		assertThat(environment.containsProperty("customprofile-specific")).isTrue();
		assertThat(environment.containsProperty("customprofile-customdefault")).isTrue();
		assertThat(environment.acceptsProfiles(Profiles.of("customdefault"))).isTrue();
	}

	@Test
	void additionalProfilesCanBeIncludedFromAnyPropertySource() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.profiles.active=myprofile", "--spring.profiles.include=dev");
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromdevpropertiesfile");
		assertThat(this.context.getEnvironment().containsProperty("customdefault")).isFalse();
	}

	@Test
	void profileCanBeIncludedWithoutAnyBeingActive() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.profiles.include=dev");
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromdevpropertiesfile");
	}

	@Test
	void activeProfilesCanBeConfiguredUsingPlaceholdersResolvedAgainstTheEnvironment() {
		Map<String, Object> source = new HashMap<>();
		source.put("activeProfile", "testPropertySource");
		org.springframework.core.env.PropertySource<?> propertySource = new MapPropertySource("test", source);
		this.environment.getPropertySources().addLast(propertySource);
		this.initializer.setSearchNames("testactiveprofiles");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("testPropertySource");
	}

	@Test
	void additionalLocationTakesPrecedenceOverDefaultLocation() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.additional-location=classpath:override.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("foo")).isEqualTo("bar");
		assertThat(this.environment.getProperty("value")).isEqualTo("1234");
	}

	@Test
	void lastAdditionalLocationWins() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.additional-location=classpath:override.properties," + "classpath:some.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("foo")).isEqualTo("spam");
		assertThat(this.environment.getProperty("value")).isEqualTo("1234");
	}

	@Test
	void locationReplaceDefaultLocation() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=classpath:override.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("foo")).isEqualTo("bar");
		assertThat(this.environment.getProperty("value")).isNull();
	}

	@Test
	void includeLoop() {
		// gh-13361
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.config.name=applicationloop");
		ConfigurableEnvironment environment = this.context.getEnvironment();
		assertThat(environment.acceptsProfiles(Profiles.of("loop"))).isTrue();
	}

	@Test
	void multiValueSpringProfiles() {
		// gh-13362
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.config.name=applicationmultiprofiles");
		ConfigurableEnvironment environment = this.context.getEnvironment();
		assertThat(environment.acceptsProfiles(Profiles.of("test"))).isTrue();
		assertThat(environment.acceptsProfiles(Profiles.of("another-test"))).isTrue();
		assertThat(environment.getProperty("message")).isEqualTo("multiprofile");
	}

	@Test
	void propertiesFromCustomPropertySourceLoaderShouldBeUsed() {
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("customloader1")).isEqualTo("true");
	}

	@Test
	void propertiesFromCustomPropertySourceLoaderShouldBeUsedWithSpecificResource() {
		String location = "classpath:application.custom";
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=" + location);
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("customloader1")).isEqualTo("true");
	}

	@Test
	void customDefaultPropertySourceIsNotReplaced() {
		// gh-17011
		Map<String, Object> source = new HashMap<>();
		source.put("mapkey", "mapvalue");
		MapPropertySource propertySource = new MapPropertySource("defaultProperties", source) {

			@Override
			public Object getProperty(String name) {
				if ("spring.config.name".equals(name)) {
					return "gh17001";
				}
				return super.getProperty(name);
			}

		};
		this.environment.getPropertySources().addFirst(propertySource);
		this.initializer.setSearchNames("testactiveprofiles");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("mapkey")).isEqualTo("mapvalue");
		assertThat(this.environment.getProperty("gh17001loaded")).isEqualTo("true");
	}

	private Condition<ConfigurableEnvironment> matchingPropertySource(final String sourceName) {
		return new Condition<ConfigurableEnvironment>("environment containing property source " + sourceName) {

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
				return value.acceptsProfiles(Profiles.of(profile));
			}

		};
	}

	@Configuration(proxyBeanMethods = false)
	protected static class Config {

	}

	@Configuration(proxyBeanMethods = false)
	@PropertySource("classpath:/specificlocation.properties")
	protected static class WithPropertySource {

	}

	@Configuration(proxyBeanMethods = false)
	@PropertySource("classpath:/${source.location}.properties")
	protected static class WithPropertySourcePlaceholders {

	}

	@Configuration(proxyBeanMethods = false)
	@PropertySource(value = "classpath:/specificlocation.properties", name = "foo")
	protected static class WithPropertySourceAndName {

	}

	@Configuration(proxyBeanMethods = false)
	@PropertySource("classpath:/enableprofile.properties")
	protected static class WithPropertySourceInProfile {

	}

	@Configuration(proxyBeanMethods = false)
	@PropertySource("classpath:/enableprofile-myprofile.properties")
	@Profile("myprofile")
	protected static class WithPropertySourceAndProfile {

	}

	@Configuration(proxyBeanMethods = false)
	@PropertySource({ "classpath:/specificlocation.properties", "classpath:/moreproperties.properties" })
	protected static class WithPropertySourceMultipleLocations {

	}

	@Configuration(proxyBeanMethods = false)
	@PropertySource(value = { "classpath:/specificlocation.properties", "classpath:/moreproperties.properties" },
			name = "foo")
	protected static class WithPropertySourceMultipleLocationsAndName {

	}

	private static class TestConfigFileApplicationListener extends ConfigFileApplicationListener {

		@Override
		List<EnvironmentPostProcessor> loadPostProcessors() {
			return new ArrayList<>(Collections.singletonList(new LowestPrecedenceEnvironmentPostProcessor()));
		}

	}

	@Order(Ordered.LOWEST_PRECEDENCE)
	private static class LowestPrecedenceEnvironmentPostProcessor implements EnvironmentPostProcessor {

		@Override
		public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
			assertThat(environment.getPropertySources()).hasSize(5);
		}

	}

}
