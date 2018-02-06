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

package org.springframework.boot;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiOutput.Enabled;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResourceBanner}.
 *
 * @author Phillip Webb
 * @author Vedran Pavic
 */
public class ResourceBannerTests {

	@After
	public void reset() {
		AnsiOutput.setEnabled(Enabled.DETECT);
	}

	@Test
	public void renderVersions() throws Exception {
		Resource resource = new ByteArrayResource(
				"banner ${a} ${spring-boot.version} ${application.version}".getBytes());
		String banner = printBanner(resource, null, null, "10.2", "2.0", null);
		assertThat(banner).startsWith("banner 1 10.2 2.0");
	}

	@Test
	public void renderWithoutVersions() throws Exception {
		Resource resource = new ByteArrayResource(
				"banner ${a} ${spring-boot.version} ${application.version}".getBytes());
		String banner = printBanner(resource, null, null, null, null, null);
		assertThat(banner).startsWith("banner 1  ");
	}

	@Test
	public void renderFormattedVersions() throws Exception {
		Resource resource = new ByteArrayResource(
				"banner ${a}${spring-boot.formatted-version}${application.formatted-version}"
						.getBytes());
		String banner = printBanner(resource, null, null, "10.2", "2.0", null);
		assertThat(banner).startsWith("banner 1 (v10.2) (v2.0)");
	}

	@Test
	public void renderWithoutFormattedVersions() throws Exception {
		Resource resource = new ByteArrayResource(
				"banner ${a}${spring-boot.formatted-version}${application.formatted-version}"
						.getBytes());
		String banner = printBanner(resource, null, null, null, null, null);
		assertThat(banner).startsWith("banner 1");
	}

	@Test
	public void renderWithColors() throws Exception {
		Resource resource = new ByteArrayResource(
				"${Ansi.RED}This is red.${Ansi.NORMAL}".getBytes());
		AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
		String banner = printBanner(resource, null, null, null, null, null);
		assertThat(banner).startsWith("\u001B[31mThis is red.\u001B[0m");
	}

	@Test
	public void renderWithColorsButDisabled() throws Exception {
		Resource resource = new ByteArrayResource(
				"${Ansi.RED}This is red.${Ansi.NORMAL}".getBytes());
		AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		String banner = printBanner(resource, null, null, null, null, null);
		assertThat(banner).startsWith("This is red.");
	}

	@Test
	public void renderWithTitle() throws Exception {
		Resource resource = new ByteArrayResource(
				"banner ${application.title} ${a}".getBytes());
		String banner = printBanner(resource, null, null, null, null, "title");
		assertThat(banner).startsWith("banner title 1");
	}

	@Test
	public void renderWithoutTitle() throws Exception {
		Resource resource = new ByteArrayResource(
				"banner ${application.title} ${a}".getBytes());
		String banner = printBanner(resource, null, null, null, null, null);
		assertThat(banner).startsWith("banner  1");
	}

	@Test
	public void renderGitInfo() throws Exception {
		Resource resource = new ByteArrayResource(
				("banner" +
				" ${a}" +
				" ${git.commit.id.abbrev}" +
				" ${git.commit.user.email}" +
				" ${git.commit.message.full}" +
				" ${git.commit.id}" +
				" ${git.commit.message.short}" +
				" ${git.commit.user.name}" +
				" ${git.build.user.name}" +
				" ${git.build.user.email}" +
				" ${git.branch}" +
				" ${git.commit.time}" +
				" ${git.build.time}").getBytes());

		String s =
				"git.commit.id.abbrev=abc\n" +
				"git.commit.user.email=abc@bce.com\n" +
				"git.commit.message.full=Test\n" +
				"git.commit.id=abcdef\n" +
				"git.commit.message.short=Test short\n" +
				"git.commit.user.name=test_user\n" +
				"git.build.user.name=Test User\n" +
				"git.build.user.email=abc@bce.com\n" +
				"git.branch=develop\n" +
				"git.commit.time=commit_time\n" +
				"git.build.time=build_time\n";

		Resource gitPropertiesResource = new ByteArrayResource(s.getBytes());

		String banner = printBanner(resource, gitPropertiesResource, null, null, null, null);
		assertThat(banner).startsWith("banner 1 abc abc@bce.com Test abcdef Test short test_user Test User abc@bce.com " +
				"develop commit_time build_time");
	}
	@Test
	public void renderBuildInfo() throws Exception {
		Resource resource = new ByteArrayResource(
				("banner" +
				" ${a}" +
				" ${build.time}" +
				" ${build.artifact}" +
				" ${build.group}" +
				" ${build.name}" +
				" ${build.version}").getBytes());

		String s =
				"build.time=time\n" +
				"build.artifact=artifact\n" +
				"build.group=group\n" +
				"build.name=name\n" +
				"build.version=0.0.1-SNAPSHOT";

		Resource buildInfo = new ByteArrayResource(s.getBytes());

		String banner = printBanner(resource, null, buildInfo, null, null, null);
		assertThat(banner).startsWith("banner 1 time artifact group name 0.0.1-SNAPSHOT");
	}

	private String printBanner(Resource resource, Resource gitPropertyResource, Resource buildInfoPropertiesResource, String bootVersion,
			String applicationVersion, String applicationTitle) {
		ResourceBanner banner = new MockResourceBanner(resource, gitPropertyResource, buildInfoPropertiesResource, bootVersion,
				applicationVersion, applicationTitle);
		ConfigurableEnvironment environment = new MockEnvironment();
		Map<String, Object> source = Collections.<String, Object>singletonMap("a", "1");
		environment.getPropertySources().addLast(new MapPropertySource("map", source));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		banner.printBanner(environment, getClass(), new PrintStream(out));
		return out.toString();
	}

	private static class MockResourceBanner extends ResourceBanner {

		private final String bootVersion;

		private final String applicationVersion;

		private final String applicationTitle;

		MockResourceBanner(Resource resource, Resource gitPropertiesResource, Resource buildInfoPropertiesResource, String bootVersion,
				String applicationVersion, String applicationTitle) {
			super(resource, gitPropertiesResource, buildInfoPropertiesResource);
			this.bootVersion = bootVersion;
			this.applicationVersion = applicationVersion;
			this.applicationTitle = applicationTitle;
		}

		@Override
		protected String getBootVersion() {
			return this.bootVersion;
		}

		@Override
		protected String getApplicationVersion(Class<?> sourceClass) {
			return this.applicationVersion;
		}

		@Override
		protected String getApplicationTitle(Class<?> sourceClass) {
			return this.applicationTitle;
		}

	}

}
