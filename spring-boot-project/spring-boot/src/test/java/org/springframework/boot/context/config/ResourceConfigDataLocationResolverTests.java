/*
 * Copyright 2012-2020 the original author or authors.
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
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ResourceConfigDataLocationResolver}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
public class ResourceConfigDataLocationResolverTests {

	private ResourceConfigDataLocationResolver resolver;

	private ConfigDataLocationResolverContext context = mock(ConfigDataLocationResolverContext.class);

	private MockEnvironment environment;

	private Binder environmentBinder;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	@BeforeEach
	void setup() {
		this.environment = new MockEnvironment();
		this.environmentBinder = Binder.get(this.environment);
		this.resolver = new ResourceConfigDataLocationResolver(new DeferredLog(), this.environmentBinder,
				this.resourceLoader);
	}

	@Test
	void isResolvableAlwaysReturnsTrue() {
		assertThat(this.resolver.isResolvable(this.context, "test")).isTrue();
	}

	@Test
	void resolveWhenLocationIsDirectoryResolvesAllMatchingFilesInDirectory() {
		String location = "classpath:/configdata/properties/";
		List<ResourceConfigDataLocation> locations = this.resolver.resolve(this.context, location);
		assertThat(locations.size()).isEqualTo(1);
		assertThat(locations).extracting(Object::toString)
				.containsExactly("class path resource [configdata/properties/application.properties]");
	}

	@Test
	void resolveWhenLocationIsFileResolvesFile() {
		String location = "file:src/test/resources/configdata/properties/application.properties";
		List<ResourceConfigDataLocation> locations = this.resolver.resolve(this.context, location);
		assertThat(locations.size()).isEqualTo(1);
		assertThat(locations).extracting(Object::toString).containsExactly(
				filePath("src", "test", "resources", "configdata", "properties", "application.properties"));
	}

	@Test
	void resolveWhenLocationIsFileAndNoMatchingLoaderThrowsException() {
		String location = "file:src/test/resources/configdata/properties/application.unknown";
		assertThatIllegalStateException().isThrownBy(() -> this.resolver.resolve(this.context, location))
				.withMessageStartingWith("Unable to load config data from")
				.satisfies((ex) -> assertThat(ex.getCause()).hasMessageStartingWith("File extension is not known"));
	}

	@Test
	void resolveWhenLocationWildcardIsSpecifiedForClasspathLocationThrowsException() {
		String location = "classpath*:application.properties";
		assertThatIllegalStateException().isThrownBy(() -> this.resolver.resolve(this.context, location))
				.withMessageContaining("Classpath wildcard patterns cannot be used as a search location");
	}

	@Test
	void resolveWhenLocationWildcardIsNotBeforeLastSlashThrowsException() {
		String location = "file:src/test/resources/*/config/";
		assertThatIllegalStateException().isThrownBy(() -> this.resolver.resolve(this.context, location))
				.withMessageStartingWith("Search location '").withMessageEndingWith("' must end with '*/'");
	}

	@Test
	void createWhenConfigNameHasWildcardThrowsException() {
		this.environment.setProperty("spring.config.name", "*/application");
		assertThatIllegalStateException()
				.isThrownBy(
						() -> new ResourceConfigDataLocationResolver(null, this.environmentBinder, this.resourceLoader))
				.withMessageStartingWith("Config name '").withMessageEndingWith("' cannot contain '*'");
	}

	@Test
	void resolveWhenLocationHasMultipleWildcardsThrowsException() {
		String location = "file:src/test/resources/config/**/";
		assertThatIllegalStateException().isThrownBy(() -> this.resolver.resolve(this.context, location))
				.withMessageStartingWith("Search location '")
				.withMessageEndingWith("' cannot contain multiple wildcards");
	}

	@Test
	void resolveWhenLocationIsWildcardDirectoriesRestrictsToOneLevelDeep() {
		String location = "file:src/test/resources/config/*/";
		this.environment.setProperty("spring.config.name", "testproperties");
		this.resolver = new ResourceConfigDataLocationResolver(null, this.environmentBinder, this.resourceLoader);
		List<ResourceConfigDataLocation> locations = this.resolver.resolve(this.context, location);
		assertThat(locations.size()).isEqualTo(3);
		assertThat(locations).extracting(Object::toString)
				.contains(filePath("src", "test", "resources", "config", "1-first", "testproperties.properties"))
				.contains(filePath("src", "test", "resources", "config", "2-second", "testproperties.properties"))
				.doesNotContain(filePath("src", "test", "resources", "config", "3-third", "testproperties.properties"));
	}

	@Test
	void resolveWhenLocationIsWildcardDirectoriesSortsAlphabeticallyBasedOnAbsolutePath() {
		String location = "file:src/test/resources/config/*/";
		this.environment.setProperty("spring.config.name", "testproperties");
		this.resolver = new ResourceConfigDataLocationResolver(null, this.environmentBinder, this.resourceLoader);
		List<ResourceConfigDataLocation> locations = this.resolver.resolve(this.context, location);
		assertThat(locations).extracting(Object::toString).containsExactly(
				filePath("src", "test", "resources", "config", "0-empty", "testproperties.properties"),
				filePath("src", "test", "resources", "config", "1-first", "testproperties.properties"),
				filePath("src", "test", "resources", "config", "2-second", "testproperties.properties"));
	}

	@Test
	void resolveWhenLocationIsWildcardFilesLoadsAllFilesThatMatch() {
		String location = "file:src/test/resources/config/*/testproperties.properties";
		List<ResourceConfigDataLocation> locations = this.resolver.resolve(this.context, location);
		assertThat(locations.size()).isEqualTo(3);
		assertThat(locations).extracting(Object::toString)
				.contains(filePath("src", "test", "resources", "config", "1-first", "testproperties.properties"))
				.contains(filePath("src", "test", "resources", "config", "2-second", "testproperties.properties"))
				.doesNotContain(filePath("src", "test", "resources", "config", "nested", "3-third",
						"testproperties.properties"));
	}

	@Test
	void resolveWhenLocationIsRelativeAndFileResolves() {
		this.environment.setProperty("spring.config.name", "other");
		String location = "other.properties";
		this.resolver = new ResourceConfigDataLocationResolver(new DeferredLog(), this.environmentBinder,
				this.resourceLoader);
		ClassPathResource parentResource = new ClassPathResource("configdata/properties/application.properties");
		ResourceConfigDataLocation parent = new ResourceConfigDataLocation(
				"classpath:/configdata/properties/application.properties", parentResource,
				new PropertiesPropertySourceLoader());
		given(this.context.getParent()).willReturn(parent);
		List<ResourceConfigDataLocation> locations = this.resolver.resolve(this.context, location);
		assertThat(locations.size()).isEqualTo(1);
		assertThat(locations).extracting(Object::toString)
				.contains("class path resource [configdata/properties/other.properties]");
	}

	@Test
	void resolveWhenLocationIsRelativeAndDirectoryResolves() {
		this.environment.setProperty("spring.config.name", "testproperties");
		String location = "nested/3-third/";
		this.resolver = new ResourceConfigDataLocationResolver(new DeferredLog(), this.environmentBinder,
				this.resourceLoader);
		ClassPathResource parentResource = new ClassPathResource("config/specific.properties");
		ResourceConfigDataLocation parent = new ResourceConfigDataLocation("classpath:/config/specific.properties",
				parentResource, new PropertiesPropertySourceLoader());
		given(this.context.getParent()).willReturn(parent);
		List<ResourceConfigDataLocation> locations = this.resolver.resolve(this.context, location);
		assertThat(locations.size()).isEqualTo(1);
		assertThat(locations).extracting(Object::toString)
				.contains("class path resource [config/nested/3-third/testproperties.properties]");
	}

	@Test
	void resolveWhenLocationIsRelativeAndNoMatchingLoaderThrowsException() {
		String location = "application.other";
		ClassPathResource parentResource = new ClassPathResource("configdata/application.properties");
		ResourceConfigDataLocation parent = new ResourceConfigDataLocation(
				"classpath:/configdata/application.properties", parentResource, new PropertiesPropertySourceLoader());
		given(this.context.getParent()).willReturn(parent);
		assertThatIllegalStateException().isThrownBy(() -> this.resolver.resolve(this.context, location))
				.withMessageStartingWith("Unable to load config data from 'application.other'")
				.satisfies((ex) -> assertThat(ex.getCause()).hasMessageStartingWith("File extension is not known"));
	}

	@Test
	void resolveProfileSpecificReturnsProfileSpecificFiles() {
		String location = "classpath:/configdata/properties/";
		Profiles profiles = mock(Profiles.class);
		given(profiles.iterator()).willReturn(Collections.singletonList("dev").iterator());
		List<ResourceConfigDataLocation> locations = this.resolver.resolveProfileSpecific(this.context, location,
				profiles);
		assertThat(locations.size()).isEqualTo(1);
		assertThat(locations).extracting(Object::toString)
				.containsExactly("class path resource [configdata/properties/application-dev.properties]");
	}

	@Test
	void resolveProfileSpecificWhenLocationIsFileReturnsEmptyList() {
		String location = "classpath:/configdata/properties/application.properties";
		Profiles profiles = mock(Profiles.class);
		given(profiles.iterator()).willReturn(Collections.emptyIterator());
		given(profiles.getActive()).willReturn(Collections.singletonList("dev"));
		List<ResourceConfigDataLocation> locations = this.resolver.resolveProfileSpecific(this.context, location,
				profiles);
		assertThat(locations).isEmpty();
	}

	private String filePath(String... components) {
		return "file [" + String.join(File.separator, components) + "]";
	}

}
