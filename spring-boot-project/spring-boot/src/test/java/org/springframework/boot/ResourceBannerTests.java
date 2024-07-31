/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiOutput.Enabled;
import org.springframework.core.env.AbstractPropertyResolver;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResourceBanner}.
 *
 * @author Phillip Webb
 * @author Vedran Pavic
 * @author Toshiaki Maki
 * @author Krzysztof Krason
 */
class ResourceBannerTests {

	@AfterEach
	void reset() {
		AnsiOutput.setEnabled(Enabled.DETECT);
	}

	@Test
	void renderVersions() {
		Resource resource = new ByteArrayResource(
				"banner ${a} ${spring-boot.version} ${application.version}".getBytes());
		String banner = printBanner(resource, "10.2", "2.0", null);
		assertThat(banner).startsWith("banner 1 10.2 2.0");
	}

	@Test
	void renderWithoutVersions() {
		Resource resource = new ByteArrayResource(
				"banner ${a} ${spring-boot.version} ${application.version}".getBytes());
		String banner = printBanner(resource, null, null, null);
		assertThat(banner).startsWith("banner 1  ");
	}

	@Test
	void renderFormattedVersions() {
		Resource resource = new ByteArrayResource(
				"banner ${a}${spring-boot.formatted-version}${application.formatted-version}".getBytes());
		String banner = printBanner(resource, "10.2", "2.0", null);
		assertThat(banner).startsWith("banner 1 (v10.2) (v2.0)");
	}

	@Test
	void renderWithoutFormattedVersions() {
		Resource resource = new ByteArrayResource(
				"banner ${a}${spring-boot.formatted-version}${application.formatted-version}".getBytes());
		String banner = printBanner(resource, null, null, null);
		assertThat(banner).startsWith("banner 1");
	}

	@Test
	void renderWithColors() {
		Resource resource = new ByteArrayResource("${Ansi.RED}This is red.${Ansi.NORMAL}".getBytes());
		AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
		String banner = printBanner(resource, null, null, null);
		assertThat(banner).startsWith("\u001B[31mThis is red.\u001B[0m");
	}

	@Test
	void renderWithColorsButDisabled() {
		Resource resource = new ByteArrayResource("${Ansi.RED}This is red.${Ansi.NORMAL}".getBytes());
		AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		String banner = printBanner(resource, null, null, null);
		assertThat(banner).startsWith("This is red.");
	}

	@Test
	void renderWith256Colors() {
		Resource resource = new ByteArrayResource("${AnsiColor.208}This is orange.${Ansi.NORMAL}".getBytes());
		AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
		String banner = printBanner(resource, null, null, null);
		assertThat(banner).startsWith("\033[38;5;208mThis is orange.\u001B[0m");
	}

	@Test
	void renderWith256ColorsButDisabled() {
		Resource resource = new ByteArrayResource("${AnsiColor.208}This is orange.${Ansi.NORMAL}".getBytes());
		AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		String banner = printBanner(resource, null, null, null);
		assertThat(banner).startsWith("This is orange.");
	}

	@Test
	void renderWithTitle() {
		Resource resource = new ByteArrayResource("banner ${application.title} ${a}".getBytes());
		String banner = printBanner(resource, null, null, "title");
		assertThat(banner).startsWith("banner title 1");
	}

	@Test
	void renderWithoutTitle() {
		Resource resource = new ByteArrayResource("banner ${application.title} ${a}".getBytes());
		String banner = printBanner(resource, null, null, null);
		assertThat(banner).startsWith("banner  1");
	}

	@Test
	void renderWithDefaultValues() {
		Resource resource = new ByteArrayResource(
				"banner ${a:default-a} ${b:default-b} ${spring-boot.version:default-boot-version} ${application.version:default-application-version}"
					.getBytes());
		String banner = printBanner(resource, "10.2", "1.0", null);
		assertThat(banner).startsWith("banner 1 default-b 10.2 1.0");
	}

	@Test
	void renderWithMutation() {
		Resource resource = new ByteArrayResource("banner ${foo}".getBytes());
		String banner = printBanner(new MutatingResourceBanner(resource, "1", null), "2");
		assertThat(banner).startsWith("banner bar");
	}

	private String printBanner(Resource resource, String bootVersion, String applicationVersion,
			String applicationTitle) {
		return printBanner(new MockResourceBanner(resource, bootVersion, applicationTitle), applicationVersion);
	}

	private String printBanner(ResourceBanner banner, String applicationVersion) {
		MockEnvironment environment = new MockEnvironment();
		if (applicationVersion != null) {
			environment.setProperty("spring.application.version", applicationVersion);
		}
		Map<String, Object> source = Collections.singletonMap("a", "1");
		environment.getPropertySources().addLast(new MapPropertySource("map", source));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		banner.printBanner(environment, getClass(), new PrintStream(out));
		return out.toString();
	}

	static class MockResourceBanner extends ResourceBanner {

		private final String bootVersion;

		private final String applicationTitle;

		MockResourceBanner(Resource resource, String bootVersion, String applicationTitle) {
			super(resource);
			this.bootVersion = bootVersion;
			this.applicationTitle = applicationTitle;
		}

		@Override
		protected String getBootVersion() {
			return this.bootVersion;
		}

		@Override
		protected String getApplicationTitle(Class<?> sourceClass) {
			return this.applicationTitle;
		}

	}

	static class MutatingResourceBanner extends MockResourceBanner {

		MutatingResourceBanner(Resource resource, String bootVersion, String applicationTitle) {
			super(resource, bootVersion, applicationTitle);
		}

		@Override
		protected List<PropertyResolver> getPropertyResolvers(Environment environment, Class<?> sourceClass) {
			List<PropertyResolver> resolvers = super.getPropertyResolvers(environment, sourceClass);
			PropertyResolver resolver = new AbstractPropertyResolver() {

				@Override
				@SuppressWarnings("unchecked")
				public <T> T getProperty(String key, Class<T> targetType) {
					return String.class.equals(targetType) ? (T) getPropertyAsRawString(key) : null;
				}

				@Override
				protected String getPropertyAsRawString(String key) {
					return ("foo".equals(key)) ? "bar" : null;
				}

			};
			resolvers.add(resolver);
			return resolvers;
		}

	}

}
