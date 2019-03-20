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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.crsh.auth.AuthenticationPlugin;
import org.crsh.auth.JaasAuthenticationPlugin;
import org.crsh.lang.impl.java.JavaLanguage;
import org.crsh.lang.spi.Language;
import org.crsh.plugin.PluginContext;
import org.crsh.plugin.PluginLifeCycle;
import org.crsh.plugin.ResourceKind;
import org.crsh.telnet.term.processor.ProcessorIOHandler;
import org.crsh.telnet.term.spi.TermIOHandler;
import org.crsh.vfs.Resource;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.boot.testutil.Matched;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.access.vote.UnanimousBased;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.isA;

/**
 * Tests for {@link CrshAutoConfiguration}.
 *
 * @author Christian Dupuis
 * @author Andreas Ahlenstorf
 * @author Eddú Meléndez
 * @author Matt Benson
 * @author Stephane Nicoll
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Deprecated
public class CrshAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDisabledPlugins() throws Exception {
		load("management.shell.disabled_plugins="
				+ "termIOHandler, org.crsh.auth.AuthenticationPlugin, javaLanguage");
		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		assertThat(lifeCycle).isNotNull();
		assertThat(lifeCycle.getContext().getPlugins(TermIOHandler.class))
				.filteredOn(Matched.<TermIOHandler>when(isA(ProcessorIOHandler.class)))
				.isEmpty();
		assertThat(lifeCycle.getContext().getPlugins(AuthenticationPlugin.class))
				.filteredOn(Matched
						.<AuthenticationPlugin>when(isA(JaasAuthenticationPlugin.class)))
				.isEmpty();
		assertThat(lifeCycle.getContext().getPlugins(Language.class))
				.filteredOn(Matched.<Language>when(isA(JavaLanguage.class))).isEmpty();
	}

	@Test
	public void testAttributes() throws Exception {
		load();
		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		Map<String, Object> attributes = lifeCycle.getContext().getAttributes();
		assertThat(attributes.containsKey("spring.version")).isTrue();
		assertThat(attributes.containsKey("spring.beanfactory")).isTrue();
		assertThat(attributes.get("spring.beanfactory"))
				.isEqualTo(this.context.getBeanFactory());
	}

	@Test
	public void testSshConfiguration() {
		load("management.shell.ssh.enabled=true", "management.shell.ssh.port=3333");
		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		assertThat(lifeCycle.getConfig().getProperty("crash.ssh.port")).isEqualTo("3333");
		assertThat(lifeCycle.getConfig().getProperty("crash.ssh.auth_timeout"))
				.isEqualTo("600000");
		assertThat(lifeCycle.getConfig().getProperty("crash.ssh.idle_timeout"))
				.isEqualTo("600000");
	}

	@Test
	public void testSshConfigurationWithKeyPath() {
		load("management.shell.ssh.enabled=true",
				"management.shell.ssh.key_path=~/.ssh/id.pem");
		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		assertThat(lifeCycle.getConfig().getProperty("crash.ssh.keypath"))
				.isEqualTo("~/.ssh/id.pem");
	}

	@Test
	public void testSshConfigurationCustomTimeouts() {
		load("management.shell.ssh.enabled=true",
				"management.shell.ssh.auth-timeout=300000",
				"management.shell.ssh.idle-timeout=400000");
		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		assertThat(lifeCycle.getConfig().getProperty("crash.ssh.auth_timeout"))
				.isEqualTo("300000");
		assertThat(lifeCycle.getConfig().getProperty("crash.ssh.idle_timeout"))
				.isEqualTo("400000");
	}

	@Test
	public void testCommandResolution() {
		load();
		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		int count = 0;
		Iterator<Resource> resources = lifeCycle.getContext()
				.loadResources("login", ResourceKind.LIFECYCLE).iterator();
		while (resources.hasNext()) {
			count++;
			resources.next();
		}
		assertThat(count).isEqualTo(1);
		count = 0;
		resources = lifeCycle.getContext()
				.loadResources("sleep.groovy", ResourceKind.COMMAND).iterator();
		while (resources.hasNext()) {
			count++;
			resources.next();
		}
		assertThat(count).isEqualTo(1);
	}

	@Test
	public void testDisabledCommandResolution() {
		load();
		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		int count = 0;
		Iterator<Resource> resources = lifeCycle.getContext()
				.loadResources("jdbc.groovy", ResourceKind.COMMAND).iterator();
		while (resources.hasNext()) {
			count++;
			resources.next();
		}
		assertThat(count).isEqualTo(0);
	}

	@Test
	public void testAuthenticationProvidersAreInstalled() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityConfiguration.class);
		this.context.register(CrshAutoConfiguration.class);
		this.context.refresh();
		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		PluginContext pluginContext = lifeCycle.getContext();
		int count = 0;
		Iterator<AuthenticationPlugin> plugins = pluginContext
				.getPlugins(AuthenticationPlugin.class).iterator();
		while (plugins.hasNext()) {
			count++;
			plugins.next();
		}
		assertThat(count).isEqualTo(3);
	}

	@Test
	public void testDefaultAuthenticationProvider() {
		MockEnvironment env = new MockEnvironment();
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setEnvironment(env);
		this.context.setServletContext(new MockServletContext());
		this.context.register(CrshAutoConfiguration.class);
		this.context.refresh();
		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		assertThat(lifeCycle.getConfig().get("crash.auth")).isEqualTo("simple");
	}

	@Test
	public void testJaasAuthenticationProvider() {
		this.context = new AnnotationConfigWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.shell.auth.type=jaas",
				"management.shell.auth.jaas.domain=my-test-domain");
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityConfiguration.class);
		this.context.register(CrshAutoConfiguration.class);
		this.context.refresh();
		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		assertThat(lifeCycle.getConfig().get("crash.auth")).isEqualTo("jaas");
		assertThat(lifeCycle.getConfig().get("crash.auth.jaas.domain"))
				.isEqualTo("my-test-domain");
	}

	@Test
	public void testKeyAuthenticationProvider() {
		this.context = new AnnotationConfigWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.shell.auth.type=key",
				"management.shell.auth.key.path=~/test.pem");
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityConfiguration.class);
		this.context.register(CrshAutoConfiguration.class);
		this.context.refresh();
		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		assertThat(lifeCycle.getConfig().get("crash.auth")).isEqualTo("key");
		assertThat(lifeCycle.getConfig().get("crash.auth.key.path"))
				.isEqualTo("~/test.pem");
	}

	@Test
	public void testSimpleAuthenticationProvider() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.shell.auth.type=simple",
				"management.shell.auth.simple.user.name=user",
				"management.shell.auth.simple.user.password=password");
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityConfiguration.class);
		this.context.register(CrshAutoConfiguration.class);
		this.context.refresh();
		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		assertThat(lifeCycle.getConfig().get("crash.auth")).isEqualTo("simple");
		AuthenticationPlugin<String> authenticationPlugin = null;
		String authentication = lifeCycle.getConfig().getProperty("crash.auth");
		assertThat(authentication).isNotNull();
		for (AuthenticationPlugin plugin : lifeCycle.getContext()
				.getPlugins(AuthenticationPlugin.class)) {
			if (authentication.equals(plugin.getName())) {
				authenticationPlugin = plugin;
				break;
			}
		}
		assertThat(authenticationPlugin).isNotNull();
		assertThat(authenticationPlugin.authenticate("user", "password")).isTrue();
		assertThat(authenticationPlugin.authenticate(UUID.randomUUID().toString(),
				"password")).isFalse();
	}

	@Test
	public void testSpringAuthenticationProvider() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.shell.auth.type=spring");
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityConfiguration.class);
		this.context.register(CrshAutoConfiguration.class);
		this.context.refresh();
		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		AuthenticationPlugin<String> authenticationPlugin = null;
		String authentication = lifeCycle.getConfig().getProperty("crash.auth");
		assertThat(authentication).isNotNull();
		for (AuthenticationPlugin plugin : lifeCycle.getContext()
				.getPlugins(AuthenticationPlugin.class)) {
			if (authentication.equals(plugin.getName())) {
				authenticationPlugin = plugin;
				break;
			}
		}
		assertThat(authenticationPlugin.authenticate(SecurityConfiguration.USERNAME,
				SecurityConfiguration.PASSWORD)).isTrue();
		assertThat(authenticationPlugin.authenticate(UUID.randomUUID().toString(),
				SecurityConfiguration.PASSWORD)).isFalse();
	}

	@Test
	public void testSpringAuthenticationProviderAsDefaultConfiguration()
			throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(ManagementServerPropertiesAutoConfiguration.class);
		this.context.register(SecurityAutoConfiguration.class);
		this.context.register(SecurityConfiguration.class);
		this.context.register(CrshAutoConfiguration.class);
		this.context.refresh();
		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		AuthenticationPlugin<String> authenticationPlugin = null;
		String authentication = lifeCycle.getConfig().getProperty("crash.auth");
		assertThat(authentication).isNotNull();
		for (AuthenticationPlugin plugin : lifeCycle.getContext()
				.getPlugins(AuthenticationPlugin.class)) {
			if (authentication.equals(plugin.getName())) {
				authenticationPlugin = plugin;
				break;
			}
		}
		assertThat(authenticationPlugin.authenticate(SecurityConfiguration.USERNAME,
				SecurityConfiguration.PASSWORD)).isTrue();
		assertThat(authenticationPlugin.authenticate(UUID.randomUUID().toString(),
				SecurityConfiguration.PASSWORD)).isFalse();
	}

	private void load(String... environment) {
		this.context = new AnnotationConfigWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, environment);
		this.context.register(CrshAutoConfiguration.class);
		this.context.refresh();
	}

	@Configuration
	public static class SecurityConfiguration {

		public static final String USERNAME = UUID.randomUUID().toString();

		public static final String PASSWORD = UUID.randomUUID().toString();

		@Bean
		public AuthenticationManager authenticationManager() {
			return new AuthenticationManager() {

				@Override
				public Authentication authenticate(Authentication authentication)
						throws AuthenticationException {
					if (authentication.getName().equals(USERNAME)
							&& authentication.getCredentials().equals(PASSWORD)) {
						authentication = new UsernamePasswordAuthenticationToken(
								authentication.getPrincipal(),
								authentication.getCredentials(), Collections.singleton(
										new SimpleGrantedAuthority("ACTUATOR")));
					}
					else {
						throw new BadCredentialsException(
								"Invalid username and password");
					}
					return authentication;
				}
			};
		}

		@Bean
		public AccessDecisionManager shellAccessDecisionManager() {
			List<AccessDecisionVoter<?>> voters = new ArrayList<AccessDecisionVoter<?>>();
			RoleVoter voter = new RoleVoter();
			voter.setRolePrefix("");
			voters.add(voter);
			return new UnanimousBased(voters);
		}

	}

}
