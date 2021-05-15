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

package org.springframework.boot.maven;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JavaCompilerPluginConfiguration}.
 *
 * @author Scott Frederick
 */
class JavaCompilerPluginConfigurationTests {

	private MavenProject project;

	private Plugin plugin;

	@BeforeEach
	void setUp() {
		this.project = mock(MavenProject.class);
		this.plugin = mock(Plugin.class);
		given(this.project.getPlugin(anyString())).willReturn(this.plugin);
	}

	@Test
	void versionsAreNullWithNoConfiguration() {
		given(this.plugin.getConfiguration()).willReturn(null);
		given(this.project.getProperties()).willReturn(new Properties());
		JavaCompilerPluginConfiguration configuration = new JavaCompilerPluginConfiguration(this.project);
		assertThat(configuration.getSourceMajorVersion()).isNull();
		assertThat(configuration.getTargetMajorVersion()).isNull();
	}

	@Test
	void versionsAreReturnedFromConfiguration() throws IOException, XmlPullParserException {
		Xpp3Dom dom = buildConfigurationDom("<source>1.9</source>", "<target>11</target>");
		given(this.plugin.getConfiguration()).willReturn(dom);
		Properties properties = new Properties();
		properties.setProperty("maven.compiler.source", "1.8");
		properties.setProperty("maven.compiler.target", "10");
		given(this.project.getProperties()).willReturn(properties);
		JavaCompilerPluginConfiguration configuration = new JavaCompilerPluginConfiguration(this.project);
		assertThat(configuration.getSourceMajorVersion()).isEqualTo("9");
		assertThat(configuration.getTargetMajorVersion()).isEqualTo("11");
	}

	@Test
	void versionsAreReturnedFromProperties() {
		given(this.plugin.getConfiguration()).willReturn(null);
		Properties properties = new Properties();
		properties.setProperty("maven.compiler.source", "1.8");
		properties.setProperty("maven.compiler.target", "11");
		given(this.project.getProperties()).willReturn(properties);
		JavaCompilerPluginConfiguration configuration = new JavaCompilerPluginConfiguration(this.project);
		assertThat(configuration.getSourceMajorVersion()).isEqualTo("8");
		assertThat(configuration.getTargetMajorVersion()).isEqualTo("11");
	}

	private Xpp3Dom buildConfigurationDom(String... properties) throws IOException, XmlPullParserException {
		return Xpp3DomBuilder
				.build(new StringReader("<configuration>" + Arrays.toString(properties) + "</configuration>"));
	}

}
