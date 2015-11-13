/*
 * Copyright 2013-2015 the original author or authors.
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
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for the shell subsystem.
 *
 * @author Christian Dupuis
 * @author Phillip Webb
 * @author Eddú Meléndez
 */
@ConfigurationProperties(prefix = "shell", ignoreUnknownFields = true)
public class ShellProperties {

	private static Log logger = LogFactory.getLog(ShellProperties.class);

	/**
	 * Authentication type. Auto-detected according to the environment (i.e. if Spring
	 * Security is available, "spring" is used by default).
	 */
	private String auth = "simple";

	private boolean defaultAuth = true;

	@Autowired(required = false)
	private CrshShellProperties[] additionalProperties = new CrshShellProperties[] {
			new SimpleAuthenticationProperties() };

	/**
	 * Scan for changes and update the command if necessary (in seconds).
	 */
	private int commandRefreshInterval = -1;

	/**
	 * Patterns to use to look for commands.
	 */
	private String[] commandPathPatterns = new String[] { "classpath*:/commands/**",
			"classpath*:/crash/commands/**" };

	/**
	 * Patterns to use to look for configurations.
	 */
	private String[] configPathPatterns = new String[] { "classpath*:/crash/*" };

	/**
	 * Comma-separated list of commands to disable.
	 */
	private String[] disabledCommands = new String[] { "jpa*", "jdbc*", "jndi*" };

	/**
	 * Comma-separated list of plugins to disable. Certain plugins are disabled by default
	 * based on the environment.
	 */
	private String[] disabledPlugins = new String[0];

	private final Ssh ssh = new Ssh();

	private final Telnet telnet = new Telnet();

	public void setAuth(String auth) {
		Assert.hasLength(auth, "Auth must not be empty");
		this.auth = auth;
		this.defaultAuth = false;
	}

	public String getAuth() {
		return this.auth;
	}

	public CrshShellProperties[] getAdditionalProperties() {
		return this.additionalProperties;
	}

	public void setCommandRefreshInterval(int commandRefreshInterval) {
		this.commandRefreshInterval = commandRefreshInterval;
	}

	public int getCommandRefreshInterval() {
		return this.commandRefreshInterval;
	}

	public void setCommandPathPatterns(String[] commandPathPatterns) {
		Assert.notEmpty(commandPathPatterns, "CommandPathPatterns must not be empty");
		this.commandPathPatterns = commandPathPatterns;
	}

	public String[] getCommandPathPatterns() {
		return this.commandPathPatterns;
	}

	public void setConfigPathPatterns(String[] configPathPatterns) {
		Assert.notEmpty(configPathPatterns, "ConfigPathPatterns must not be empty");
		this.configPathPatterns = configPathPatterns;
	}

	public String[] getConfigPathPatterns() {
		return this.configPathPatterns;
	}

	public void setDisabledCommands(String[] disabledCommands) {
		Assert.notEmpty(disabledCommands);
		this.disabledCommands = disabledCommands;
	}

	public String[] getDisabledCommands() {
		return this.disabledCommands;
	}

	public void setDisabledPlugins(String[] disabledPlugins) {
		Assert.notEmpty(disabledPlugins);
		this.disabledPlugins = disabledPlugins;
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

	/**
	 * Return a properties file configured from these settings that can be applied to a
	 * CRaSH shell instance.
	 * @return the CRaSH properties
	 */
	public Properties asCrshShellConfig() {
		Properties properties = new Properties();
		this.ssh.applyToCrshShellConfig(properties);
		this.telnet.applyToCrshShellConfig(properties);

		for (CrshShellProperties shellProperties : this.additionalProperties) {
			shellProperties.applyToCrshShellConfig(properties);
		}

		if (this.commandRefreshInterval > 0) {
			properties.put("crash.vfs.refresh_period",
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

		validateCrshShellConfig(properties);

		return properties;
	}

	/**
	 * Basic validation of applied CRaSH shell configuration.
	 * @param properties the properties to validate
	 */
	protected void validateCrshShellConfig(Properties properties) {
		String finalAuth = properties.getProperty("crash.auth");
		if (!this.defaultAuth && !this.auth.equals(finalAuth)) {
			logger.warn(String.format(
					"Shell authentication fell back to method '%s' opposed to "
							+ "configured method '%s'. Please check your classpath.",
					finalAuth, this.auth));
		}
		// Make sure we keep track of final authentication method
		this.auth = finalAuth;
	}

	/**
	 * Base class for CRaSH properties.
	 */
	public static abstract class CrshShellProperties {

		/**
		 * Apply the properties to a CRaSH configuration.
		 * @param config the CRaSH configuration properties
		 */
		protected abstract void applyToCrshShellConfig(Properties config);

	}

	/**
	 * Base class for Auth specific properties.
	 */
	public static abstract class CrshShellAuthenticationProperties
			extends CrshShellProperties {

	}

	/**
	 * SSH properties.
	 */
	public static class Ssh extends CrshShellProperties {

		/**
		 * Enable CRaSH SSH support.
		 */
		private boolean enabled = true;

		/**
		 * Path to the SSH server key.
		 */
		private String keyPath;

		/**
		 * SSH port.
		 */
		private Integer port = 2000;

		/**
		 * Number of milliseconds after user will be prompted to login again.
		 */
		private Integer authTimeout = 600000;

		/**
		 * Number of milliseconds after which unused connections are closed.
		 */
		private Integer idleTimeout = 600000;

		@Override
		protected void applyToCrshShellConfig(Properties config) {
			if (this.enabled) {
				config.put("crash.ssh.port", String.valueOf(this.port));
				config.put("crash.ssh.auth_timeout", String.valueOf(this.authTimeout));
				config.put("crash.ssh.idle_timeout", String.valueOf(this.idleTimeout));
				if (this.keyPath != null) {
					config.put("crash.ssh.keypath", this.keyPath);
				}
			}
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setKeyPath(String keyPath) {
			Assert.hasText(keyPath, "keyPath must have text");
			this.keyPath = keyPath;
		}

		public String getKeyPath() {
			return this.keyPath;
		}

		public void setPort(Integer port) {
			Assert.notNull(port, "port must not be null");
			this.port = port;
		}

		public Integer getPort() {
			return this.port;
		}

		public Integer getIdleTimeout() {
			return this.idleTimeout;
		}

		public void setIdleTimeout(Integer idleTimeout) {
			this.idleTimeout = idleTimeout;
		}

		public Integer getAuthTimeout() {
			return this.authTimeout;
		}

		public void setAuthTimeout(Integer authTimeout) {
			this.authTimeout = authTimeout;
		}
	}

	/**
	 * Telnet properties.
	 */
	public static class Telnet extends CrshShellProperties {

		/**
		 * Enable CRaSH telnet support. Enabled by default if the TelnetPlugin is
		 * available.
		 */
		private boolean enabled = ClassUtils.isPresent("org.crsh.telnet.TelnetPlugin",
				ClassUtils.getDefaultClassLoader());

		/**
		 * Telnet port.
		 */
		private Integer port = 5000;

		@Override
		protected void applyToCrshShellConfig(Properties config) {
			if (this.enabled) {
				config.put("crash.telnet.port", String.valueOf(this.port));
			}
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setPort(Integer port) {
			Assert.notNull(port, "port must not be null");
			this.port = port;
		}

		public Integer getPort() {
			return this.port;
		}

	}

	/**
	 * Auth specific properties for JAAS authentication.
	 */
	@ConfigurationProperties(prefix = "shell.auth.jaas", ignoreUnknownFields = false)
	public static class JaasAuthenticationProperties
			extends CrshShellAuthenticationProperties {

		/**
		 * JAAS domain.
		 */
		private String domain = "my-domain";

		@Override
		protected void applyToCrshShellConfig(Properties config) {
			config.put("crash.auth", "jaas");
			config.put("crash.auth.jaas.domain", this.domain);
		}

		public void setDomain(String domain) {
			Assert.hasText(domain, "domain must have text");
			this.domain = domain;
		}

		public String getDomain() {
			return this.domain;
		}

	}

	/**
	 * Auth specific properties for key authentication.
	 */
	@ConfigurationProperties(prefix = "shell.auth.key", ignoreUnknownFields = false)
	public static class KeyAuthenticationProperties
			extends CrshShellAuthenticationProperties {

		/**
		 * Path to the authentication key. This should point to a valid ".pem" file.
		 */
		private String path;

		@Override
		protected void applyToCrshShellConfig(Properties config) {
			config.put("crash.auth", "key");
			if (this.path != null) {
				config.put("crash.auth.key.path", this.path);
			}
		}

		public void setPath(String path) {
			Assert.hasText(path, "path must have text");
			this.path = path;
		}

		public String getPath() {
			return this.path;
		}

	}

	/**
	 * Auth specific properties for simple authentication.
	 */
	@ConfigurationProperties(prefix = "shell.auth.simple", ignoreUnknownFields = false)
	public static class SimpleAuthenticationProperties
			extends CrshShellAuthenticationProperties {

		private static Log logger = LogFactory
				.getLog(SimpleAuthenticationProperties.class);

		private User user = new User();

		@Override
		protected void applyToCrshShellConfig(Properties config) {
			config.put("crash.auth", "simple");
			config.put("crash.auth.simple.username", this.user.getName());
			config.put("crash.auth.simple.password", this.user.getPassword());
			if (this.user.isDefaultPassword()) {
				logger.info("\n\nUsing default password for shell access: "
						+ this.user.getPassword() + "\n\n");
			}
		}

		public User getUser() {
			return this.user;
		}

		public void setUser(User user) {
			this.user = user;
		}

		public static class User {

			/**
			 * Login user.
			 */
			private String name = "user";

			/**
			 * Login password.
			 */
			private String password = UUID.randomUUID().toString();

			private boolean defaultPassword = true;

			boolean isDefaultPassword() {
				return this.defaultPassword;
			}

			public String getName() {
				return this.name;
			}

			public String getPassword() {
				return this.password;
			}

			public void setName(String name) {
				Assert.hasLength(name, "name must have text");
				this.name = name;
			}

			public void setPassword(String password) {
				if (password.startsWith("${") && password.endsWith("}")
						|| !StringUtils.hasLength(password)) {
					return;
				}
				this.password = password;
				this.defaultPassword = false;
			}

		}

	}

	/**
	 * Auth specific properties for Spring authentication.
	 */
	@ConfigurationProperties(prefix = "shell.auth.spring", ignoreUnknownFields = false)
	public static class SpringAuthenticationProperties
			extends CrshShellAuthenticationProperties {

		/**
		 * Comma-separated list of required roles to login to the CRaSH console.
		 */
		private String[] roles = new String[] { "ADMIN" };

		@Override
		protected void applyToCrshShellConfig(Properties config) {
			config.put("crash.auth", "spring");
			config.put("crash.auth.spring.roles",
					StringUtils.arrayToCommaDelimitedString(this.roles));
		}

		public void setRoles(String[] roles) {
			// 'roles' can be null. This means no special to access right to connect to
			// shell is required.
			this.roles = roles;
		}

		public String[] getRoles() {
			return this.roles;
		}

	}

}
