/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.Properties;
import java.util.UUID;

import org.crsh.plugin.PluginLifeCycle;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.actuate.autoconfigure.ShellProperties.CrshShellProperties;
import org.springframework.boot.actuate.autoconfigure.ShellProperties.JaasAuthenticationProperties;
import org.springframework.boot.actuate.autoconfigure.ShellProperties.KeyAuthenticationProperties;
import org.springframework.boot.actuate.autoconfigure.ShellProperties.SimpleAuthenticationProperties;
import org.springframework.boot.actuate.autoconfigure.ShellProperties.SpringAuthenticationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ShellProperties}.
 *
 * @author Christian Dupuis
 * @author Stephane Nicoll
 */
@Deprecated
public class ShellPropertiesTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testBindingAuth() {
		ShellProperties props = load(ShellProperties.class,
				"management.shell.auth.type=spring");
		assertThat(props.getAuth().getType()).isEqualTo("spring");
	}

	@Test
	public void testBindingAuthIfEmpty() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("Auth type must not be empty");
		load(ShellProperties.class, "management.shell.auth.type= ");
	}

	@Test
	public void testBindingCommandRefreshInterval() {
		ShellProperties props = load(ShellProperties.class,
				"management.shell.command-refresh-interval=1");
		assertThat(props.getCommandRefreshInterval()).isEqualTo(1);
	}

	@Test
	public void testBindingCommandPathPatterns() {
		ShellProperties props = load(ShellProperties.class,
				"management.shell.command-path-patterns=pattern1, pattern2");
		assertThat(props.getCommandPathPatterns().length).isEqualTo(2);
		Assert.assertArrayEquals(new String[] { "pattern1", "pattern2" },
				props.getCommandPathPatterns());
	}

	@Test
	public void testBindingConfigPathPatterns() {
		ShellProperties props = load(ShellProperties.class,
				"management.shell.config-path-patterns=pattern1, pattern2");
		assertThat(props.getConfigPathPatterns().length).isEqualTo(2);
		Assert.assertArrayEquals(new String[] { "pattern1", "pattern2" },
				props.getConfigPathPatterns());
	}

	@Test
	public void testBindingDisabledPlugins() {
		ShellProperties props = load(ShellProperties.class,
				"management.shell.disabled-plugins=pattern1, pattern2");
		assertThat(props.getDisabledPlugins().length).isEqualTo(2);
		assertThat(props.getDisabledPlugins()).containsExactly("pattern1", "pattern2");
	}

	@Test
	public void testBindingDisabledCommands() {
		ShellProperties props = load(ShellProperties.class,
				"management.shell.disabled-commands=pattern1, pattern2");
		assertThat(props.getDisabledCommands()).containsExactly("pattern1", "pattern2");
	}

	@Test
	public void testBindingSsh() {
		ShellProperties props = load(ShellProperties.class,
				"management.shell.ssh.enabled=true", "management.shell.ssh.port=2222",
				"management.shell.ssh.key-path=~/.ssh/test.pem");
		Properties p = props.asCrshShellConfig();
		assertThat(p.get("crash.ssh.port")).isEqualTo("2222");
		assertThat(p.get("crash.ssh.keypath")).isEqualTo("~/.ssh/test.pem");
	}

	@Test
	public void testBindingSshIgnored() {
		ShellProperties props = load(ShellProperties.class,
				"management.shell.ssh.enabled=false", "management.shell.ssh.port=2222",
				"management.shell.ssh.key-path=~/.ssh/test.pem");
		Properties p = props.asCrshShellConfig();
		assertThat(p.get("crash.ssh.port")).isNull();
		assertThat(p.get("crash.ssh.keypath")).isNull();
	}

	@Test
	public void testBindingTelnet() {
		ShellProperties props = load(ShellProperties.class,
				"management.shell.telnet.enabled=true",
				"management.shell.telnet.port=2222");
		Properties p = props.asCrshShellConfig();
		assertThat(p.get("crash.telnet.port")).isEqualTo("2222");
	}

	@Test
	public void testBindingTelnetIgnored() {
		ShellProperties props = load(ShellProperties.class,
				"management.shell.telnet.enabled=false",
				"management.shell.telnet.port=2222");
		Properties p = props.asCrshShellConfig();
		assertThat(p.get("crash.telnet.port")).isNull();
	}

	@Test
	public void testBindingJaas() {
		JaasAuthenticationProperties props = load(JaasAuthenticationProperties.class,
				"management.shell.auth.jaas.domain=my-test-domain");
		Properties p = new Properties();
		props.applyToCrshShellConfig(p);
		assertThat(p.get("crash.auth.jaas.domain")).isEqualTo("my-test-domain");
	}

	@Test
	public void testBindingKey() {
		KeyAuthenticationProperties props = load(KeyAuthenticationProperties.class,
				"management.shell.auth.key.path=~/.ssh/test.pem");
		Properties p = new Properties();
		props.applyToCrshShellConfig(p);
		assertThat(p.get("crash.auth.key.path")).isEqualTo("~/.ssh/test.pem");
	}

	@Test
	public void testBindingKeyIgnored() {
		KeyAuthenticationProperties props = load(KeyAuthenticationProperties.class);
		Properties p = new Properties();
		props.applyToCrshShellConfig(p);
		assertThat(p.get("crash.auth.key.path")).isNull();
	}

	@Test
	public void testBindingSimple() {
		SimpleAuthenticationProperties props = load(SimpleAuthenticationProperties.class,
				"management.shell.auth.simple.user.name=username123",
				"management.shell.auth.simple.user.password=password123");
		Properties p = new Properties();
		props.applyToCrshShellConfig(p);
		assertThat(p.get("crash.auth.simple.username")).isEqualTo("username123");
		assertThat(p.get("crash.auth.simple.password")).isEqualTo("password123");
	}

	@Test
	public void testDefaultPasswordAutoGeneratedIfUnresolvedPlaceholder() {
		SimpleAuthenticationProperties security = load(
				SimpleAuthenticationProperties.class,
				"management.shell.auth.simple.user.password=${ADMIN_PASSWORD}");
		assertThat(security.getUser().isDefaultPassword()).isTrue();
	}

	@Test
	public void testDefaultPasswordAutoGeneratedIfEmpty() {
		SimpleAuthenticationProperties security = load(
				SimpleAuthenticationProperties.class,
				"management.shell.auth.simple.user.password=");
		assertThat(security.getUser().isDefaultPassword()).isTrue();
	}

	@Test
	public void testBindingSpring() {
		SpringAuthenticationProperties props = load(SpringAuthenticationProperties.class,
				"management.shell.auth.spring.roles=role1,role2");
		Properties p = new Properties();
		props.applyToCrshShellConfig(p);
		assertThat(p.get("crash.auth.spring.roles")).isEqualTo("role1,role2");
	}

	@Test
	public void testCustomShellProperties() throws Exception {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("management.shell.auth.type", "simple");
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		ctx.setEnvironment(env);
		ctx.setServletContext(new MockServletContext());
		ctx.register(TestShellConfiguration.class);
		ctx.register(CrshAutoConfiguration.class);
		ctx.refresh();

		PluginLifeCycle lifeCycle = ctx.getBean(PluginLifeCycle.class);
		String uuid = lifeCycle.getConfig().getProperty("test.uuid");
		assertThat(uuid).isEqualTo(TestShellConfiguration.uuid);
		ctx.close();
	}

	private <T> T load(Class<T> type, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(ctx, environment);
		ctx.register(TestConfiguration.class);
		ctx.refresh();
		this.context = ctx;
		return this.context.getBean(type);
	}

	@SuppressWarnings("deprecation")
	@Configuration
	@EnableConfigurationProperties({ ShellProperties.class,
			JaasAuthenticationProperties.class, KeyAuthenticationProperties.class,
			SimpleAuthenticationProperties.class, SpringAuthenticationProperties.class })
	static class TestConfiguration {

	}

	@Configuration
	@Deprecated
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
