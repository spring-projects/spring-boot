/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ResourceBanner}.
 *
 * @author Phillip Webb
 */
public class ResourceBannerTests {

	@Test
	public void renderVersions() throws Exception {
		Resource resource = new ByteArrayResource(
				"banner ${a} ${spring-boot.version} ${application.version}".getBytes());
		String banner = printBanner(resource, "10.2", "2.0");
		assertThat(banner, startsWith("banner 1 10.2 2.0"));
	}

	@Test
	public void renderWithoutVersions() throws Exception {
		Resource resource = new ByteArrayResource(
				"banner ${a} ${spring-boot.version} ${application.version}".getBytes());
		String banner = printBanner(resource, null, null);
		assertThat(banner, startsWith("banner 1  "));
	}

	@Test
	public void renderFormattedVersions() throws Exception {
		Resource resource = new ByteArrayResource(
				"banner ${a}${spring-boot.formatted-version}${application.formatted-version}"
						.getBytes());
		String banner = printBanner(resource, "10.2", "2.0");
		assertThat(banner, startsWith("banner 1 (v10.2) (v2.0)"));
	}

	@Test
	public void renderWithoutFormattedVersions() throws Exception {
		Resource resource = new ByteArrayResource(
				"banner ${a}${spring-boot.formatted-version}${application.formatted-version}"
						.getBytes());
		String banner = printBanner(resource, null, null);
		assertThat(banner, startsWith("banner 1"));
	}

	private String printBanner(Resource resource, String bootVersion,
			String applicationVersion) {
		ResourceBanner banner = new MockResourceBanner(resource, bootVersion,
				applicationVersion);
		ConfigurableEnvironment environment = new MockEnvironment();
		Map<String, Object> source = Collections.<String, Object> singletonMap("a", "1");
		environment.getPropertySources().addLast(new MapPropertySource("map", source));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		banner.printBanner(environment, getClass(), new PrintStream(out));
		return out.toString();
	}

	private static class MockResourceBanner extends ResourceBanner {

		private final String bootVersion;

		private final String applicationVersion;

		public MockResourceBanner(Resource resource, String bootVersion,
				String applicationVersion) {
			super(resource);
			this.bootVersion = bootVersion;
			this.applicationVersion = applicationVersion;
		}

		@Override
		protected String getBootVersion() {
			return this.bootVersion;
		}

		@Override
		protected String getApplicationVersion(Class<?> sourceClass) {
			return this.applicationVersion;
		}

	}

}
