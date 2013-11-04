/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.boot.actuate.properties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.boot.actuate.properties.CrshProperties.JaasAuthenticationProperties;
import org.springframework.boot.actuate.properties.CrshProperties.KeyAuthenticationProperties;
import org.springframework.boot.actuate.properties.CrshProperties.SimpleAuthenticationProperties;
import org.springframework.boot.actuate.properties.CrshProperties.SpringAuthenticationProperties;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link CrshProperties}.
 * 
 * @author Christian Dupuis
 */
public class CrshPropertiesTests {

	@Test
	public void testBindingAuth() {
		CrshProperties props = new CrshProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.bind(new MutablePropertyValues(Collections.singletonMap("shell.auth",
				"spring")));
		assertFalse(binder.getBindingResult().hasErrors());
		assertEquals("spring", props.getAuth());
	}

	@Test
	public void testBindingAuthIfEmpty() {
		CrshProperties props = new CrshProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.bind(new MutablePropertyValues(Collections.singletonMap("shell.auth", "")));
		assertTrue(binder.getBindingResult().hasErrors());
		assertEquals("simple", props.getAuth());
	}

	@Test
	public void testBindingCommandRefreshInterval() {
		CrshProperties props = new CrshProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		binder.bind(new MutablePropertyValues(Collections.singletonMap(
				"shell.command_refresh_interval", "1")));
		assertFalse(binder.getBindingResult().hasErrors());
		assertEquals(1, props.getCommandRefreshInterval());
	}

	@Test
	public void testBindingCommandPathPatterns() {
		CrshProperties props = new CrshProperties();
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
		CrshProperties props = new CrshProperties();
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
		CrshProperties props = new CrshProperties();
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
		CrshProperties props = new CrshProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		Map<String, String> map = new HashMap<String, String>();
		map.put("shell.ssh.enabled", "true");
		map.put("shell.ssh.port", "2222");
		map.put("shell.ssh.key_path", "~/.ssh/test.pem");
		binder.bind(new MutablePropertyValues(map));
		assertFalse(binder.getBindingResult().hasErrors());

		Properties p = new Properties();
		p = props.mergeProperties(p);

		assertEquals("2222", p.get(CrshProperties.CRASH_SSH_PORT));
		assertEquals("~/.ssh/test.pem", p.get(CrshProperties.CRASH_SSH_KEYPATH));
	}

	@Test
	public void testBindingSshIgnored() {
		CrshProperties props = new CrshProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		Map<String, String> map = new HashMap<String, String>();
		map.put("shell.ssh.enabled", "false");
		map.put("shell.ssh.port", "2222");
		map.put("shell.ssh.key_path", "~/.ssh/test.pem");
		binder.bind(new MutablePropertyValues(map));
		assertFalse(binder.getBindingResult().hasErrors());

		Properties p = new Properties();
		p = props.mergeProperties(p);

		assertNull(p.get(CrshProperties.CRASH_SSH_PORT));
		assertNull(p.get(CrshProperties.CRASH_SSH_KEYPATH));
	}

	@Test
	public void testBindingTelnet() {
		CrshProperties props = new CrshProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		Map<String, String> map = new HashMap<String, String>();
		map.put("shell.telnet.enabled", "true");
		map.put("shell.telnet.port", "2222");
		binder.bind(new MutablePropertyValues(map));
		assertFalse(binder.getBindingResult().hasErrors());

		Properties p = new Properties();
		p = props.mergeProperties(p);

		assertEquals("2222", p.get(CrshProperties.CRASH_TELNET_PORT));
	}

	@Test
	public void testBindingTelnetIgnored() {
		CrshProperties props = new CrshProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		Map<String, String> map = new HashMap<String, String>();
		map.put("shell.telnet.enabled", "false");
		map.put("shell.telnet.port", "2222");
		binder.bind(new MutablePropertyValues(map));
		assertFalse(binder.getBindingResult().hasErrors());

		Properties p = new Properties();
		p = props.mergeProperties(p);

		assertNull(p.get(CrshProperties.CRASH_TELNET_PORT));
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
		p = props.mergeProperties(p);

		assertEquals("my-test-domain", p.get(CrshProperties.CRASH_AUTH_JAAS_DOMAIN));
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
		p = props.mergeProperties(p);

		assertEquals("~/.ssh/test.pem", p.get(CrshProperties.CRASH_AUTH_KEY_PATH));
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
		p = props.mergeProperties(p);

		assertNull(p.get(CrshProperties.CRASH_AUTH_KEY_PATH));
	}

	@Test
	public void testBindingSimple() {
		SimpleAuthenticationProperties props = new SimpleAuthenticationProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell.auth.simple");
		binder.setConversionService(new DefaultConversionService());
		Map<String, String> map = new HashMap<String, String>();
		map.put("shell.auth.simple.username", "username123");
		map.put("shell.auth.simple.password", "password123");
		binder.bind(new MutablePropertyValues(map));
		assertFalse(binder.getBindingResult().hasErrors());

		Properties p = new Properties();
		p = props.mergeProperties(p);

		assertEquals("username123", p.get(CrshProperties.CRASH_AUTH_SIMPLE_USERNAME));
		assertEquals("password123", p.get(CrshProperties.CRASH_AUTH_SIMPLE_PASSWORD));
	}

	@Test
	public void testDefaultPasswordAutogeneratedIfUnresolovedPlaceholder() {
		SimpleAuthenticationProperties security = new SimpleAuthenticationProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(security, "security");
		binder.bind(new MutablePropertyValues(Collections.singletonMap(
				"shell.auth.simple.password", "${ADMIN_PASSWORD}")));
		assertFalse(binder.getBindingResult().hasErrors());
		assertTrue(security.isDefaultPassword());
	}

	@Test
	public void testDefaultPasswordAutogeneratedIfEmpty() {
		SimpleAuthenticationProperties security = new SimpleAuthenticationProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(security, "security");
		binder.bind(new MutablePropertyValues(Collections.singletonMap(
				"shell.auth.simple.password", "")));
		assertFalse(binder.getBindingResult().hasErrors());
		assertTrue(security.isDefaultPassword());
	}

	@Test
	public void testBindingSpring() {
		SpringAuthenticationProperties props = new SpringAuthenticationProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell.auth.spring");
		binder.bind(new MutablePropertyValues(Collections.singletonMap(
				"shell.auth.spring.roles", "role1, role2")));
		assertFalse(binder.getBindingResult().hasErrors());

		Properties p = new Properties();
		p = props.mergeProperties(p);

		assertEquals("role1, role2", p.get(CrshProperties.CRASH_AUTH_SPRING_ROLES));
	}

}
