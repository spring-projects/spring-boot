/*
 * Copyright 2012-2025 the original author or authors.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.TestApplicationEnvironment;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.testsupport.classpath.resources.WithResourceDirectory;
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
import org.springframework.util.FileSystemUtils;
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
	@WithResource(name = "application.properties", content = "foo=bucket")
	void runLoadsApplicationPropertiesOnClasspath() {
		ConfigurableApplicationContext context = this.application.run();
		String property = context.getEnvironment().getProperty("foo");
		assertThat(property).isEqualTo("bucket");
	}

	@Test
	@WithResource(name = "application.yaml", content = "yamlkey: yamlvalue")
	void runLoadsApplicationYamlOnClasspath() {
		ConfigurableApplicationContext context = this.application.run();
		String property = context.getEnvironment().getProperty("yamlkey");
		assertThat(property).isEqualTo("yamlvalue");
	}

	@Test
	@WithResource(name = "testproperties.properties", content = "the.property=frompropertiesfile")
	void runLoadsFileWithCustomName() {
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=testproperties");
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	@WithResource(name = "application.properties", content = """
			duplicate=properties
			only-properties = properties
			""")
	@WithResource(name = "application.yaml", content = """
			duplicate: yaml
			only-yaml: yaml
			""")
	void runWhenPropertiesAndYamlShouldPreferProperties() {
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("duplicate")).isEqualTo("properties");
		assertThat(context.getEnvironment().getProperty("only-properties")).isEqualTo("properties");
		assertThat(context.getEnvironment().getProperty("only-yaml")).isEqualTo("yaml");
	}

	@Test
	@WithResource(name = "moreproperties.properties", content = """
			the.property=more
			only.more=more
			""")
	@WithResource(name = "testproperties.properties", content = """
			the.property=test
			only.test=test
			""")
	void runWhenMultipleCustomNamesLoadsEachName() {
		ConfigurableApplicationContext context = this.application
			.run("--spring.config.name=moreproperties,testproperties");
		assertThat(context.getEnvironment().getProperty("the.property")).isEqualTo("test");
		assertThat(context.getEnvironment().getProperty("only.more")).isEqualTo("more");
		assertThat(context.getEnvironment().getProperty("only.test")).isEqualTo("test");
	}

	@Test
	@WithResource(name = "application-default.properties", content = "my.property=fromdefaultpropertiesfile")
	void runWhenNoActiveProfilesLoadsDefaultProfileFile() {
		ConfigurableApplicationContext context = this.application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromdefaultpropertiesfile");
	}

	@Test
	@WithResource(name = "application.yaml", content = """
			---
			my:
			  property: fromyamlfile
			other: notempty
			---
			spring.config.activate.on-profile: default
			my:
			  property: fromdefaultprofile
			---
			spring.config.activate.on-profile: other
			my:
			  property: fromotherprofile
			""")
	void runWhenActiveProfilesDoesNotLoadDefault() {
		ConfigurableApplicationContext context = this.application.run("--spring.profiles.active=other");
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromotherprofile");
	}

	@Test
	@WithResource(name = "application-thedefault.properties", content = "the.property=fromdefaultpropertiesfile")
	void runWhenHasCustomDefaultProfileLoadsDefaultProfileFile() {
		ConfigurableApplicationContext context = this.application.run("--spring.profiles.default=thedefault");
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("fromdefaultpropertiesfile");
	}

	@Test
	@WithResource(name = "application.properties", content = """
			foo=bucket
			my.property=fromapplicationproperties
			""")
	@WithResource(name = "testproperties.properties", content = """
			my.property=frompropertiesfile
			the.property=frompropertiesfile
			""")
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
	@WithResource(name = "application.properties", content = """
			foo=bucket
			my.property=fromapplicationproperties
			""")
	@WithResource(name = "testproperties.properties", content = """
			my.property=frompropertiesfile
			the.property=frompropertiesfile
			""")
	void runWhenOneCustomOptionalLocationDoesNotExistLoadsOthers() {
		ConfigurableApplicationContext context = this.application.run(
				"--spring.config.location=classpath:application.properties,classpath:testproperties.properties,optional:classpath:nonexistent.properties");
		assertThat(context.getEnvironment().getProperty("my.property")).isEqualTo("frompropertiesfile");
		assertThat(context.getEnvironment().getProperty("foo")).isEqualTo("bucket");
	}

	@Test
	@WithResource(name = "application.yml", content = """
			---
			my:
			  property: fromyamlfile
			---
			spring.config.activate.on-profile: prod
			spring.config.import: file:./non-existent.yml
			""")
	void runWhenProfileSpecificMandatoryLocationDoesNotExistShouldNotFail() {
		ConfigurableApplicationContext context = this.application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromyamlfile");
	}

	@Test
	@WithResource(name = "application.yml", content = """
			---
			my:
			  property: fromyamlfile
			---
			spring.config.activate.on-profile: prod
			spring.config.import: file:./non-existent.yml
			""")
	void runWhenProfileSpecificMandatoryLocationDoesNotExistShouldFailWhenProfileActive() {
		this.application.setAdditionalProfiles("prod");
		assertThatExceptionOfType(ConfigDataResourceNotFoundException.class).isThrownBy(() -> this.application.run())
			.withMessageContaining("non-existent.yml");
	}

	@Test
	@WithResource(name = "enableprofile.properties", content = """
			spring.profiles.active=myprofile
			my.property=frompropertiesfile
			the.property=frompropertiesfile
			one.more=${my.property}
			""")
	@WithResource(name = "enableother.properties", content = """
			spring.profiles.active=other
			my.property=fromenableotherpropertiesfile
			one.more=${my.property}
			""")
	@WithResource(name = "enableprofile-other.properties", content = "other.property=fromotherpropertiesfile")
	@WithResource(name = "enableprofile-myprofile.properties", content = """
			my.property=fromprofilepropertiesfile
			the.property=fromprofilepropertiesfile
			""")
	void runWhenHasActiveProfilesFromMultipleLocationsActivatesProfileFromOneLocation() {
		ConfigurableApplicationContext context = this.application
			.run("--spring.config.location=classpath:enableprofile.properties,classpath:enableother.properties");
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.getActiveProfiles()).containsExactly("other");
		String property = environment.getProperty("other.property");
		assertThat(property).isEqualTo("fromotherpropertiesfile");
	}

	@Test
	@WithResource(name = "enableprofile.properties", content = """
			spring.profiles.active=myprofile
			my.property=frompropertiesfile
			""")
	@WithResource(name = "enabletwoprofiles.properties", content = """
			spring.profiles.active=myprofile,another
			my.property=fromtwopropertiesfile
			""")
	@WithResource(name = "enableprofile-myprofile.properties", content = """
			my.property=frommyprofilepropertiesfile
			""")
	@WithResource(name = "enableprofile-another.properties", content = """
			my.property=fromanotherprofilepropertiesfile
			""")
	void runWhenHasActiveProfilesFromMultipleAdditionalLocationsWithOneSwitchedOffLoadsExpectedProperties() {
		ConfigurableApplicationContext context = this.application.run(
				"--spring.config.additional-location=classpath:enabletwoprofiles.properties,classpath:enableprofile.properties");
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.getActiveProfiles()).containsExactly("myprofile");
		String property = environment.getProperty("my.property");
		assertThat(property).isEqualTo("frommyprofilepropertiesfile");
	}

	@Test
	void runWhenHasLocalFileLoadsWithLocalFileTakingPrecedenceOverClasspath() throws Exception {
		File localFile = new File(new File("."), "application.properties");
		assertThat(localFile).doesNotExist();
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
	@WithResource(name = "application.properties", content = """
			my.property=frompropertiesfile
			the.property=frompropertiesfile
			""")
	void runWhenHasCommandLinePropertiesLoadsWithCommandLineTakingPrecedence() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources()
			.addFirst(new SimpleCommandLinePropertySource("--the.property=fromcommandline"));
		this.application.setEnvironment(environment);
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("the.property")).isEqualTo("fromcommandline");
		assertThat(context.getEnvironment().getProperty("my.property")).isEqualTo("frompropertiesfile");
	}

	@Test
	void runWhenHasSystemPropertyLoadsWithSystemPropertyTakingPrecedence() {
		System.setProperty("the.property", "fromsystem");
		ConfigurableApplicationContext context = this.application.run("--spring.config.name=testproperties");
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("fromsystem");
	}

	@Test
	@WithResource(name = "application.properties", content = "my.property=fromapplicationproperties")
	void runWhenHasDefaultPropertiesIncludesDefaultPropertiesLast() {
		this.application.setDefaultProperties(Map.of("my.property", "fromdefaults", "my.fallback", "fromdefaults"));
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("my.fallback")).isEqualTo("fromdefaults");
		assertThat(context.getEnvironment().getProperty("my.property")).isEqualTo("fromapplicationproperties");
	}

	@Test
	@WithResource(name = "testproperties.properties", content = "the.property=frompropertiesfile")
	void runWhenHasDefaultPropertiesWithConfigNameLoadsExpectedProperties() {
		this.application.setDefaultProperties(Collections.singletonMap("spring.config.name", "testproperties"));
		ConfigurableApplicationContext context = this.application.run();
		String property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("frompropertiesfile");
	}

	@Test
	@WithResource(name = "application.properties", content = "spring.profiles.active=myprofile")
	void runWhenHasActiveProfilesFromDefaultPropertiesAndFileLoadsWithActiveProfilesFromFileTakingPrecedence() {
		this.application.setDefaultProperties(Collections.singletonMap("spring.profiles.active", "dev"));
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("myprofile");
	}

	@Test
	@WithResource(name = "application-other.properties", content = "my.property=fromotherpropertiesfile")
	void runWhenProgrammaticallySetProfilesLoadsWithSetProfilesTakePrecedenceOverDefaultProfile() {
		this.application.setAdditionalProfiles("other");
		ConfigurableApplicationContext context = this.application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromotherpropertiesfile");
	}

	@Test
	@WithResource(name = "application-dev.properties", content = "my.property=fromdevpropertiesfile")
	@WithResource(name = "application-other.properties", content = "my.property=fromotherpropertiesfile")
	void runWhenTwoProfilesSetProgrammaticallyLoadsWithPreservedProfileOrder() {
		this.application.setAdditionalProfiles("other", "dev");
		ConfigurableApplicationContext context = this.application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromdevpropertiesfile");
	}

	@Test
	@WithResource(name = "application.properties", content = "spring.profiles.active=myprofile")
	@WithResource(name = "application-myprofile.properties", content = "the.property=fromprofilepropertiesfile")
	@WithResource(name = "application-other.properties", content = "other.property=fromotherpropertiesfile")
	void runWhenProfilesPresentBeforeConfigFileProcessingAugmentsProfileActivatedByConfigFile() {
		this.application.setAdditionalProfiles("other");
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("other", "myprofile");
		String property = context.getEnvironment().getProperty("other.property");
		assertThat(property).isEqualTo("fromotherpropertiesfile");
		property = context.getEnvironment().getProperty("the.property");
		assertThat(property).isEqualTo("fromprofilepropertiesfile");
	}

	@Test
	@WithResource(name = "application.properties", content = """
			spring.profiles.active=myprofile
			one.more=${my.property}
			""")
	@WithResource(name = "application-myprofile.properties", content = "my.property=fromprofilepropertiesfile")
	void runWhenProfilePropertiesUsedInPlaceholdersLoadsWithResolvedPlaceholders() {
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("one.more")).isEqualTo("fromprofilepropertiesfile");
	}

	@Test
	@WithResource(name = "application-dev.properties", content = "my.property=fromdevpropertiesfile")
	@WithResource(name = "application-other.properties", content = "my.property=fromotherpropertiesfile")
	void runWhenDuplicateProfileSetProgrammaticallyAndViaPropertyLoadsWithProfiles() {
		this.application.setAdditionalProfiles("dev");
		ConfigurableApplicationContext context = this.application.run("--spring.profiles.active=dev,other");
		assertThat(context.getEnvironment().getActiveProfiles()).contains("dev", "other");
		assertThat(context.getEnvironment().getProperty("my.property")).isEqualTo("fromotherpropertiesfile");
	}

	@Test
	@WithResource(name = "application-dev.properties", content = "my.property=fromdevpropertiesfile")
	@WithResource(name = "application-other.properties", content = "my.property=fromotherpropertiesfile")
	void runWhenProfilesActivatedViaBracketNotationSetsProfiles() {
		ConfigurableApplicationContext context = this.application.run("--spring.profiles.active[0]=dev",
				"--spring.profiles.active[1]=other");
		assertThat(context.getEnvironment().getActiveProfiles()).contains("dev", "other");
		assertThat(context.getEnvironment().getProperty("my.property")).isEqualTo("fromotherpropertiesfile");
	}

	@Test // gh-45387
	void runWhenProfileActivatedViaSystemEnvironmentVariableWithPrefix() {
		this.application.setEnvironmentPrefix("example.prefix");
		this.application.setEnvironment(new TestApplicationEnvironment() {

			@Override
			public Map<String, Object> getSystemEnvironment() {
				return Map.of("EXAMPLE_PREFIX_SPRING_PROFILES_ACTIVE", "other,dev");
			}

		});
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getActiveProfiles()).contains("dev", "other");
	}

	@Test
	@WithResource(name = "application.yaml", content = """
			---
			my:
			  property: fromyamlfile
			  other: notempty
			---
			spring.config.activate.on-profile: dev
			my:
			  property: fromdevprofile
			""")
	void loadWhenProfileInMultiDocumentFilesLoadsExpectedProperties() {
		this.application.setAdditionalProfiles("dev");
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("my.property")).isEqualTo("fromdevprofile");
		assertThat(context.getEnvironment().getProperty("my.other")).isEqualTo("notempty");
	}

	@Test
	@WithResource(name = "application.yaml", content = """
			---
			my:
			  property: fromyamlfile
			  other: notempty
			---
			spring.config.activate.on-profile: dev
			my:
			  property: fromdevprofile
			dev:
			  property: devproperty
			---
			spring.config.activate.on-profile: other
			my:
			  property: fromotherprofile
			""")
	void runWhenMultipleActiveProfilesWithMultiDocumentFilesLoadsInOrderOfDocument() {
		this.application.setAdditionalProfiles("other", "dev");
		ConfigurableApplicationContext context = this.application.run();
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(property).isEqualTo("fromotherprofile");
		property = context.getEnvironment().getProperty("my.other");
		assertThat(property).isEqualTo("notempty");
		property = context.getEnvironment().getProperty("dev.property");
		assertThat(property).isEqualTo("devproperty");
	}

	@Test
	@WithResource(name = "application.yaml", content = """
			---
			my:
			  property: fromyamlfile
			---
			spring.config.activate.on-profile: dev & other
			my:
			  property: devandother
			---
			spring.config.activate.on-profile: (dev | other) & another
			my:
			  property: devorotherandanother
			""")
	void runWhenHasAndProfileExpressionLoadsExpectedProperties() {
		this.application.setAdditionalProfiles("dev", "other");
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("my.property")).isEqualTo("devandother");
	}

	@Test
	@WithResource(name = "application.yaml", content = """
			---
			my:
			  property: fromyamlfile
			---
			spring.config.activate.on-profile: dev & other
			my:
			  property: devandother
			---
			spring.config.activate.on-profile: (dev | other) & another
			my:
			  property: devorotherandanother
			""")
	void runWhenHasComplexProfileExpressionsLoadsExpectedProperties() {
		this.application.setAdditionalProfiles("dev", "another");
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("my.property")).isEqualTo("devorotherandanother");
	}

	@Test
	@WithResource(name = "application.yaml", content = """
			---
			my:
			  property: fromyamlfile
			---
			spring.config.activate.on-profile: dev & other
			my:
			  property: devandother
			---
			spring.config.activate.on-profile: (dev | other) & another
			my:
			  property: devorotherandanother
			""")
	void runWhenProfileExpressionsDoNotMatchLoadsExpectedProperties() {
		this.application.setAdditionalProfiles("dev");
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("my.property")).isEqualTo("fromyamlfile");
	}

	@Test
	@WithResource(name = "application.yml", content = """
			---
			my:
			  property: fromyamlfile
			  other: notempty
			---
			spring.config.activate.on-profile: other
			my:
			  property: fromotherprofile
			---
			spring.config.activate.on-profile: "!other"
			my:
			  property: fromnototherprofile
			  notother: foo

			""")
	void runWhenHasNegatedProfilesLoadsExpectedProperties() {
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("my.property")).isEqualTo("fromnototherprofile");
		assertThat(context.getEnvironment().getProperty("my.notother")).isEqualTo("foo");
	}

	@Test
	@WithResource(name = "application.yml", content = """
			---
			my:
			  property: fromyamlfile
			  other: notempty
			---
			spring.config.activate.on-profile: other
			my:
			  property: fromotherprofile
			---
			spring.config.activate.on-profile: "!other"
			my:
			  property: fromnototherprofile
			  notother: foo

			""")
	void runWhenHasNegatedProfilesWithProfileActiveLoadsExpectedProperties() {
		this.application.setAdditionalProfiles("other");
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("my.property")).isEqualTo("fromotherprofile");
		assertThat(context.getEnvironment().getProperty("my.notother")).isNull();
	}

	@Test
	@WithResource(name = "application.yml", content = """
			---
			spring:
			  profiles:
			    active: dev
			my:
			  property: fromyamlfile
			---
			spring.config.activate.on-profile: dev
			my:
			  property: fromdevprofile
			""")
	void runWhenHasActiveProfileConfigurationInMultiDocumentFileLoadsInExpectedOrder() {
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("dev");
		String property = context.getEnvironment().getProperty("my.property");
		assertThat(context.getEnvironment().getActiveProfiles()).contains("dev");
		assertThat(property).isEqualTo("fromdevprofile");
		assertThat(context.getEnvironment().getPropertySources()).extracting("name")
			.contains(
					"Config resource 'class path resource [application.yml]' via location 'optional:classpath:/' (document #0)",
					"Config resource 'class path resource [application.yml]' via location 'optional:classpath:/' (document #1)");
	}

	@Test
	@WithResource(name = "application.yml", content = """
			---
			spring:
			  profiles:
			    active: dev,healthcheck
			""")
	void runWhenHasYamlWithCommaSeparatedMultipleProfilesLoadsExpectedProperties() {
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("dev", "healthcheck");
	}

	@Test
	@WithResource(name = "application.yml", content = """
			---
			spring:
			  profiles:
			    active:
			      - dev
			      - healthcheck
			""")
	void runWhenHasYamlWithListProfilesLoadsExpectedProperties() {
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("dev", "healthcheck");
	}

	@Test
	@WithResource(name = "application.yml", content = """
			---
			spring:
			  profiles:
			    active: dev  ,    healthcheck
			""")
	void loadWhenHasWhitespaceTrims() {
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("dev", "healthcheck");
	}

	@Test
	void loadWhenHasConfigLocationAsFile() throws IOException {
		File properties = new File(this.temp, "specificlocation.properties");
		Files.write(properties.toPath(),
				List.of("my.property=fromspecificlocation", "the.property=fromspecificlocation"));
		String location = properties.toURI().toURL().toString();
		ConfigurableApplicationContext context = this.application.run("--spring.config.location=" + location);
		assertThat(context.getEnvironment())
			.has(matchingPropertySource("Config resource 'file [" + properties + "]' via location '" + location + "'"));
	}

	@Test
	void loadWhenHasRelativeConfigLocationUsesFileLocation() throws IOException {
		File buildOutput = new BuildOutput(getClass()).getRootLocation();
		File resources = new File(buildOutput, "resources-" + UUID.randomUUID());
		try {
			resources.mkdirs();
			File properties = new File(resources, "specificlocation.properties").getAbsoluteFile();
			Files.write(properties.toPath(),
					List.of("my.property=fromspecificlocation", "the.property=fromspecificlocation"));
			Path relative = new File("").getAbsoluteFile().toPath().relativize(properties.toPath());
			ConfigurableApplicationContext context = this.application.run("--spring.config.location=" + relative);
			assertThat(context.getEnvironment()).has(matchingPropertySource(
					"Config resource 'file [" + relative + "]' via location '" + relative + "'"));
		}
		finally {
			FileSystemUtils.deleteRecursively(resources);
		}
	}

	@Test
	@WithResource(name = "application-customdefault.properties", content = "customdefault=true")
	@WithResource(name = "application-dev.properties", content = "my.property=fromdevpropertiesfile")
	void loadWhenCustomDefaultProfileAndActiveFromPreviousSourceDoesNotActivateDefault() {
		ConfigurableApplicationContext context = this.application.run("--spring.profiles.default=customdefault",
				"--spring.profiles.active=dev");
		assertThat(context.getEnvironment().getProperty("my.property")).isEqualTo("fromdevpropertiesfile");
		assertThat(context.getEnvironment().containsProperty("customdefault")).isFalse();
	}

	@Test
	@WithResource(name = "application.properties", content = """
			spring.profiles.active=customdefault
			customprofile=true
			""")
	@WithResource(name = "application-customdefault.properties", content = "customprofile-customdefault=true")
	void runWhenCustomDefaultProfileSameAsActiveFromFileActivatesProfile() {
		ConfigurableApplicationContext context = this.application.run("--spring.profiles.default=customdefault");
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.containsProperty("customprofile")).isTrue();
		assertThat(environment.containsProperty("customprofile-customdefault")).isTrue();
		assertThat(environment.acceptsProfiles(Profiles.of("customdefault"))).isTrue();
	}

	@Test
	@WithResource(name = "application.properties", content = "spring.profiles.active=${activeProfile:propertiesfile}")
	void runWhenActiveProfilesCanBeConfiguredUsingPlaceholdersResolvedAgainstTheEnvironmentLoadsExpectedProperties() {
		ConfigurableApplicationContext context = this.application.run("--activeProfile=testPropertySource");
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("testPropertySource");
	}

	@Test
	@WithResource(name = "application.properties", content = """
			foo=bucket
			value=1234
			""")
	@WithResource(name = "override.properties", content = "foo=bar")
	void runWhenHasAdditionalLocationLoadsWithAdditionalTakingPrecedenceOverDefaultLocation() {
		ConfigurableApplicationContext context = this.application
			.run("--spring.config.additional-location=classpath:override.properties");
		assertThat(context.getEnvironment().getProperty("foo")).isEqualTo("bar");
		assertThat(context.getEnvironment().getProperty("value")).isEqualTo("1234");
	}

	@Test
	@WithResource(name = "application.properties", content = """
			foo=bucket
			value=1234
			""")
	@WithResource(name = "override.properties", content = "foo=bar")
	@WithResource(name = "some.properties", content = "foo=spam")
	void runWhenMultipleAdditionalLocationsLoadsWithLastWinning() {
		ConfigurableApplicationContext context = this.application
			.run("--spring.config.additional-location=classpath:override.properties,classpath:some.properties");
		assertThat(context.getEnvironment().getProperty("foo")).isEqualTo("spam");
		assertThat(context.getEnvironment().getProperty("value")).isEqualTo("1234");
	}

	@Test
	@WithResource(name = "application.properties", content = "value=1234")
	@WithResource(name = "override.properties", content = "foo=bar")
	@WithResource(name = "some.properties", content = "foo=spam")
	void runWhenAdditionalLocationAndLocationLoadsWithAdditionalTakingPrecedenceOverConfigured() {
		ConfigurableApplicationContext context = this.application.run(
				"--spring.config.location=classpath:some.properties",
				"--spring.config.additional-location=classpath:override.properties");
		assertThat(context.getEnvironment().getProperty("foo")).isEqualTo("bar");
		assertThat(context.getEnvironment().getProperty("value")).isNull();
	}

	@Test
	@WithResource(name = "application.custom", content = "")
	@WithResource(name = "META-INF/spring.factories", content = """
			org.springframework.boot.env.PropertySourceLoader=\
			org.springframework.boot.context.config.TestPropertySourceLoader1,\
			org.springframework.boot.context.config.TestPropertySourceLoader2
			""")
	void runWhenPropertiesFromCustomPropertySourceLoaderShouldLoadFromCustomSource() {
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("customloader1")).isEqualTo("true");
	}

	@Test
	@WithResource(name = "gh17001.properties", content = "gh17001loaded=true")
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
			.withMessageContaining("Unable to load config data")
			.withMessageContaining(location)
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
		assertThatExceptionOfType(ConfigDataLocationNotFoundException.class)
			.isThrownBy(() -> this.application.run("--spring.config.location=" + StringUtils.cleanPath("invalid/")));
	}

	@Test
	void runWhenConfigLocationHasNonOptionalEmptyFileDoesNotThrowException() throws IOException {
		File location = new File(this.temp, "application.properties");
		FileCopyUtils.copy(new byte[0], location);
		assertThatNoException().isThrownBy(() -> this.application
			.run("--spring.config.location=" + StringUtils.cleanPath(location.getAbsolutePath())));
	}

	@Test
	@WithResource(name = "META-INF/spring.factories", content = """
			org.springframework.boot.context.config.ConfigDataLocationResolver=\
			org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessorIntegrationTests$LocationResolver

			org.springframework.boot.context.config.ConfigDataLoader=\
			org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessorIntegrationTests$Loader
			""")
	void runWhenResolvedIsOptionalDoesNotThrowException() {
		ApplicationContext context = this.application.run("--spring.config.location=test:optionalresult");
		assertThat(context.getEnvironment().containsProperty("spring")).isFalse();
	}

	@Test
	@WithResource(name = "application.properties", content = "spring.profiles=a")
	void runWhenUsingInvalidPropertyThrowsException() {
		assertThatExceptionOfType(InvalidConfigDataPropertyException.class).isThrownBy(() -> this.application.run());
	}

	@Test
	@WithResource(name = "application.properties", content = """
			my.import=imported
			spring.config.import=classpath:${my.import}.properties
			""")
	@WithResource(name = "imported.properties", content = "my.value=iwasimported")
	void runWhenImportUsesPlaceholder() {
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("my.value")).isEqualTo("iwasimported");
	}

	@Test
	@WithResource(name = "application.properties", content = """
			my.import=imported
			#---
			spring.config.import=classpath:${my.import}.properties
			""")
	@WithResource(name = "imported.properties", content = "my.value=iwasimported")
	void runWhenImportFromEarlierDocumentUsesPlaceholder() {
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("my.value")).isEqualTo("iwasimported");
	}

	@Test // gh-26858
	@WithResource(name = "application.properties", content = """
			spring.config.import=classpath:imported.properties
			my.value=application.properties
			""")
	@WithResource(name = "imported.properties", content = "my.value=imported.properties")
	@WithResource(name = "application-dev.properties", content = "my.value=application-dev.properties")
	void runWhenImportWithProfileVariantOrdersPropertySourcesCorrectly() {
		this.application.setAdditionalProfiles("dev");
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("my.value")).isEqualTo("application-dev.properties");
	}

	@Test
	@WithResource(name = "application.properties", content = """
			spring.config.import=classpath:imported.properties
			my.value=application.properties
			""")
	@WithResource(name = "imported.properties", content = "my.value=imported.properties")
	@WithResource(name = "imported-dev.properties", content = "my.value=imported-dev.properties")
	@WithResource(name = "application-dev.properties", content = """
			spring.config.import=imported-dev.properties
			my.value=application-dev.properties""")
	void runWhenImportWithProfileVariantAndDirectProfileImportOrdersPropertySourcesCorrectly() {
		this.application.setAdditionalProfiles("dev");
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("my.value")).isEqualTo("imported-dev.properties");
	}

	@Test
	@WithResource(name = "application.properties", content = """
			my.import=application-import-with-placeholder-imported
			#---
			spring.config.import=classpath:org/springframework/boot/context/config/${my.import}.properties
			#---
			my.import=badbadbad
			spring.config.activate.on-profile=missing
			""")
	void runWhenHasPropertyInProfileDocumentThrowsException() {
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> this.application.run())
			.withCauseInstanceOf(InactiveConfigDataAccessException.class);
	}

	@Test // gh-29386
	@WithResource(name = "application.properties", content = """
			my.value=application
			#---
			my.import=imported
			spring.config.activate.on-profile=missing
			#---
			spring.config.import=${my.import}.properties
			""")
	@WithResource(name = "imported.properties", content = "my.value=imported")
	void runWhenHasPropertyInEarlierProfileDocumentThrowsException() {
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> this.application.run())
			.withCauseInstanceOf(InactiveConfigDataAccessException.class);
	}

	@Test // gh-29386
	@WithResource(name = "application.properties", content = """
			my.import=imported
			#---
			my.value=should-be-ignored
			spring.config.activate.on-profile=missing
			#---
			spring.config.import=classpath:${my.import}.properties
			""")
	@WithResource(name = "imported.properties", content = "my.value=imported")
	void runWhenHasPropertyInEarlierDocumentLoads() {
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getProperty("my.value")).isEqualTo("imported");
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
	@WithResource(name = "application.properties", content = """
			spring.profiles.active=p1
			spring.profiles.include=p2
			#---
			spring.profiles.include=p3,p4
			#---
			spring.profiles.include=p5
			""")
	void runWhenHasIncludedProfilesActivatesProfiles() {
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactlyInAnyOrder("p1", "p2", "p3", "p4",
				"p5");
	}

	@Test
	@WithResource(name = "application.properties", content = """
			spring.profiles.active=p1
			spring.profiles.include=p2
			#---
			myprofile=p4
			spring.profiles.include=p3,${myprofile}
			#---
			myotherprofile=p5
			spring.profiles.include=${myotherprofile}
			""")
	void runWhenHasIncludedProfilesWithPlaceholderActivatesProfiles() {
		ConfigurableApplicationContext context = this.application.run();
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactlyInAnyOrder("p1", "p2", "p3", "p4",
				"p5");
	}

	@Test
	@WithResource(name = "application.properties", content = """
			spring.profiles.active=p1
			spring.profiles.include=p2
			#---
			spring.config.activate.on-profile=p2
			spring.profiles.include=p3
			""")
	void runWhenHasIncludedProfilesWithProfileSpecificDocumentThrowsException() {
		assertThatExceptionOfType(InactiveConfigDataAccessException.class).isThrownBy(() -> this.application.run());
	}

	@Test
	@WithResource(name = "application-test.yaml", content = """
			spring:
			  profiles:
			    include:
			      - p
			""")
	void runWhenHasIncludedProfilesWithListSyntaxWithProfileSpecificDocumentThrowsException() {
		assertThatExceptionOfType(InvalidConfigDataPropertyException.class)
			.isThrownBy(() -> this.application.run("--spring.profiles.active=test"));
	}

	@Test
	@WithResource(name = "application.properties", content = """
			my.import=imported
			spring.config.import=classpath:${my.import}.properties
			""")
	@WithResource(name = "imported.properties", content = "my.value=imported")
	void runWhenImportingIncludesParentOrigin() {
		ConfigurableApplicationContext context = this.application.run();
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
		assertThat(origin.toString()).contains("imported.properties");
		assertThat(origin.getParent().toString()).contains("application.properties");
	}

	@Test
	@WithResource(name = "config/first/application.properties", content = "first.property=apple")
	@WithResource(name = "config/second/application.properties", content = "second.property=ball")
	@WithResource(name = "config/third/nested/application.properties", content = "third.property=three")
	void runWhenHasWildcardLocationLoadsFromAllMatchingLocations() {
		ConfigurableApplicationContext context = this.application.run("--spring.config.location=classpath:config/*/");
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.getProperty("first.property")).isEqualTo("apple");
		assertThat(environment.getProperty("second.property")).isEqualTo("ball");
		assertThat(environment.getProperty("third.property")).isNull();
	}

	@Test
	void runWhenOptionalWildcardFileDoesNotExistDoesNotThrowException() {
		assertThatNoException().isThrownBy(() -> this.application
			.run("--spring.config.location=optional:classpath:nonexistent/*/testproperties.properties"));
	}

	@Test
	void runWhenMandatoryWildcardFileDoesNotExistThrowsException() {
		assertThatExceptionOfType(ConfigDataLocationNotFoundException.class).isThrownBy(() -> this.application
			.run("--spring.config.location=classpath:nonexistent/*/testproperties.properties"));
	}

	@Test
	@WithResourceDirectory("config/empty")
	void runWhenMandatoryWildcardDirectoryHasEmptyDirectoryDoesNotThrowException() {
		assertThatNoException().isThrownBy(() -> this.application.run("--spring.config.location=classpath:config/*/"));
	}

	@Test
	@WithResourceDirectory("config/empty")
	void runWhenOptionalWildcardDirectoryHasNoSubdirectoriesDoesNotThrow() {
		assertThatNoException()
			.isThrownBy(() -> this.application.run("--spring.config.location=optional:classpath:config/*/"));
	}

	@Test
	@WithResourceDirectory("config")
	void runWhenMandatoryWildcardDirectoryHasNoSubdirectoriesThrows() {
		assertThatExceptionOfType(ConfigDataLocationNotFoundException.class)
			.isThrownBy(() -> this.application.run("--spring.config.location=classpath:config/*/"))
			.withMessage("Config data location 'classpath:config/*/' contains no subdirectories");
	}

	@Test
	void runWhenOptionalWildcardDirectoryDoesNotExistDoesNotThrowException() {
		assertThatNoException()
			.isThrownBy(() -> this.application.run("--spring.config.location=optional:file:invalid/*/"));
	}

	@Test // gh-24990
	@WithResource(name = "application.properties", content = "spring.profiles.active=test,other")
	@WithResource(name = "application-test.properties", content = """
			test1=test1
			#---
			spring.config.activate.on-profile=other
			test2=test2
			""")
	void runWhenHasProfileSpecificFileWithActiveOnProfileProperty() {
		ConfigurableApplicationContext context = this.application.run();
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.getProperty("test1")).isEqualTo("test1");
		assertThat(environment.getProperty("test2")).isEqualTo("test2");
	}

	@Test // gh-26960
	@WithResource(name = "application.properties", content = """
			spring.profiles.active=p1,p2
			application=true
			""")
	@WithResource(name = "application-p1.properties", content = """
			application-p1=true
			spring.config.import=import.properties
			""")
	@WithResource(name = "import.properties", content = "import=true")
	@WithResource(name = "import-p1.properties", content = "import-p1=true")
	@WithResource(name = "import-p2.properties", content = "import-p2=true")
	void runWhenHasProfileSpecificImportWithImportImportsSecondProfileSpecificFile() {
		ConfigurableApplicationContext context = this.application.run();
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.containsProperty("application")).isTrue();
		assertThat(environment.containsProperty("application-p1")).isTrue();
		assertThat(environment.containsProperty("application-p2")).isFalse();
		assertThat(environment.containsProperty("import")).isTrue();
		assertThat(environment.containsProperty("import-p1")).isTrue();
		assertThat(environment.containsProperty("import-p2")).isTrue();
	}

	@Test // gh-26960
	@WithResource(name = "application.properties", content = "spring.profiles.active=p1,p2")
	@WithResource(name = "application-p1.properties", content = "spring.config.import:test:boot")
	@WithResource(name = "META-INF/spring.factories", content = """
			org.springframework.boot.context.config.ConfigDataLocationResolver=\
			org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessorIntegrationTests$LocationResolver

			org.springframework.boot.context.config.ConfigDataLoader=\
			org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessorIntegrationTests$Loader
			""")
	void runWhenHasProfileSpecificImportWithCustomImportResolvesProfileSpecific() {
		ConfigurableApplicationContext context = this.application.run();
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.containsProperty("test:boot")).isTrue();
		assertThat(environment.containsProperty("test:boot:ps")).isTrue();
	}

	@Test // gh-26593
	@WithResource(name = "application.properties", content = """
			root=true
			v1=root
			v2=root
			""")
	@WithResource(name = "application-p1.properties", content = """
			root-p1=true
			v1=root-p1
			v2=root-p1
			""")
	@WithResource(name = "application-p2.properties", content = """
			root-p2=true
			v1=root-p2
			v2=root-p2
			""")
	@WithResource(name = "config/application.properties", content = """
			config=true
			v1=config
			v2=config
			""")
	@WithResource(name = "config/application-p1.properties", content = """
			config-p1=true
			v1=config-p1
			#v2 intentionally missing
			""")
	@WithResource(name = "config/application-p2.properties", content = """
			config-p2=true
			v1=config-p2
			#v2 intentionally missing
			""")
	void runWhenHasFilesInRootAndConfigWithProfiles() {
		ConfigurableApplicationContext context = this.application.run("--spring.profiles.active=p1,p2");
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.containsProperty("root")).isTrue();
		assertThat(environment.containsProperty("root-p1")).isTrue();
		assertThat(environment.containsProperty("root-p2")).isTrue();
		assertThat(environment.containsProperty("config")).isTrue();
		assertThat(environment.containsProperty("config-p1")).isTrue();
		assertThat(environment.containsProperty("config-p2")).isTrue();
		assertThat(environment.getProperty("v1")).isEqualTo("config-p2");
		assertThat(environment.getProperty("v2")).isEqualTo("root-p2");
	}

	private Condition<ConfigurableEnvironment> matchingPropertySource(final String sourceName) {
		return new Condition<>("environment containing property source " + sourceName) {

			@Override
			public boolean matches(ConfigurableEnvironment value) {
				return value.getPropertySources().contains(sourceName);
			}

		};
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
			map.put(resource + suffix, "true");
			MapPropertySource propertySource = new MapPropertySource("loaded" + suffix, map);
			return new ConfigData(Collections.singleton(propertySource));
		}

	}

	static class TestConfigDataResource extends ConfigDataResource {

		private final ConfigDataLocation location;

		private final boolean profileSpecific;

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
