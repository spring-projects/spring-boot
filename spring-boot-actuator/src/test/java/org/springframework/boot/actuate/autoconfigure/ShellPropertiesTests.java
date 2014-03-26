/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.crsh.plugin.PluginLifeCycle;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.boot.actuate.autoconfigure.ShellProperties.CrshShellProperties;
import org.springframework.boot.actuate.autoconfigure.ShellProperties.JaasAuthenticationProperties;
import org.springframework.boot.actuate.autoconfigure.ShellProperties.KeyAuthenticationProperties;
import org.springframework.boot.actuate.autoconfigure.ShellProperties.SimpleAuthenticationProperties;
import org.springframework.boot.actuate.autoconfigure.ShellProperties.SpringAuthenticationProperties;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ShellProperties}.
 * 
 * @author Christian Dupuis
 */
public class ShellPropertiesTests {

	@Test
	public void testBindingAuth() {
		ShellProperties props = new ShellProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.bind(new MutablePropertyValues(Collections.singletonMap("shell.auth",
				"spring")));
		assertFalse(binder.getBindingResult().hasErrors());
		assertEquals("spring", props.getAuth());
	}

	@Test
	public void testBindingAuthIfEmpty() {
		ShellProperties props = new ShellProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.bind(new MutablePropertyValues(Collections.singletonMap("shell.auth", "")));
		assertTrue(binder.getBindingResult().hasErrors());
		assertEquals("simple", props.getAuth());
	}

	@Test
	public void testBindingCommandRefreshInterval() {
		ShellProperties props = new ShellProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		binder.bind(new MutablePropertyValues(Collections.singletonMap(
				"shell.command_refresh_interval", "1")));
		assertFalse(binder.getBindingResult().hasErrors());
		assertEquals(1, props.getCommandRefreshInterval());
	}

	@Test
	public void testBindingCommandPathPatterns() {
		ShellProperties props = new ShellProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		binder.bind(new MutablePropertyValues(Collections.singletonMap(
				"shell.command_path_patterns", "pattern1, pattern2")));
		assertFalse(binder.getBindingResult().hasErrors());
		assertEquals(2, props.getCommandPathPatterns().length);
		Assert.assertArrayEquals(new String[] { "pattern1", "pattern2" },
				props.getCommandPathPatterns());
	}

	@Test
	public void testBindingConfigPathPatterns() {
		ShellProperties props = new ShellProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		binder.bind(new MutablePropertyValues(Collections.singletonMap(
				"shell.config_path_patterns", "pattern1, pattern2")));
		assertFalse(binder.getBindingResult().hasErrors());
		assertEquals(2, props.getConfigPathPatterns().length);
		Assert.assertArrayEquals(new String[] { "pattern1", "pattern2" },
				props.getConfigPathPatterns());
	}

	@Test
	public void testBindingDisabledPlugins() {
		ShellProperties props = new ShellProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		binder.bind(new MutablePropertyValues(Collections.singletonMap(
				"shell.disabled_plugins", "pattern1, pattern2")));
		assertFalse(binder.getBindingResult().hasErrors());
		assertEquals(2, props.getDisabledPlugins().length);
		assertArrayEquals(new String[] { "pattern1", "pattern2" },
				props.getDisabledPlugins());
	}

	@Test
	public void testBindingSsh() {
		ShellProperties props = new ShellProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		Map<String, String> map = new HashMap<String, String>();
		map.put("shell.ssh.enabled", "true");
		map.put("shell.ssh.port", "2222");
		map.put("shell.ssh.key_path", "~/.ssh/test.pem");
		binder.bind(new MutablePropertyValues(map));
		assertFalse(binder.getBindingResult().hasErrors());

		Properties p = props.asCrshShellConfig();

		assertEquals("2222", p.get("crash.ssh.port"));
		assertEquals("~/.ssh/test.pem", p.get("crash.ssh.keypath"));
	}

	@Test
	public void testBindingSshIgnored() {
		ShellProperties props = new ShellProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		Map<String, String> map = new HashMap<String, String>();
		map.put("shell.ssh.enabled", "false");
		map.put("shell.ssh.port", "2222");
		map.put("shell.ssh.key_path", "~/.ssh/test.pem");
		binder.bind(new MutablePropertyValues(map));
		assertFalse(binder.getBindingResult().hasErrors());

		Properties p = props.asCrshShellConfig();

		assertNull(p.get("crash.ssh.port"));
		assertNull(p.get("crash.ssh.keypath"));
	}

	@Test
	public void testBindingTelnet() {
		ShellProperties props = new ShellProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		Map<String, String> map = new HashMap<String, String>();
		map.put("shell.telnet.enabled", "true");
		map.put("shell.telnet.port", "2222");
		binder.bind(new MutablePropertyValues(map));
		assertFalse(binder.getBindingResult().hasErrors());

		Properties p = props.asCrshShellConfig();

		assertEquals("2222", p.get("crash.telnet.port"));
	}

	@Test
	public void testBindingTelnetIgnored() {
		ShellProperties props = new ShellProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		Map<String, String> map = new HashMap<String, String>();
		map.put("shell.telnet.enabled", "false");
		map.put("shell.telnet.port", "2222");
		binder.bind(new MutablePropertyValues(map));
		assertFalse(binder.getBindingResult().hasErrors());

		Properties p = props.asCrshShellConfig();

		assertNull(p.get("crash.telnet.port"));
	}

	@Test
	public void testBindingJaas() {
		JaasAuthenticationProperties props = new JaasAuthenticationProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell.auth.jaas");
		binder.setConversionService(new DefaultConversionService());
		Map<String, String> map = new HashMap<String, String>();
		map.put("shell.auth.jaas.domain", "my-test-domain");
		binder.bind(new MutablePropertyValues(map));
		assertFalse(binder.getBindingResult().hasErrors());

		Properties p = new Properties();
		props.applyToCrshShellConfig(p);

		assertEquals("my-test-domain", p.get("crash.auth.jaas.domain"));
	}

	@Test
	public void testBindingKey() {
		KeyAuthenticationProperties props = new KeyAuthenticationProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell.auth.key");
		binder.setConversionService(new DefaultConversionService());
		Map<String, String> map = new HashMap<String, String>();
		map.put("shell.auth.key.path", "~/.ssh/test.pem");
		binder.bind(new MutablePropertyValues(map));
		assertFalse(binder.getBindingResult().hasErrors());

		Properties p = new Properties();
		props.applyToCrshShellConfig(p);

		assertEquals("~/.ssh/test.pem", p.get("crash.auth.key.path"));
	}

	@Test
	public void testBindingKeyIgnored() {
		KeyAuthenticationProperties props = new KeyAuthenticationProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell.auth.key");
		binder.setConversionService(new DefaultConversionService());
		Map<String, String> map = new HashMap<String, String>();
		binder.bind(new MutablePropertyValues(map));
		assertFalse(binder.getBindingResult().hasErrors());

		Properties p = new Properties();
		props.applyToCrshShellConfig(p);

		assertNull(p.get("crash.auth.key.path"));
	}

	@Test
	public void testBindingSimple() {
		SimpleAuthenticationProperties props = new SimpleAuthenticationProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell.auth.simple");
		binder.setConversionService(new DefaultConversionService());
		Map<String, String> map = new HashMap<String, String>();
		map.put("shell.auth.simple.user.name", "username123");
		map.put("shell.auth.simple.user.password", "password123");
		binder.bind(new MutablePropertyValues(map));
		assertFalse(binder.getBindingResult().hasErrors());

		Properties p = new Properties();
		props.applyToCrshShellConfig(p);

		assertEquals("username123", p.get("crash.auth.simple.username"));
		assertEquals("password123", p.get("crash.auth.simple.password"));
	}

	@Test
	public void testDefaultPasswordAutogeneratedIfUnresolovedPlaceholder() {
		SimpleAuthenticationProperties security = new SimpleAuthenticationProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(security, "security");
		binder.bind(new MutablePropertyValues(Collections.singletonMap(
				"shell.auth.simple.user.password", "${ADMIN_PASSWORD}")));
		assertFalse(binder.getBindingResult().hasErrors());
		assertTrue(security.getUser().isDefaultPassword());
	}

	@Test
	public void testDefaultPasswordAutogeneratedIfEmpty() {
		SimpleAuthenticationProperties security = new SimpleAuthenticationProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(security, "security");
		binder.bind(new MutablePropertyValues(Collections.singletonMap(
				"shell.auth.simple.user.password", "")));
		assertFalse(binder.getBindingResult().hasErrors());
		assertTrue(security.getUser().isDefaultPassword());
	}

	@Test
	public void testBindingSpring() {
		SpringAuthenticationProperties props = new SpringAuthenticationProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell.auth.spring");
		binder.bind(new MutablePropertyValues(Collections.singletonMap(
				"shell.auth.spring.roles", "role1, role2")));
		assertFalse(binder.getBindingResult().hasErrors());

		Properties p = new Properties();
		props.applyToCrshShellConfig(p);

		assertEquals("role1, role2", p.get("crash.auth.spring.roles"));
	}

	@Test
	public void testCustomShellProperties() throws Exception {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("shell.auth", "simple");
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setEnvironment(env);
		context.setServletContext(new MockServletContext());
		context.register(TestShellConfiguration.class);
		context.register(CrshAutoConfiguration.class);
		context.refresh();

		PluginLifeCycle lifeCycle = context.getBean(PluginLifeCycle.class);
		String uuid = lifeCycle.getConfig().getProperty("test.uuid");
		assertEquals(TestShellConfiguration.uuid, uuid);
		context.close();
	}

	@Configuration
	public static class TestShellConfiguration {

		public static String uuid = UUID.randomUUID().toString();

		@Bean
		public CrshShellProperties testProperties() {
			return new CrshShellProperties() {

				@Override
				protected void applyToCrshShellConfig(Properties config) {
					config.put("test.uuid", uuid);
				}
			};
		}
	}
}
