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

package org.springframework.boot.context.initializer;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
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

	@After
	public void cleanup() {
		System.clearProperty("my.property");
	}

	@Test
	public void loadPropertiesFile() throws Exception {
		this.initializer.setNames("testproperties");
		this.initializer.initialize(this.context);
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("frompropertiesfile"));
	}

	@Test
	public void randomValue() throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		this.initializer.setEnvironment(environment);
		this.initializer.initialize(new SpringApplication(), new String[0]);
		String property = environment.getProperty("random.value");
		assertThat(property, notNullValue());
	}

	@Test
	public void loadTwoPropertiesFiles() throws Exception {
		this.initializer.setNames("testproperties,moreproperties");
		this.initializer.initialize(this.context);
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("frommorepropertiesfile"));
	}

	@Test
	public void loadYamlFile() throws Exception {
		this.initializer.setNames("testyaml");
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
		this.initializer.setNames("testproperties");
		this.initializer.initialize(this.context);
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("fromcommandline"));
	}

	@Test
	public void systemPropertyWins() throws Exception {
		System.setProperty("my.property", "fromsystem");
		this.initializer.setNames("testproperties");
		this.initializer.initialize(this.context);
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("fromsystem"));
	}

	@Test
	public void loadPropertiesThenProfileProperties() throws Exception {
		this.initializer.setNames("enableprofile");
		this.initializer.initialize(this.context);
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("fromprofilepropertiesfile"));
	}

	@Test
	public void profilePropertiesUsedInPlaceholders() throws Exception {
		this.initializer.setNames("enableprofile");
		this.initializer.initialize(this.context);
		String property = this.context.getEnvironment().getProperty("one.more");
		assertThat(property, equalTo("fromprofilepropertiesfile"));
	}

	@Test
	public void yamlProfiles() throws Exception {
		this.initializer.setNames("testprofiles");
		this.context.getEnvironment().setActiveProfiles("dev");
		this.initializer.initialize(this.context);
		String property = this.context.getEnvironment().getProperty("my.property");
		assertThat(property, equalTo("fromdevprofile"));
		property = this.context.getEnvironment().getProperty("my.other");
		assertThat(property, equalTo("notempty"));
	}

	@Test
	public void yamlSetsProfiles() throws Exception {
		this.initializer.setNames("testsetprofiles");
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

	@Test
	public void defaultApplicationProperties() throws Exception {

	}

}
