/*
 * Copyright 2012-2022 the original author or authors.
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
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.util.Strings;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.origin.Origin;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration tests for {@link ConfigDataEnvironmentPostProcessor}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ConfigDataEnvironmentPostProcessorIntegrationTests {

	private SpringApplication application;

	@TempDir
	public File temp;

	@BeforeEach
	void setup() {
		this.application = new SpringApplication(Config.class);
		this.application.setWebApplicationType(WebApplicationType.NONE);
	}

	@AfterEach
	void clearProperties() {
		System.clearProperty("the.property");
	}

	@Test
	void runWhenUsingCustomResourceLoader() {
		this.application.setResourceLoader(new ResourceLoader() {

			@Override
			public Resource getResource(String location) {
				if (location.equals("classpath:/custom.properties")) {
					return new ByteArrayResource("the.property: fromcustom".getBytes(), location);
				}
				return new ClassPathResource("doesnotexist");
			}

			@Override
			public ClassLoader getClassLoader() {
				return getClass().getClassLoader();
			}

		});
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=custom");
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("fromcustom");
	}

	@Test
	void runLoadsApplicationPropertiesOnClasspath() {
		ConfigurableApplicationContext context = this.application.run();
		String property = context.getEnvironment().getProperty("foo");
		assertThat(property).isEqualTo("bucket");
	}

	@Test
	void runLoadsApplicationYamlOnClasspath() {
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=customapplication");
		String property = context.getEnvironment().getProperty("yamlkey");
		assertThat(property).isEqualTo("yamlvalue");
	}

	@Test
	void runLoadsFileWithCustomName() {
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=testproperties");
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	void runWhenPropertiesAndYamlShouldPreferProperties() {
		ConfigurableApplicationContext context = this.application.run();
		String property = context.getEnvironment().getProperty("duplicate");
		assertThat(property).isEqualTo("properties");
	}

	@Test
	void runWhenMultipleCustomNamesLoadsEachName() {
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.name=moreproperties,testproperties");
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	void runWhenNoActiveProfilesLoadsDefaultProfileFile() {
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=testprofiles");
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromdefaultpropertiesfile");
	}

	@Test
	void runWhenActiveProfilesDoesNotLoadDefault() {
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=testprofilesdocument",
				"--spring.profiles.default=thedefault", "--spring.profiles.active=other");
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromotherprofile");
	}

	@Test
	void runWhenHasCustomDefaultProfileLoadsDefaultProfileFile() {
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=testprofiles",
				"--spring.profiles.default=thedefault");
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("fromdefaultpropertiesfile");
	}

	@Test
	void runWhenHasCustomSpringConfigLocationLoadsAllFromSpecifiedLocation() {
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.location=classpath:application.properties,classpath:testproperties.properties");
		String property1 = context.getEnvironment().getProperty("the.property");
		String property2 = context.getEnvironment().getProperty("my.property");
		String property3 = context.getEnvironment().getProperty("foo");
		assertThat(property1).isEqualTo("frompropertiesfile");
		assertThat(property2).isEqualTo("frompropertiesfile");
		assertThat(property3).isEqualTo("bucket");
	}

	@Test
	void runWhenOneCustomLocationDoesNotExistLoadsOthers() {
		ConfigurableApplicationContext context = this.application.run(
				"--spring.config.location=classpath:application.properties,classpath:testproperties.properties,optional:classpath:nonexistent.properties");
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	void runWhenProfileSpecificMandatoryLocationDoesNotExistShouldNotFail() {
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=testprofiles",
				"--spring.config.location=classpath:configdata/profiles/");
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromyamlfile");
	}

	@Test
	void runWhenProfileSpecificMandatoryLocationDoesNotExistShouldFailWhenProfileActive() {
		this.application.setAdditionalProfiles("prod");
		assertThatExceptionOfType(ConfigDataResourceNotFoundException.class).isThrownBy(() -> this.application
				.run("--spring.config.name=testprofiles", "--spring.config.location=classpath:configdata/profiles/"));
	}

	@Test
	void runWhenHasActiveProfilesFromMultipleLocationsActivatesProfileFromOneLocation() {
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.location=classpath:enableprofile.properties,classpath:enableother.properties");
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.getActiveProfiles()).containsExactly("other");
		String property = environment.getProperty("other.property");
		assertThat(property).isEqualTo("fromotherpropertiesfile");
	}

	@Test
	void runWhenHasActiveProfilesFromMultipleAdditionaLocationsWithOneSwitchedOffLoadsExpectedProperties() {
		ConfigurableApplicationContext context = this.application.run(
				"--spring.config.additional-location=classpath:enabletwoprofiles.properties,classpath:enableprofile.properties");
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.getActiveProfiles()).containsExactly("myprofile");
		String property = environment.getProperty("my.property");
		assertThat(property).isEqualTo("fromprofilepropertiesfile");
	}

	@Test
	void runWhenHaslocalFileLoadsWithLocalFileTakingPrecedenceOverClasspath() throws Exception {
		File localFile = new File(new File("."), "application.properties");
		assertThat(localFile.exists()).isFalse();
		try {
			Properties properties = new Properties();
			properties.put("my.property", "fromlocalfile");
			try (OutputStream outputStream = new FileOutputStream(localFile)) {
				properties.store(outputStream, "");
			}
			ConfigurableApplicationContext context = this.application.run();
			String property = context.getEnvironment().getProperty("my.property");
			assertThat(property).isEqualTo("fromlocalfile");
		}
		finally {
			localFile.delete();
		}
	}

	@Test
	void runWhenHasCommandLinePropertiesLoadsWithCommandLineTakingPrecedence() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources()
				.addFirst(new SimpleCommandLinePropertySource("--the.property=fromcommandline"));
		this.application.setEnvironment(environment);
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=testproperties");
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("fromcommandline");
	}

	@Test
	void runWhenHasSystemPropertyLoadsWithSystemPropertyTakingPrecedence() {
		System.setProperty("the.property", "fromsystem");
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=testproperties");
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("fromsystem");
	}

	@Test
	void runWhenHasDefaultPropertiesIncludesDefaultPropertiesLast() {
		this.application.setDefaultProperties(Collections.singletonMap("my.fallback", "foo"));
		ConfigurableApplicationContext context = this.application.run();
		String property = context.getEnvironment().getProperty("my.fallback");
		assertThat(property).isEqualTo("foo");
	}

	@Test
	void runWhenHasDefaultPropertiesWithConfigLocationConfigurationLoadsExpectedProperties() {
		this.application.setDefaultProperties(Collections.singletonMap("spring.config.name", "testproperties"));
		ConfigurableApplicationContext context = this.application.run();
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	void runWhenHasActiveProfilesFromDefaultPropertiesAndFileLoadsWithFileTakingPrecedence() {
		this.application.setDefaultProperties(Collections.singletonMap("spring.profiles.active", "dev"));
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=enableprofile");
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("myprofile");
	}

	@Test
	void runWhenProgrammaticallySetProfilesLoadsWithSetProfilesTakePrecedenceOverDefaultProfile() {
		this.application.setAdditionalProfiles("other");
		ConfigurableApplicationContext context = this.application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromotherpropertiesfile");
	}

	@Test
	void runWhenTwoProfilesSetProgrammaticallyLoadsWithPreservedProfileOrder() {
		this.application.setAdditionalProfiles("other", "dev");
		ConfigurableApplicationContext context = this.application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromdevpropertiesfile");
	}

	@Test
	void runWhenProfilesPresentBeforeConfigFileProcessingAugmentsProfileActivatedByConfigFile() {
		this.application.setAdditionalProfiles("other");
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=enableprofile");
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("other", "myprofile");
		String property = context.getEnvironment().getProperty("other.property");
		assertThat(property).isEqualTo("fromotherpropertiesfile");
		property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("fromprofilepropertiesfile");
	}

	@Test
	void runWhenProfilePropertiesUsedInPlaceholdersLoadsWithResolvedPlaceholders() {
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=enableprofile");
		String property = context.getEnvironment().getProperty("one.more");
		assertThat(property).isEqualTo("fromprofilepropertiesfile");
	}

	@Test
	void runWhenDuplicateProfileSetProgrammaticallyAndViaPropertyLoadsWithProfiles() {
		this.application.setAdditionalProfiles("dev");
		ConfigurableApplicationContext context = this.application.run("--spring.profiles.active=dev,other");
		assertThat(context.getEnvironment().getActiveProfiles()).contains("dev", "other");
		assertThat(context.getEnvironment().getProperty("my.property")).isEqualTo("fromotherpropertiesfile");
	}

	@Test
	void runWhenProfilesActivatedViaBracketNotationSetsProfiles() {
		ConfigurableApplicationContext context = this.application.run("--spring.profiles.active[0]=dev",
				"--spring.profiles.active[1]=other");
		assertThat(context.getEnvironment().getActiveProfiles()).contains("dev", "other");
		assertThat(context.getEnvironment().getProperty("my.property")).isEqualTo("fromotherpropertiesfile");
	}

	@Test
	void loadWhenProfileInMultiDocumentFilesLoadsExpectedProperties() {
		this.application.setAdditionalProfiles("dev");
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=testprofiles",
				"--spring.config.location=classpath:configdata/profiles/");
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromdevprofile");
		property = context.getEnvironment().getProperty("my.other");
		assertThat(property).isEqualTo("notempty");
	}

	@Test
	void runWhenMultipleActiveProfilesWithMultiDocumentFilesLoadsInOrderOfDocument() {
		this.application.setAdditionalProfiles("other", "dev");
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=testprofiles",
				"--spring.config.location=classpath:configdata/profiles/");
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromotherprofile");
		property = context.getEnvironment().getProperty("my.other");
		assertThat(property).isEqualTo("notempty");
		property = context.getEnvironment().getProperty("dev.property");
		assertThat(property).isEqualTo("devproperty");
	}

	@Test
	void runWhenHasAndProfileExpressionLoadsExpectedProperties() {
		assertProfileExpression("devandother", "dev", "other");
	}

	@Test
	void runWhenHasComplexProfileExpressionsLoadsExpectedProperties() {
		assertProfileExpression("devorotherandanother", "dev", "another");
	}

	@Test
	void runWhenProfileExpressionsDoNotMatchLoadsExpectedProperties() {
		assertProfileExpression("fromyamlfile", "dev");
	}

	@Test
	void runWhenHasNegatedProfilesLoadsExpectedProperties() {
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=testnegatedprofiles",
				"--spring.config.location=classpath:configdata/profiles/");
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromnototherprofile");
		property = context.getEnvironment().getProperty("my.notother");
		assertThat(property).isEqualTo("foo");
	}

	@Test
	void runWhenHasNegatedProfilesWithProfileActiveLoadsExpectedProperties() {
		this.application.setAdditionalProfiles("other");
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=testnegatedprofiles",
				"--spring.config.location=classpath:configdata/profiles/");
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromotherprofile");
		property = context.getEnvironment().getProperty("my.notother");
		assertThat(property).isNull();
	}

	@Test
	void runWhenHasActiveProfileConfigurationInMultiDocumentFileLoadsInExpectedOrder() {
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=testsetprofiles",
				"--spring.config.location=classpath:configdata/profiles/");
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("dev");
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(context.getEnvironment().getActiveProfiles()).contains("dev");
		assertThat(property).isEqualTo("fromdevprofile");
		assertThat(context.getEnvironment().getPropertySources()).extracting("name").contains(
				"Config resource 'class path resource [configdata/profiles/testsetprofiles.yml]' via location 'classpath:configdata/profiles/' (document #0)",
				"Config resource 'class path resource [configdata/profiles/testsetprofiles.yml]' via location 'classpath:configdata/profiles/' (document #1)");
	}

	@Test
	void runWhenHasYamlWithCommaSeparatedMultipleProfilesLoadsExpectedProperties() {
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=testsetmultiprofiles");
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("dev", "healthcheck");
	}

	@Test
	void runWhenHasYamlWithListProfilesLoadsExpectedProperties() {
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=testsetmultiprofileslist");
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("dev", "healthcheck");
	}

	@Test
	void loadWhenHasWhitespaceTrims() {
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.name=testsetmultiprofileswhitespace");
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("dev", "healthcheck");
	}

	@Test
	void loadWhenHasConfigLocationAsFile() {
		String location = "file:src/test/resources/specificlocation.properties";
		ConfigurableApplicationContext context = this.application.run("--spring.config.location=" + location);
		assertThat(context.getEnvironment()).has(matchingPropertySource("Config resource 'file [" + Strings
				.join(Arrays.asList("src", "test", "resources", "specificlocation.properties"), File.separatorChar)
				+ "]' via location '" + location + "'"));
	}

	@Test
	void loadWhenHasRelativeConfigLocationUsesFileLocation() {
		String location = "src/test/resources/specificlocation.properties";
		ConfigurableApplicationContext context = this.application.run("--spring.config.location=" + location);
		assertThat(context.getEnvironment()).has(matchingPropertySource("Config resource 'file [" + Strings
				.join(Arrays.asList("src", "test", "resources", "specificlocation.properties"), File.separatorChar)
				+ "]' via location '" + location + "'"));
	}

	@Test
	void loadWhenCustomDefaultProfileAndActiveFromPreviousSourceDoesNotActivateDefault() {
		ConfigurableApplicationContext context = this.application.run("--spring.profiles.default=customdefault",
				"--spring.profiles.active=dev");
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromdevpropertiesfile");
		assertThat(context.getEnvironment().containsProperty("customdefault")).isFalse();
	}

	@Test
	void runWhenCustomDefaultProfileSameAsActiveFromFileActivatesProfile() {
		ConfigurableApplicationContext context = this.application.run(
				"--spring.config.location=classpath:configdata/profiles/", "--spring.profiles.default=customdefault",
				"--spring.config.name=customprofile");
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.containsProperty("customprofile")).isTrue();
		assertThat(environment.containsProperty("customprofile-customdefault")).isTrue();
		assertThat(environment.acceptsProfiles(Profiles.of("customdefault"))).isTrue();
	}

	@Test
	void runWhenActiveProfilesCanBeConfiguredUsingPlaceholdersResolvedAgainstTheEnvironmentLoadsExpectedProperties() {
		ConfigurableApplicationContext context = this.application.run("--activeProfile=testPropertySource",
				"--spring.config.name=testactiveprofiles");
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("testPropertySource");
	}

	@Test
	void runWhenHasAdditionalLocationLoadsWithAdditionalTakingPrecedenceOverDefaultLocation() {
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.additional-location=classpath:override.properties");
		assertThat(context.getEnvironment().getProperty("foo")).isEqualTo("bar");
		assertThat(context.getEnvironment().getProperty("value")).isEqualTo("1234");
	}

	@Test
	void runWhenMultipleAdditionalLocationsLoadsWithLastWinning() {
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.additional-location=classpath:override.properties,classpath:some.properties");
		assertThat(context.getEnvironment().getProperty("foo")).isEqualTo("spam");
		assertThat(context.getEnvironment().getProperty("value")).isEqualTo("1234");
	}

	@Test
	void runWhenAdditionalLocationAndLocationLoadsWithAdditionalTakingPrecedenceOverConfigured() {
		ConfigurableApplicationContext context = this.application.run(
				"--spring.config.location=classpath:some.properties",
				"--spring.config.additional-location=classpath:override.properties");
		assertThat(context.getEnvironment().getProperty("foo")).isEqualTo("bar");
		assertThat(context.getEnvironment().getProperty("value")).isNull();
	}

	@Test
	void runWhenPropertiesFromCustomPropertySourceLoaderShouldLoadFromCustomSource() {
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("customloader1")).isEqualTo("true");
	}

	@Test
	void runWhenCustomDefaultPropertySourceLoadsWithoutReplacingCustomSource() {
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
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(propertySource);
		this.application.setEnvironment(environment);
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("mapkey")).isEqualTo("mapvalue");
		assertThat(context.getEnvironment().getProperty("gh17001loaded")).isEqualTo("true");
	}

	@Test
	void runWhenConfigLocationHasUnknownFileExtensionFailsFast() {
		String location = "classpath:application.unknown";
		assertThatIllegalStateException().isThrownBy(() -> this.application.run("--spring.config.location=" + location))
				.withMessageContaining("Unable to load config data").withMessageContaining(location)
				.satisfies((ex) -> assertThat(ex.getCause()).hasMessageContaining("File extension is not known")
						.hasMessageContaining("it must end in '/'"));
	}

	@Test
	void runWhenConfigLocationHasOptionalMissingDirectoryContinuesToLoad() {
		String location = "optional:classpath:application.unknown/";
		this.application.run("--spring.config.location=" + location);
	}

	@Test
	void runWhenConfigLocationHasNonOptionalMissingFileDirectoryThrowsResourceNotFoundException() {
		File location = new File(this.temp, "application.unknown");
		assertThatExceptionOfType(ConfigDataLocationNotFoundException.class).isThrownBy(() -> this.application
				.run("--spring.config.location=" + StringUtils.cleanPath(location.getAbsolutePath()) + "/"));
	}

	@Test
	void runWhenConfigLocationHasNonOptionalMissingClasspathDirectoryThrowsLocationNotFoundException() {
		String location = "classpath:application.unknown/";
		assertThatExceptionOfType(ConfigDataLocationNotFoundException.class)
				.isThrownBy(() -> this.application.run("--spring.config.location=" + location));
	}

	@Test
	void runWhenConfigLocationHasNonOptionalEmptyFileDirectoryDoesNotThrowException() {
		File location = new File(this.temp, "application.empty");
		location.mkdirs();
		assertThatNoException().isThrownBy(() -> this.application
				.run("--spring.config.location=" + StringUtils.cleanPath(location.getAbsolutePath()) + "/"));
	}

	@Test
	void runWhenConfigLocationHasMandatoryDirectoryThatDoesntExistThrowsException() {
		assertThatExceptionOfType(ConfigDataLocationNotFoundException.class).isThrownBy(
				() -> this.application.run("--spring.config.location=" + StringUtils.cleanPath("invalid/")));
	}

	@Test
	void runWhenConfigLocationHasNonOptionalEmptyFileDoesNotThrowException() throws IOException {
		File location = new File(this.temp, "application.properties");
		FileCopyUtils.copy(new byte[0], location);
		assertThatNoException()
				.isThrownBy(() -> this.application.run("--spring.config.location=classpath:/application.properties,"
						+ StringUtils.cleanPath(location.getAbsolutePath())));
	}

	@Test
	void runWhenResolvedIsOptionalDoesNotThrowException() {
		ApplicationContext context = this.application.run("--spring.config.location=test:optionalresult");
		assertThat(context.getEnvironment().containsProperty("spring")).isFalse();
	}

	@Test
	@Disabled("Disabled until spring.profiles suppport is dropped")
	void runWhenUsingInvalidPropertyThrowsException() {
		assertThatExceptionOfType(InvalidConfigDataPropertyException.class).isThrownBy(
				() -> this.application.run("--spring.config.location=classpath:invalidproperty.properties"));
	}

	@Test
	void runWhenImportUsesPlaceholder() {
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.location=classpath:application-import-with-placeholder.properties");
		assertThat(context.getEnvironment().getProperty("my.value")).isEqualTo("iwasimported");
	}

	@Test
	void runWhenImportFromEarlierDocumentUsesPlaceholder() {
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.location=classpath:application-import-with-placeholder-in-document.properties");
		assertThat(context.getEnvironment().getProperty("my.value")).isEqualTo("iwasimported");
	}

	@Test // gh-26858
	void runWhenImportWithProfileVariantOrdersPropertySourcesCorrectly() {
		this.application.setAdditionalProfiles("dev");
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.location=classpath:application-import-with-profile-variant.properties");
		assertThat(context.getEnvironment().getProperty("my.value"))
				.isEqualTo("application-import-with-profile-variant-imported-dev");
	}

	@Test
	void runWhenImportWithProfileVariantAndDirectProfileImportOrdersPropertySourcesCorrectly() {
		this.application.setAdditionalProfiles("dev");
		ConfigurableApplicationContext context = this.application.run(
				"--spring.config.location=classpath:application-import-with-profile-variant-and-direct-profile-import.properties");
		assertThat(context.getEnvironment().getProperty("my.value"))
				.isEqualTo("application-import-with-profile-variant-imported-dev");
	}

	@Test
	void runWhenHasPropertyInProfileDocumentThrowsException() {
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> this.application.run(
				"--spring.config.location=classpath:application-import-with-placeholder-in-profile-document.properties"))
				.withCauseInstanceOf(InactiveConfigDataAccessException.class);
	}

	@Test // gh-29386
	void runWhenHasPropertyInEarlierProfileDocumentThrowsException() {
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> this.application.run(
				"--spring.config.location=classpath:application-import-with-placeholder-in-earlier-profile-document.properties"))
				.withCauseInstanceOf(InactiveConfigDataAccessException.class);
	}

	@Test // gh-29386
	void runWhenHasPropertyInEarlierDocumentLoads() {
		ConfigurableApplicationContext context = this.application.run(
				"--spring.config.location=classpath:application-import-with-placeholder-in-earlier-document.properties");
		assertThat(context.getEnvironment().getProperty("my.value"))
				.isEqualTo("application-import-with-placeholder-in-earlier-document-imported");
	}

	@Test
	void runWhenHasNonOptionalImportThrowsException() {
		assertThatExceptionOfType(ConfigDataResourceNotFoundException.class).isThrownBy(
				() -> this.application.run("--spring.config.location=classpath:missing-appplication.properties"));
	}

	@Test
	void runWhenHasNonOptionalImportAndIgnoreNotFoundPropertyDoesNotThrowException() {
		this.application.run("--spring.config.on-not-found=ignore",
				"--spring.config.location=classpath:missing-appplication.properties");
	}

	@Test
	void runWhenHasIncludedProfilesActivatesProfiles() {
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.location=classpath:application-include-profiles.properties");
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactlyInAnyOrder("p1", "p2", "p3", "p4",
				"p5");
	}

	@Test
	void runWhenHasIncludedProfilesWithPlaceholderActivatesProfiles() {
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.location=classpath:application-include-profiles-with-placeholder.properties");
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactlyInAnyOrder("p1", "p2", "p3", "p4",
				"p5");
	}

	@Test
	void runWhenHasIncludedProfilesWithProfileSpecificDocumentThrowsException() {
		assertThatExceptionOfType(InactiveConfigDataAccessException.class).isThrownBy(() -> this.application.run(
				"--spring.config.location=classpath:application-include-profiles-in-profile-specific-document.properties"));
	}

	@Test
	void runWhenHasIncludedProfilesWithListSyntaxWithProfileSpecificDocumentThrowsException() {
		assertThatExceptionOfType(InvalidConfigDataPropertyException.class).isThrownBy(() -> this.application.run(
				"--spring.config.name=application-include-profiles-list-in-profile-specific-file",
				"--spring.profiles.active=test"));
	}

	@Test
	void runWhenImportingIncludesParentOrigin() {
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.location=classpath:application-import-with-placeholder.properties");
		Binder binder = Binder.get(context.getEnvironment());
		List<ConfigurationProperty> properties = new ArrayList<>();
		BindHandler bindHandler = new BindHandler() {

			@Override
			public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context,
					Object result) {
				properties.add(context.getConfigurationProperty());
				return result;
			}

		};
		binder.bind("my.value", Bindable.of(String.class), bindHandler);
		assertThat(properties).hasSize(1);
		Origin origin = properties.get(0).getOrigin();
		assertThat(origin.toString()).contains("application-import-with-placeholder-imported");
		assertThat(origin.getParent().toString()).contains("application-import-with-placeholder");
	}

	@Test
	void runWhenHasWildcardLocationLoadsFromAllMatchingLocations() {
		ConfigurableApplicationContext context = this.application.run(
				"--spring.config.location=file:src/test/resources/config/*/", "--spring.config.name=testproperties");
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.getProperty("first.property")).isEqualTo("apple");
		assertThat(environment.getProperty("second.property")).isEqualTo("ball");
	}

	@Test
	void runWhenOptionalWildcardLocationDoesNotExistDoesNotThrowException() {
		assertThatNoException().isThrownBy(() -> this.application.run(
				"--spring.config.location=optional:file:src/test/resources/nonexistent/*/testproperties.properties"));
	}

	@Test
	void runWhenMandatoryWildcardLocationDoesNotExistThrowsException() {
		assertThatExceptionOfType(ConfigDataLocationNotFoundException.class).isThrownBy(() -> this.application
				.run("--spring.config.location=file:src/test/resources/nonexistent/*/testproperties.properties"));
	}

	@Test
	void runWhenMandatoryWildcardLocationHasEmptyFileDirectory() {
		assertThatNoException()
				.isThrownBy(() -> this.application.run("--spring.config.location=file:src/test/resources/config/*/"));
	}

	@Test
	void runWhenMandatoryWildcardLocationHasNoSubdirectories() {
		assertThatExceptionOfType(ConfigDataLocationNotFoundException.class).isThrownBy(
				() -> this.application.run("--spring.config.location=file:src/test/resources/config/0-empty/*/"))
				.withMessage(
						"Config data location 'file:src/test/resources/config/0-empty/*/' contains no subdirectories");
	}

	@Test
	void runWhenHasMandatoryWildcardLocationThatDoesNotExist() {
		assertThatExceptionOfType(ConfigDataLocationNotFoundException.class)
				.isThrownBy(() -> this.application.run("--spring.config.location=file:invalid/*/"));
	}

	@Test
	void runWhenHasOptionalWildcardLocationThatDoesNotExistDoesNotThrow() {
		assertThatNoException()
				.isThrownBy(() -> this.application.run("--spring.config.location=optional:file:invalid/*/"));
	}

	@Test
	void runWhenOptionalWildcardLocationHasNoSubdirectoriesDoesNotThrow() {
		assertThatNoException().isThrownBy(() -> this.application
				.run("--spring.config.location=optional:file:src/test/resources/config/0-empty/*/"));
	}

	@Test // gh-24990
	void runWhenHasProfileSpecificFileWithActiveOnProfileProperty() {
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.name=application-activate-on-profile-in-profile-specific-file");
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.getProperty("test1")).isEqualTo("test1");
		assertThat(environment.getProperty("test2")).isEqualTo("test2");
	}

	@Test // gh-26960
	void runWhenHasProfileSpecificImportWithImportImportsSecondProfileSpecificFile() {
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.name=application-profile-specific-import-with-import");
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.containsProperty("application-profile-specific-import-with-import")).isTrue();
		assertThat(environment.containsProperty("application-profile-specific-import-with-import-p1")).isTrue();
		assertThat(environment.containsProperty("application-profile-specific-import-with-import-p2")).isFalse();
		assertThat(environment.containsProperty("application-profile-specific-import-with-import-import")).isTrue();
		assertThat(environment.containsProperty("application-profile-specific-import-with-import-import-p1")).isTrue();
		assertThat(environment.containsProperty("application-profile-specific-import-with-import-import-p2")).isTrue();
	}

	@Test // gh-26960
	void runWhenHasProfileSpecificImportWithCustomImportResolvesProfileSpecific() {
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.name=application-profile-specific-import-with-custom-import");
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.containsProperty("test:boot")).isTrue();
		assertThat(environment.containsProperty("test:boot:ps")).isTrue();
	}

	@Test // gh-26593
	void runWhenHasFilesInRootAndConfigWithProfiles() {
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.name=file-in-root-and-config-with-profile", "--spring.profiles.active=p1,p2");
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.containsProperty("file-in-root-and-config-with-profile")).isTrue();
		assertThat(environment.containsProperty("file-in-root-and-config-with-profile-p1")).isTrue();
		assertThat(environment.containsProperty("file-in-root-and-config-with-profile-p2")).isTrue();
		assertThat(environment.containsProperty("config-file-in-root-and-config-with-profile")).isTrue();
		assertThat(environment.containsProperty("config-file-in-root-and-config-with-profile-p1")).isTrue();
		assertThat(environment.containsProperty("config-file-in-root-and-config-with-profile-p2")).isTrue();
		assertThat(environment.getProperty("v1")).isEqualTo("config-file-in-root-and-config-with-profile-p2");
		assertThat(environment.getProperty("v2")).isEqualTo("file-in-root-and-config-with-profile-p2");
	}

	private Condition<ConfigurableEnvironment> matchingPropertySource(final String sourceName) {
		return new Condition<ConfigurableEnvironment>("environment containing property source " + sourceName) {

			@Override
			public boolean matches(ConfigurableEnvironment value) {
				value.getPropertySources().forEach((ps) -> System.out.println(ps.getName()));
				return value.getPropertySources().contains(sourceName);
			}

		};
	}

	private void assertProfileExpression(String value, String... activeProfiles) {
		this.application.setAdditionalProfiles(activeProfiles);
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=testprofileexpression",
				"--spring.config.location=classpath:configdata/profiles/");
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo(value);
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

	static class LocationResolver implements ConfigDataLocationResolver<TestConfigDataResource> {

		@Override
		public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
			return location.hasPrefix("test:");

		}

		@Override
		public List<TestConfigDataResource> resolve(ConfigDataLocationResolverContext context,
				ConfigDataLocation location)
				throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {
			return Collections.singletonList(new TestConfigDataResource(location, false));
		}

		@Override
		public List<TestConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext context,
				ConfigDataLocation location, org.springframework.boot.context.config.Profiles profiles)
				throws ConfigDataLocationNotFoundException {
			return Collections.singletonList(new TestConfigDataResource(location, true));
		}

	}

	static class Loader implements ConfigDataLoader<TestConfigDataResource> {

		@Override
		public ConfigData load(ConfigDataLoaderContext context, TestConfigDataResource resource)
				throws IOException, ConfigDataResourceNotFoundException {
			if (resource.isOptional()) {
				return null;
			}
			Map<String, Object> map = new LinkedHashMap<>();
			if (!resource.isProfileSpecific()) {
				map.put("spring", "boot");
			}
			String suffix = (!resource.isProfileSpecific()) ? "" : ":ps";
			map.put(resource.toString() + suffix, "true");
			MapPropertySource propertySource = new MapPropertySource("loaded" + suffix, map);
			return new ConfigData(Collections.singleton(propertySource));
		}

	}

	static class TestConfigDataResource extends ConfigDataResource {

		private final ConfigDataLocation location;

		private boolean profileSpecific;

		TestConfigDataResource(ConfigDataLocation location, boolean profileSpecific) {
			super(location.toString().contains("optionalresult"));
			this.location = location;
			this.profileSpecific = profileSpecific;
		}

		boolean isProfileSpecific() {
			return this.profileSpecific;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			TestConfigDataResource other = (TestConfigDataResource) obj;
			return ObjectUtils.nullSafeEquals(this.location, other.location)
					&& this.profileSpecific == other.profileSpecific;
		}

		@Override
		public int hashCode() {
			return 0;
		}

		@Override
		public String toString() {
			return this.location.toString();
		}

	}

}
