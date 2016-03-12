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

import static org.assertj.core.api.Assertions.assertThat;

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
		binder.bind(new MutablePropertyValues(
				Collections.singletonMap("shell.auth", "spring")));
		assertThat(binder.getBindingResult().hasErrors()).isFalse();
		assertThat(props.getAuth()).isEqualTo("spring");
	}

	@Test
	public void testBindingAuthIfEmpty() {
		ShellProperties props = new ShellProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.bind(
				new MutablePropertyValues(Collections.singletonMap("shell.auth", "")));
		assertThat(binder.getBindingResult().hasErrors()).isTrue();
		assertThat(props.getAuth()).isEqualTo("simple");
	}

	@Test
	public void testBindingCommandRefreshInterval() {
		ShellProperties props = new ShellProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		binder.bind(new MutablePropertyValues(
				Collections.singletonMap("shell.command_refresh_interval", "1")));
		assertThat(binder.getBindingResult().hasErrors()).isFalse();
		assertThat(props.getCommandRefreshInterval()).isEqualTo(1);
	}

	@Test
	public void testBindingCommandPathPatterns() {
		ShellProperties props = new ShellProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		binder.bind(new MutablePropertyValues(Collections
				.singletonMap("shell.command_path_patterns", "pattern1, pattern2")));
		assertThat(binder.getBindingResult().hasErrors()).isFalse();
		assertThat(props.getCommandPathPatterns().length).isEqualTo(2);
		Assert.assertArrayEquals(new String[] { "pattern1", "pattern2" },
				props.getCommandPathPatterns());
	}

	@Test
	public void testBindingConfigPathPatterns() {
		ShellProperties props = new ShellProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		binder.bind(new MutablePropertyValues(Collections
				.singletonMap("shell.config_path_patterns", "pattern1, pattern2")));
		assertThat(binder.getBindingResult().hasErrors()).isFalse();
		assertThat(props.getConfigPathPatterns().length).isEqualTo(2);
		Assert.assertArrayEquals(new String[] { "pattern1", "pattern2" },
				props.getConfigPathPatterns());
	}

	@Test
	public void testBindingDisabledPlugins() {
		ShellProperties props = new ShellProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		binder.bind(new MutablePropertyValues(Collections
				.singletonMap("shell.disabled_plugins", "pattern1, pattern2")));
		assertThat(binder.getBindingResult().hasErrors()).isFalse();
		assertThat(props.getDisabledPlugins().length).isEqualTo(2);
		assertThat(props.getDisabledPlugins()).containsExactly("pattern1", "pattern2");
	}

	@Test
	public void testBindingDisabledCommands() {
		ShellProperties props = new ShellProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell");
		binder.setConversionService(new DefaultConversionService());
		binder.bind(new MutablePropertyValues(Collections
				.singletonMap("shell.disabled_commands", "pattern1, pattern2")));
		assertThat(binder.getBindingResult().hasErrors()).isFalse();
		assertThat(props.getDisabledCommands()).containsExactly("pattern1", "pattern2");
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
		assertThat(binder.getBindingResult().hasErrors()).isFalse();

		Properties p = props.asCrshShellConfig();

		assertThat(p.get("crash.ssh.port")).isEqualTo("2222");
		assertThat(p.get("crash.ssh.keypath")).isEqualTo("~/.ssh/test.pem");
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
		assertThat(binder.getBindingResult().hasErrors()).isFalse();

		Properties p = props.asCrshShellConfig();

		assertThat(p.get("crash.ssh.port")).isNull();
		assertThat(p.get("crash.ssh.keypath")).isNull();
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
		assertThat(binder.getBindingResult().hasErrors()).isFalse();

		Properties p = props.asCrshShellConfig();

		assertThat(p.get("crash.telnet.port")).isEqualTo("2222");
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
		assertThat(binder.getBindingResult().hasErrors()).isFalse();

		Properties p = props.asCrshShellConfig();

		assertThat(p.get("crash.telnet.port")).isNull();
	}

	@Test
	public void testBindingJaas() {
		JaasAuthenticationProperties props = new JaasAuthenticationProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell.auth.jaas");
		binder.setConversionService(new DefaultConversionService());
		Map<String, String> map = new HashMap<String, String>();
		map.put("shell.auth.jaas.domain", "my-test-domain");
		binder.bind(new MutablePropertyValues(map));
		assertThat(binder.getBindingResult().hasErrors()).isFalse();

		Properties p = new Properties();
		props.applyToCrshShellConfig(p);

		assertThat(p.get("crash.auth.jaas.domain")).isEqualTo("my-test-domain");
	}

	@Test
	public void testBindingKey() {
		KeyAuthenticationProperties props = new KeyAuthenticationProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell.auth.key");
		binder.setConversionService(new DefaultConversionService());
		Map<String, String> map = new HashMap<String, String>();
		map.put("shell.auth.key.path", "~/.ssh/test.pem");
		binder.bind(new MutablePropertyValues(map));
		assertThat(binder.getBindingResult().hasErrors()).isFalse();

		Properties p = new Properties();
		props.applyToCrshShellConfig(p);

		assertThat(p.get("crash.auth.key.path")).isEqualTo("~/.ssh/test.pem");
	}

	@Test
	public void testBindingKeyIgnored() {
		KeyAuthenticationProperties props = new KeyAuthenticationProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell.auth.key");
		binder.setConversionService(new DefaultConversionService());
		Map<String, String> map = new HashMap<String, String>();
		binder.bind(new MutablePropertyValues(map));
		assertThat(binder.getBindingResult().hasErrors()).isFalse();

		Properties p = new Properties();
		props.applyToCrshShellConfig(p);

		assertThat(p.get("crash.auth.key.path")).isNull();
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
		assertThat(binder.getBindingResult().hasErrors()).isFalse();

		Properties p = new Properties();
		props.applyToCrshShellConfig(p);

		assertThat(p.get("crash.auth.simple.username")).isEqualTo("username123");
		assertThat(p.get("crash.auth.simple.password")).isEqualTo("password123");
	}

	@Test
	public void testDefaultPasswordAutogeneratedIfUnresolvedPlaceholder() {
		SimpleAuthenticationProperties security = new SimpleAuthenticationProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(security, "security");
		binder.bind(new MutablePropertyValues(Collections
				.singletonMap("shell.auth.simple.user.password", "${ADMIN_PASSWORD}")));
		assertThat(binder.getBindingResult().hasErrors()).isFalse();
		assertThat(security.getUser().isDefaultPassword()).isTrue();
	}

	@Test
	public void testDefaultPasswordAutogeneratedIfEmpty() {
		SimpleAuthenticationProperties security = new SimpleAuthenticationProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(security, "security");
		binder.bind(new MutablePropertyValues(
				Collections.singletonMap("shell.auth.simple.user.password", "")));
		assertThat(binder.getBindingResult().hasErrors()).isFalse();
		assertThat(security.getUser().isDefaultPassword()).isTrue();
	}

	@Test
	public void testBindingSpring() {
		SpringAuthenticationProperties props = new SpringAuthenticationProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(props, "shell.auth.spring");
		binder.bind(new MutablePropertyValues(
				Collections.singletonMap("shell.auth.spring.roles", "role1, role2")));
		assertThat(binder.getBindingResult().hasErrors()).isFalse();

		Properties p = new Properties();
		props.applyToCrshShellConfig(p);

		assertThat(p.get("crash.auth.spring.roles")).isEqualTo("role1,role2");
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
		assertThat(uuid).isEqualTo(TestShellConfiguration.uuid);
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
