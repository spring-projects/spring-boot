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

package org.springframework.boot.context.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.LoggerFactory;

import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener.ConfigurationPropertySources;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.env.EnumerableCompositePropertySource;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.testutil.InternalOutputCapture;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.ReflectionUtils;
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
	public InternalOutputCapture out = new InternalOutputCapture();

	private ConfigurableApplicationContext context;

	@Before
	public void resetLogging() {
		LoggerContext loggerContext = ((Logger) LoggerFactory.getLogger(getClass()))
				.getLoggerContext();
		loggerContext.reset();
		new BasicConfigurator().configure(loggerContext);
	}

	@After
	public void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
		System.clearProperty("the.property");
		System.clearProperty("spring.config.location");
		System.clearProperty("spring.main.banner-mode");
		System.clearProperty(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME);
	}

	@Test
	public void loadCustomResource() throws Exception {
		this.application.setResourceLoader(new ResourceLoader() {
			@Override
			public Resource getResource(final String location) {
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
	public void loadPropertiesFile() throws Exception {
		this.initializer.setSearchNames("testproperties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	public void loadDefaultPropertiesFile() throws Exception {
		this.environment.setDefaultProfiles("thedefault");
		this.initializer.setSearchNames("testprofiles");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("fromdefaultpropertiesfile");
	}

	@Test
	public void loadTwoPropertiesFile() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location="
						+ "classpath:application.properties,classpath:testproperties.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	public void loadTwoPropertiesFilesWithProfiles() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location="
						+ "classpath:enableprofile.properties,classpath:enableother.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("other");
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromotherpropertiesfile");
	}

	@Test
	public void loadTwoPropertiesFilesWithProfilesAndSwitchOneOff() throws Exception {
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
	public void loadTwoPropertiesFilesWithProfilesAndSwitchOneOffFromSpecificLocation()
			throws Exception {
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
			OutputStream out = new FileOutputStream(localFile);
			try {
				properties.store(out, "");
			}
			finally {
				out.close();
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
	public void moreSpecificLocationTakesPrecedenceOverRoot() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.name=specific");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("specific");
	}

	@Test
	public void loadTwoOfThreePropertiesFile() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=classpath:application.properties,"
						+ "classpath:testproperties.properties,"
						+ "classpath:nonexistent.properties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	public void randomValue() throws Exception {
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("random.value");
		assertThat(property).isNotNull();
	}

	@Test
	public void loadTwoPropertiesFiles() throws Exception {
		this.initializer.setSearchNames("moreproperties,testproperties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		// The search order has highest precedence last (like merging a map)
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	public void loadYamlFile() throws Exception {
		this.initializer.setSearchNames("testyaml");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromyamlfile");
		assertThat(this.environment.getProperty("my.array[0]")).isEqualTo("1");
		assertThat(this.environment.getProperty("my.array")).isNull();
	}

	@Test
	public void loadProfileEmptySameAsNotSpecified() throws Exception {
		this.initializer.setSearchNames("testprofilesempty");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromemptyprofile");
	}

	@Test
	public void loadDefaultYamlDocument() throws Exception {
		this.environment.setDefaultProfiles("thedefault");
		this.initializer.setSearchNames("testprofilesdocument");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromdefaultprofile");
	}

	@Test
	public void loadDefaultYamlDocumentNotActivated() throws Exception {
		this.environment.setDefaultProfiles("thedefault");
		this.environment.setActiveProfiles("other");
		this.initializer.setSearchNames("testprofilesdocument");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromotherprofile");
	}

	@Test
	public void commandLineWins() throws Exception {
		this.environment.getPropertySources().addFirst(
				new SimpleCommandLinePropertySource("--the.property=fromcommandline"));
		this.initializer.setSearchNames("testproperties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("fromcommandline");
	}

	@Test
	public void systemPropertyWins() throws Exception {
		System.setProperty("the.property", "fromsystem");
		this.initializer.setSearchNames("testproperties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("fromsystem");
	}

	@Test
	public void defaultPropertyAsFallback() throws Exception {
		this.environment.getPropertySources()
				.addLast(new MapPropertySource("defaultProperties",
						Collections.singletonMap("my.fallback", (Object) "foo")));
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.fallback");
		assertThat(property).isEqualTo("foo");
	}

	@Test
	public void defaultPropertyAsFallbackDuringFileParsing() throws Exception {
		this.environment.getPropertySources()
				.addLast(new MapPropertySource("defaultProperties", Collections
						.singletonMap("spring.config.name", (Object) "testproperties")));
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	public void loadPropertiesThenProfilePropertiesActivatedInSpringApplication()
			throws Exception {
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
	public void twoProfilesFromProperties() throws Exception {
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
	public void loadPropertiesThenProfilePropertiesActivatedInFirst() throws Exception {
		this.initializer.setSearchNames("enableprofile");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		// The "myprofile" profile is activated in enableprofile.properties so its value
		// should show up here
		assertThat(property).isEqualTo("fromprofilepropertiesfile");
	}

	@Test
	public void loadPropertiesThenProfilePropertiesWithOverride() throws Exception {
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
	public void profilePropertiesUsedInPlaceholders() throws Exception {
		this.initializer.setSearchNames("enableprofile");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("one.more");
		assertThat(property).isEqualTo("fromprofilepropertiesfile");
	}

	@Test
	public void profilesAddedToEnvironmentAndViaProperty() throws Exception {
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
	public void profilesAddedToEnvironmentAndViaPropertyDuplicate() throws Exception {
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
	public void profilesAddedToEnvironmentAndViaPropertyDuplicateEnvironmentWins()
			throws Exception {
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
		this.initializer.onApplicationEvent(event);
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

	private String createLogForProfile(String profile) {
		String suffix = profile != null ? "-" + profile : "";
		String string = ".properties)";
		return "Loaded config file '"
				+ new File("target/test-classes/application" + suffix + ".properties")
						.getAbsoluteFile().toURI().toString()
				+ "' (classpath:/application" + suffix + string;
	}

	@Test
	public void yamlProfiles() throws Exception {
		this.initializer.setSearchNames("testprofiles");
		this.environment.setActiveProfiles("dev");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromdevprofile");
		property = this.environment.getProperty("my.other");
		assertThat(property).isEqualTo("notempty");
	}

	@Test
	public void yamlTwoProfiles() throws Exception {
		this.initializer.setSearchNames("testprofiles");
		this.environment.setActiveProfiles("other", "dev");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromdevprofile");
		property = this.environment.getProperty("my.other");
		assertThat(property).isEqualTo("notempty");
	}

	@Test
	public void yamlSetsProfiles() throws Exception {
		this.initializer.setSearchNames("testsetprofiles");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("dev");
		String property = this.environment.getProperty("my.property");
		assertThat(this.environment.getActiveProfiles()).contains("dev");
		assertThat(property).isEqualTo("fromdevprofile");
		ConfigurationPropertySources propertySource = (ConfigurationPropertySources) this.environment
				.getPropertySources()
				.get(ConfigFileApplicationListener.APPLICATION_CONFIGURATION_PROPERTY_SOURCE_NAME);
		Collection<org.springframework.core.env.PropertySource<?>> sources = propertySource
				.getSource();
		assertThat(sources).hasSize(2);
		List<String> names = new ArrayList<String>();
		for (org.springframework.core.env.PropertySource<?> source : sources) {
			if (source instanceof EnumerableCompositePropertySource) {
				for (org.springframework.core.env.PropertySource<?> nested : ((EnumerableCompositePropertySource) source)
						.getSource()) {
					names.add(nested.getName());
				}
			}
			else {
				names.add(source.getName());
			}
		}
		assertThat(names).contains(
				"applicationConfig: [classpath:/testsetprofiles.yml]#dev",
				"applicationConfig: [classpath:/testsetprofiles.yml]");
	}

	@Test
	public void yamlSetsMultiProfiles() throws Exception {
		this.initializer.setSearchNames("testsetmultiprofiles");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("dev",
				"healthcheck");
	}

	@Test
	public void yamlSetsMultiProfilesWithWhitespace() throws Exception {
		this.initializer.setSearchNames("testsetmultiprofileswhitespace");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("dev",
				"healthcheck");
	}

	@Test
	public void yamlProfileCanBeChanged() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.profiles.active=prod");
		this.initializer.setSearchNames("testsetprofiles");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getActiveProfiles()).containsExactly("prod");
	}

	@Test
	public void specificNameAndProfileFromExistingSource() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.profiles.active=specificprofile",
				"spring.config.name=specificfile");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromspecificpropertiesfile");
	}

	@Test
	public void specificResource() throws Exception {
		String location = "classpath:specificlocation.properties";
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=" + location);
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
	public void specificResourceAsFile() throws Exception {
		String location = "file:src/test/resources/specificlocation.properties";
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=" + location);
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment)
				.has(matchingPropertySource("applicationConfig: [" + location + "]"));
	}

	@Test
	public void specificResourceDefaultsToFile() throws Exception {
		String location = "src/test/resources/specificlocation.properties";
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.config.location=" + location);
		this.initializer.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment).has(
				matchingPropertySource("applicationConfig: [file:" + location + "]"));
	}

	@Test
	public void absoluteResourceDefaultsToFile() throws Exception {
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
	public void propertySourceAnnotation() throws Exception {
		SpringApplication application = new SpringApplication(WithPropertySource.class);
		application.setWebEnvironment(false);
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
	public void propertySourceAnnotationWithPlaceholder() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"source.location=specificlocation");
		SpringApplication application = new SpringApplication(
				WithPropertySourcePlaceholders.class);
		application.setEnvironment(this.environment);
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("fromspecificlocation");
		assertThat(context.getEnvironment()).has(matchingPropertySource(
				"class path resource " + "[specificlocation.properties]"));
		context.close();
	}

	@Test
	public void propertySourceAnnotationWithName() throws Exception {
		SpringApplication application = new SpringApplication(
				WithPropertySourceAndName.class);
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("fromspecificlocation");
		assertThat(context.getEnvironment()).has(matchingPropertySource("foo"));
		context.close();
	}

	@Test
	public void propertySourceAnnotationInProfile() throws Exception {
		SpringApplication application = new SpringApplication(
				WithPropertySourceInProfile.class);
		application.setWebEnvironment(false);
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
	public void propertySourceAnnotationAndNonActiveProfile() throws Exception {
		SpringApplication application = new SpringApplication(
				WithPropertySourceAndProfile.class);
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromapplicationproperties");
		assertThat(context.getEnvironment()).doesNotHave(matchingPropertySource(
				"classpath:" + "/enableprofile-myprofile.properties"));
		context.close();
	}

	@Test
	public void propertySourceAnnotationMultipleLocations() throws Exception {
		SpringApplication application = new SpringApplication(
				WithPropertySourceMultipleLocations.class);
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("frommorepropertiesfile");
		assertThat(context.getEnvironment()).has(matchingPropertySource(
				"class path resource " + "[specificlocation.properties]"));
		context.close();
	}

	@Test
	public void propertySourceAnnotationMultipleLocationsAndName() throws Exception {
		SpringApplication application = new SpringApplication(
				WithPropertySourceMultipleLocationsAndName.class);
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application.run();
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("frommorepropertiesfile");
		assertThat(context.getEnvironment()).has(matchingPropertySource("foo"));
		context.close();
	}

	@Test
	public void activateProfileFromProfileSpecificProperties() throws Exception {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		this.context = application.run("--spring.profiles.active=includeprofile");
		assertThat(this.context.getEnvironment()).has(matchingProfile("includeprofile"));
		assertThat(this.context.getEnvironment()).has(matchingProfile("specific"));
		assertThat(this.context.getEnvironment()).has(matchingProfile("morespecific"));
		assertThat(this.context.getEnvironment()).has(matchingProfile("yetmorespecific"));
		assertThat(this.context.getEnvironment()).doesNotHave(matchingProfile("missing"));
	}

	@Test
	public void profileSubDocumentInSameProfileSpecificFile() throws Exception {
		// gh-340
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		this.context = application
				.run("--spring.profiles.active=activeprofilewithsubdoc");
		String property = this.context.getEnvironment().getProperty("foobar");
		assertThat(property).isEqualTo("baz");
	}

	@Test
	public void bindsToSpringApplication() throws Exception {
		// gh-346
		this.initializer.setSearchNames("bindtoapplication");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		Field field = ReflectionUtils.findField(SpringApplication.class, "bannerMode");
		field.setAccessible(true);
		assertThat((Banner.Mode) field.get(this.application)).isEqualTo(Banner.Mode.OFF);
	}

	@Test
	public void bindsSystemPropertyToSpringApplication() throws Exception {
		// gh-951
		System.setProperty("spring.main.banner-mode", "off");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		Field field = ReflectionUtils.findField(SpringApplication.class, "bannerMode");
		field.setAccessible(true);
		assertThat((Banner.Mode) field.get(this.application)).isEqualTo(Banner.Mode.OFF);
	}

	@Test
	public void profileSubDocumentInDifferentProfileSpecificFile() throws Exception {
		// gh-4132
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		this.context = application.run(
				"--spring.profiles.active=activeprofilewithdifferentsubdoc,activeprofilewithdifferentsubdoc2");
		String property = this.context.getEnvironment().getProperty("foobar");
		assertThat(property).isEqualTo("baz");
	}

	@Test
	public void setIgnoreBeanInfoPropertyByDefault() throws Exception {
		this.initializer.setSearchNames("testproperties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = System
				.getProperty(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME);
		assertThat(property).isEqualTo("true");
	}

	@Test
	public void disableIgnoreBeanInfoProperty() throws Exception {
		this.initializer.setSearchNames("testproperties");
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.beaninfo.ignore=false");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = System
				.getProperty(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME);
		assertThat(property).isEqualTo("false");
	}

	@Test
	public void addBeforeDefaultProperties() throws Exception {
		MapPropertySource defaultSource = new MapPropertySource("defaultProperties",
				Collections.<String, Object>singletonMap("the.property",
						"fromdefaultproperties"));
		this.environment.getPropertySources().addFirst(defaultSource);
		this.initializer.setSearchNames("testproperties");
		this.initializer.postProcessEnvironment(this.environment, this.application);
		String property = this.environment.getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	public void customDefaultProfile() throws Exception {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		this.context = application.run("--spring.profiles.default=customdefault");
		String property = this.context.getEnvironment().getProperty("customdefault");
		assertThat(property).isEqualTo("true");
	}

	@Test
	public void customDefaultProfileAndActive() throws Exception {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		this.context = application.run("--spring.profiles.default=customdefault",
				"--spring.profiles.active=dev");
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromdevpropertiesfile");
		assertThat(this.context.getEnvironment().containsProperty("customdefault"))
				.isFalse();
	}

	@Test
	public void customDefaultProfileAndActiveFromFile() throws Exception {
		// gh-5998
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		this.context = application.run("--spring.config.name=customprofile",
				"--spring.profiles.default=customdefault");
		ConfigurableEnvironment environment = this.context.getEnvironment();
		assertThat(environment.containsProperty("customprofile")).isTrue();
		assertThat(environment.containsProperty("customprofile-specific")).isTrue();
		assertThat(environment.containsProperty("customprofile-customdefault")).isFalse();
	}

	private Condition<ConfigurableEnvironment> matchingPropertySource(
			final String sourceName) {
		return new Condition<ConfigurableEnvironment>(
				"environment containing property source " + sourceName) {

			@Override
			public boolean matches(ConfigurableEnvironment value) {
				MutablePropertySources sources = new MutablePropertySources(
						value.getPropertySources());
				ConfigurationPropertySources.finishAndRelocate(sources);
				return sources.contains(sourceName);
			}

		};
	}

	private Condition<ConfigurableEnvironment> matchingProfile(final String profile) {
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
			return new ArrayList<EnvironmentPostProcessor>(
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
