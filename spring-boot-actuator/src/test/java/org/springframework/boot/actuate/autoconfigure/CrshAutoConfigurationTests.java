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

package org.springframework.boot.actuate.autoconfigure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.crsh.auth.AuthenticationPlugin;
import org.crsh.auth.JaasAuthenticationPlugin;
import org.crsh.lang.groovy.GroovyREPL;
import org.crsh.plugin.PluginContext;
import org.crsh.plugin.PluginLifeCycle;
import org.crsh.plugin.ResourceKind;
import org.crsh.processor.term.ProcessorIOHandler;
import org.crsh.vfs.Resource;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link CrshAutoConfiguration}.
 * 
 * @author Christian Dupuis
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CrshAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
			this.context = null;
		}
	}

	@Test
	public void testDisabledPlugins() throws Exception {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("shell.disabled_plugins",
				"GroovyREPL, termIOHandler, org.crsh.auth.AuthenticationPlugin");
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setEnvironment(env);
		this.context.register(CrshAutoConfiguration.class);
		this.context.refresh();

		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		assertNotNull(lifeCycle);

		assertNull(lifeCycle.getContext().getPlugin(GroovyREPL.class));
		assertNull(lifeCycle.getContext().getPlugin(ProcessorIOHandler.class));
		assertNull(lifeCycle.getContext().getPlugin(JaasAuthenticationPlugin.class));
	}

	@Test
	public void testAttributes() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(CrshAutoConfiguration.class);
		this.context.refresh();

		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);

		Map<String, Object> attributes = lifeCycle.getContext().getAttributes();
		assertTrue(attributes.containsKey("spring.version"));
		assertTrue(attributes.containsKey("spring.beanfactory"));
		assertEquals(this.context.getBeanFactory(), attributes.get("spring.beanfactory"));
	}

	@Test
	public void testSshConfiguration() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("shell.ssh.enabled", "true");
		env.setProperty("shell.ssh.port", "3333");
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setEnvironment(env);
		this.context.register(CrshAutoConfiguration.class);
		this.context.refresh();

		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);

		assertEquals(lifeCycle.getConfig().getProperty("crash.ssh.port"), "3333");
	}

	@Test
	public void testSshConfigurationWithKeyPath() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("shell.ssh.enabled", "true");
		env.setProperty("shell.ssh.key_path", "~/.ssh/id.pem");
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setEnvironment(env);
		this.context.register(CrshAutoConfiguration.class);
		this.context.refresh();

		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);

		assertEquals(lifeCycle.getConfig().getProperty("crash.ssh.keypath"),
				"~/.ssh/id.pem");
	}

	@Test
	public void testCommandResolution() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(CrshAutoConfiguration.class);
		this.context.refresh();

		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);

		int count = 0;
		Iterator<Resource> resources = lifeCycle.getContext()
				.loadResources("login", ResourceKind.LIFECYCLE).iterator();
		while (resources.hasNext()) {
			count++;
			resources.next();
		}
		assertEquals(1, count);

		count = 0;
		resources = lifeCycle.getContext()
				.loadResources("help.java", ResourceKind.COMMAND).iterator();
		while (resources.hasNext()) {
			count++;
			resources.next();
		}
		assertEquals(1, count);
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
		Iterator<AuthenticationPlugin> plugins = pluginContext.getPlugins(
				AuthenticationPlugin.class).iterator();
		while (plugins.hasNext()) {
			count++;
			plugins.next();
		}
		assertEquals(3, count);
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
		assertEquals(lifeCycle.getConfig().get("crash.auth"), "simple");
	}

	@Test
	public void testJaasAuthenticationProvider() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("shell.auth", "jaas");
		env.setProperty("shell.auth.jaas.domain", "my-test-domain");
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setEnvironment(env);
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityConfiguration.class);
		this.context.register(CrshAutoConfiguration.class);
		this.context.refresh();

		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		assertEquals(lifeCycle.getConfig().get("crash.auth"), "jaas");
		assertEquals(lifeCycle.getConfig().get("crash.auth.jaas.domain"),
				"my-test-domain");
	}

	@Test
	public void testKeyAuthenticationProvider() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("shell.auth", "key");
		env.setProperty("shell.auth.key.path", "~/test.pem");
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setEnvironment(env);
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityConfiguration.class);
		this.context.register(CrshAutoConfiguration.class);
		this.context.refresh();

		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		assertEquals(lifeCycle.getConfig().get("crash.auth"), "key");
		assertEquals(lifeCycle.getConfig().get("crash.auth.key.path"), "~/test.pem");
	}

	@Test
	public void testSimpleAuthenticationProvider() throws Exception {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("shell.auth", "simple");
		env.setProperty("shell.auth.simple.username", "user");
		env.setProperty("shell.auth.simple.password", "password");
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setEnvironment(env);
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityConfiguration.class);
		this.context.register(CrshAutoConfiguration.class);
		this.context.refresh();

		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);
		assertEquals(lifeCycle.getConfig().get("crash.auth"), "simple");

		AuthenticationPlugin<String> authenticationPlugin = null;
		String authentication = lifeCycle.getConfig().getProperty("crash.auth");
		assertNotNull(authentication);
		for (AuthenticationPlugin plugin : lifeCycle.getContext().getPlugins(
				AuthenticationPlugin.class)) {
			if (authentication.equals(plugin.getName())) {
				authenticationPlugin = plugin;
				break;
			}
		}
		assertNotNull(authenticationPlugin);
		assertTrue(authenticationPlugin.authenticate("user", "password"));
		assertFalse(authenticationPlugin.authenticate(UUID.randomUUID().toString(),
				"password"));
	}

	@Test
	public void testSpringAuthenticationProvider() throws Exception {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("shell.auth", "spring");
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setEnvironment(env);
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityConfiguration.class);
		this.context.register(CrshAutoConfiguration.class);
		this.context.refresh();

		PluginLifeCycle lifeCycle = this.context.getBean(PluginLifeCycle.class);

		AuthenticationPlugin<String> authenticationPlugin = null;
		String authentication = lifeCycle.getConfig().getProperty("crash.auth");
		assertNotNull(authentication);
		for (AuthenticationPlugin plugin : lifeCycle.getContext().getPlugins(
				AuthenticationPlugin.class)) {
			if (authentication.equals(plugin.getName())) {
				authenticationPlugin = plugin;
				break;
			}
		}
		assertTrue(authenticationPlugin.authenticate(SecurityConfiguration.USERNAME,
				SecurityConfiguration.PASSWORD));

		assertFalse(authenticationPlugin.authenticate(UUID.randomUUID().toString(),
				SecurityConfiguration.PASSWORD));
	}

	@Test
	public void testSpringAuthenticationProviderAsDefaultConfiguration() throws Exception {
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
		assertNotNull(authentication);
		for (AuthenticationPlugin plugin : lifeCycle.getContext().getPlugins(
				AuthenticationPlugin.class)) {
			if (authentication.equals(plugin.getName())) {
				authenticationPlugin = plugin;
				break;
			}
		}
		assertTrue(authenticationPlugin.authenticate(SecurityConfiguration.USERNAME,
				SecurityConfiguration.PASSWORD));

		assertFalse(authenticationPlugin.authenticate(UUID.randomUUID().toString(),
				SecurityConfiguration.PASSWORD));
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
								authentication.getCredentials(),
								Collections
										.singleton(new SimpleGrantedAuthority("ADMIN")));
					}
					else {
						throw new BadCredentialsException("Invalid username and password");
					}
					return authentication;
				}
			};
		}

		@Bean
		public AccessDecisionManager accessDecisionManager() {
			List<AccessDecisionVoter> voters = new ArrayList<AccessDecisionVoter>();
			RoleVoter voter = new RoleVoter();
			voter.setRolePrefix("");
			voters.add(voter);
			AccessDecisionManager result = new UnanimousBased(voters);
			return result;
		}
	}

}
