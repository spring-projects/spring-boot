/*
 * Copyright 2010-2012 the original author or authors.
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

package org.springframework.bootstrap.context.initializer;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ConfigFileApplicationContextInitializer}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public class ConfigFileApplicationContextInitializerTests {

	private StaticApplicationContext context = new StaticApplicationContext();

	private ConfigFileApplicationContextInitializer initializer = new ConfigFileApplicationContextInitializer();

	@Test
	public void loadPropertiesFile() throws Exception {
		this.initializer.setName("testproperties");
		this.initializer.initialize(this.context);
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("frompropertiesfile"));
	}

	@Test
	public void loadYamlFile() throws Exception {
		this.initializer.setName("testyaml");
		this.initializer.initialize(this.context);
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("fromyamlfile"));
		assertThat(this.context.getEnvironment().getProperty("my.array[0]"), equalTo("1"));
		assertThat(this.context.getEnvironment().getProperty("my.array"),
				nullValue(String.class));
	}

	@Test
	public void commandLineWins() throws Exception {
		this.context
				.getEnvironment()
				.getPropertySources()
				.addFirst(
						new SimpleCommandLinePropertySource(
								"--my.property=fromcommandline"));
		this.initializer.setName("testproperties");
		this.initializer.initialize(this.context);
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("fromcommandline"));
	}

	@Test
	public void loadPropertiesThenProfileProperties() throws Exception {
		this.initializer.setName("enableprofile");
		this.initializer.initialize(this.context);
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("fromprofilepropertiesfile"));
	}

	@Test
	public void yamlProfiles() throws Exception {
		this.initializer.setName("testprofiles");
		this.context.getEnvironment().setActiveProfiles("dev");
		this.initializer.initialize(this.context);
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("fromdevprofile"));
		property = this.context.getEnvironment().getProperty("my.other");
		assertThat(property, equalTo("notempty"));
	}

	@Test
	public void yamlSetsProfiles() throws Exception {
		this.initializer.setName("testsetprofiles");
		this.initializer.initialize(this.context);
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("fromdevprofile"));
	}

	@Test
	public void specificNameAndProfileFromExistingSource() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("spring.profiles.active", "specificprofile");
		map.put("spring.config.name", "specificfile");
		MapPropertySource source = new MapPropertySource("map", map);
		this.context.getEnvironment().getPropertySources().addFirst(source);
		this.initializer.initialize(this.context);
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("fromspecificpropertiesfile"));
	}

	@Test
	public void specificResource() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("spring.config.location", "classpath:/specificlocation.properties");
		MapPropertySource source = new MapPropertySource("map", map);
		this.context.getEnvironment().getPropertySources().addFirst(source);
		this.initializer.initialize(this.context);
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("fromspecificlocation"));
	}

}
