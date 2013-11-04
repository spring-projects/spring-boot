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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for the shell subsystem.
 * 
 * @author Christian Dupuis
 */
@ConfigurationProperties(name = "shell", ignoreUnknownFields = true)
public class CrshProperties {

	protected static final String CRASH_AUTH = "crash.auth";
	protected static final String CRASH_AUTH_JAAS_DOMAIN = "crash.auth.jaas.domain";
	protected static final String CRASH_AUTH_KEY_PATH = "crash.auth.key.path";
	protected static final String CRASH_AUTH_SIMPLE_PASSWORD = "crash.auth.simple.password";
	protected static final String CRASH_AUTH_SIMPLE_USERNAME = "crash.auth.simple.username";
	protected static final String CRASH_AUTH_SPRING_ROLES = "crash.auth.spring.roles";
	protected static final String CRASH_SSH_KEYPATH = "crash.ssh.keypath";
	protected static final String CRASH_SSH_PORT = "crash.ssh.port";
	protected static final String CRASH_TELNET_PORT = "crash.telnet.port";
	protected static final String CRASH_VFS_REFRESH_PERIOD = "crash.vfs.refresh_period";

	private String auth = "simple";

	@Autowired(required = false)
	private AuthenticationProperties authenticationProperties;

	private int commandRefreshInterval = -1;

	private String[] commandPathPatterns = new String[] { "classpath*:/commands/**",
			"classpath*:/crash/commands/**" };

	private String[] configPathPatterns = new String[] { "classpath*:/crash/*" };

	private String[] disabledPlugins = new String[0];

	private Ssh ssh = new Ssh();

	private Telnet telnet = new Telnet();

	public String getAuth() {
		return this.auth;
	}

	public AuthenticationProperties getAuthenticationProperties() {
		return this.authenticationProperties;
	}

	public int getCommandRefreshInterval() {
		return this.commandRefreshInterval;
	}

	public String[] getCommandPathPatterns() {
		return this.commandPathPatterns;
	}

	public String[] getConfigPathPatterns() {
		return this.configPathPatterns;
	}

	public String[] getDisabledPlugins() {
		return this.disabledPlugins;
	}

	public Ssh getSsh() {
		return this.ssh;
	}

	public Telnet getTelnet() {
		return this.telnet;
	}

	public Properties mergeProperties(Properties properties) {
		properties = this.ssh.mergeProperties(properties);
		properties = this.telnet.mergeProperties(properties);

		properties.put(CRASH_AUTH, this.auth);
		if (this.authenticationProperties != null) {
			properties = this.authenticationProperties.mergeProperties(properties);
		}

		if (this.commandRefreshInterval > 0) {
			properties.put(CRASH_VFS_REFRESH_PERIOD,
					String.valueOf(this.commandRefreshInterval));
		}

		// special handling for disabling Ssh and Telnet support
		List<String> dp = new ArrayList<String>(Arrays.asList(this.disabledPlugins));
		if (!this.ssh.isEnabled()) {
			dp.add("org.crsh.ssh.SSHPlugin");
		}
		if (!this.telnet.isEnabled()) {
			dp.add("org.crsh.telnet.TelnetPlugin");
		}
		this.disabledPlugins = dp.toArray(new String[dp.size()]);

		return properties;
	}

	public void setAuth(String auth) {
		Assert.hasLength(auth);
		this.auth = auth;
	}

	public void setAuthenticationProperties(
			AuthenticationProperties authenticationProperties) {
		Assert.notNull(authenticationProperties);
		this.authenticationProperties = authenticationProperties;
	}

	public void setCommandRefreshInterval(int commandRefreshInterval) {
		this.commandRefreshInterval = commandRefreshInterval;
	}

	public void setCommandPathPatterns(String[] commandPathPatterns) {
		Assert.notEmpty(commandPathPatterns);
		this.commandPathPatterns = commandPathPatterns;
	}

	public void setConfigPathPatterns(String[] configPathPatterns) {
		Assert.notEmpty(configPathPatterns);
		this.configPathPatterns = configPathPatterns;
	}

	public void setDisabledPlugins(String[] disabledPlugins) {
		Assert.notEmpty(disabledPlugins);
		this.disabledPlugins = disabledPlugins;
	}

	public void setSsh(Ssh ssh) {
		Assert.notNull(ssh);
		this.ssh = ssh;
	}

	public void setTelnet(Telnet telnet) {
		Assert.notNull(telnet);
		this.telnet = telnet;
	}

	public interface AuthenticationProperties extends PropertiesProvider {
	}

	@ConfigurationProperties(name = "shell.auth.jaas", ignoreUnknownFields = false)
	public static class JaasAuthenticationProperties implements AuthenticationProperties {

		private String domain = "my-domain";

		@Override
		public Properties mergeProperties(Properties properties) {
			properties.put(CRASH_AUTH_JAAS_DOMAIN, this.domain);
			return properties;
		}

		public void setDomain(String domain) {
			Assert.hasText(domain);
			this.domain = domain;
		}

	}

	@ConfigurationProperties(name = "shell.auth.key", ignoreUnknownFields = false)
	public static class KeyAuthenticationProperties implements AuthenticationProperties {

		private String path;

		@Override
		public Properties mergeProperties(Properties properties) {
			if (this.path != null) {
				properties.put(CRASH_AUTH_KEY_PATH, this.path);
			}
			return properties;
		}

		public void setPath(String path) {
			Assert.hasText(path);
			this.path = path;
		}

	}

	public interface PropertiesProvider {
		Properties mergeProperties(Properties properties);
	}

	@ConfigurationProperties(name = "shell.auth.simple", ignoreUnknownFields = false)
	public static class SimpleAuthenticationProperties implements
			AuthenticationProperties {

		private static Log logger = LogFactory
				.getLog(SimpleAuthenticationProperties.class);

		private String username = "user";

		private String password = UUID.randomUUID().toString();

		private boolean defaultPassword = true;

		public boolean isDefaultPassword() {
			return this.defaultPassword;
		}

		@Override
		public Properties mergeProperties(Properties properties) {
			properties.put(CRASH_AUTH_SIMPLE_USERNAME, this.username);
			properties.put(CRASH_AUTH_SIMPLE_PASSWORD, this.password);
			if (this.defaultPassword) {
				logger.info("Using default password for shell access: " + this.password);
			}
			return properties;
		}

		public void setPassword(String password) {
			if (password.startsWith("${") && password.endsWith("}")
					|| !StringUtils.hasLength(password)) {
				return;
			}
			this.password = password;
			this.defaultPassword = false;
		}

		public void setUsername(String username) {
			Assert.hasLength(username);
			this.username = username;
		}

	}

	@ConfigurationProperties(name = "shell.auth.spring", ignoreUnknownFields = false)
	public static class SpringAuthenticationProperties implements
			AuthenticationProperties {

		private String[] roles = new String[] { "ROLE_ADMIN" };

		@Override
		public Properties mergeProperties(Properties properties) {
			if (this.roles != null) {
				properties.put(CRASH_AUTH_SPRING_ROLES,
						StringUtils.arrayToCommaDelimitedString(this.roles));
			}
			return properties;
		}

		public void setRoles(String[] roles) {
			Assert.notNull(roles);
			this.roles = roles;
		}

	}

	public static class Ssh implements PropertiesProvider {

		private boolean enabled = true;

		private String keyPath = null;

		private String port = "2000";

		public boolean isEnabled() {
			return this.enabled;
		}

		@Override
		public Properties mergeProperties(Properties properties) {
			if (this.enabled) {
				properties.put(CRASH_SSH_PORT, this.port);
				if (this.keyPath != null) {
					properties.put(CRASH_SSH_KEYPATH, this.keyPath);
				}
			}
			return properties;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public void setKeyPath(String keyPath) {
			Assert.hasText(keyPath);
			this.keyPath = keyPath;
		}

		public void setPort(Integer port) {
			Assert.notNull(port);
			this.port = port.toString();
		}

	}

	public static class Telnet implements PropertiesProvider {

		private boolean enabled = false;

		private String port = "5000";

		public boolean isEnabled() {
			return this.enabled;
		}

		@Override
		public Properties mergeProperties(Properties properties) {
			if (this.enabled) {
				properties.put(CRASH_TELNET_PORT, this.port);
			}
			return properties;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public void setPort(Integer port) {
			Assert.notNull(port);
			this.port = port.toString();
		}

	}

}
