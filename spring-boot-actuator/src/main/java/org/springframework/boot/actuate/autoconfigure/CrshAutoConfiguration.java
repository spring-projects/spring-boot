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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.crsh.auth.AuthenticationPlugin;
import org.crsh.plugin.CRaSHPlugin;
import org.crsh.plugin.PluginContext;
import org.crsh.plugin.PluginDiscovery;
import org.crsh.plugin.PluginLifeCycle;
import org.crsh.plugin.PropertyDescriptor;
import org.crsh.plugin.ServiceLoaderDiscovery;
import org.crsh.vfs.FS;
import org.crsh.vfs.spi.AbstractFSDriver;
import org.crsh.vfs.spi.FSDriver;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.ShellProperties.CrshShellAuthenticationProperties;
import org.springframework.boot.actuate.autoconfigure.ShellProperties.CrshShellProperties;
import org.springframework.boot.actuate.autoconfigure.ShellProperties.JaasAuthenticationProperties;
import org.springframework.boot.actuate.autoconfigure.ShellProperties.KeyAuthenticationProperties;
import org.springframework.boot.actuate.autoconfigure.ShellProperties.SimpleAuthenticationProperties;
import org.springframework.boot.actuate.autoconfigure.ShellProperties.SpringAuthenticationProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.SpringVersion;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for embedding an extensible shell
 * into a Spring Boot enabled application. By default a SSH daemon is started on port
 * 2000. If the CRaSH Telnet plugin is available on the classpath, Telnet daemon will be
 * launched on port 5000.
 * <p>
 * The default shell authentication method uses a username and password combination. If no
 * configuration is provided the default username is 'user' and the password will be
 * printed to console during application startup. Those default values can be overridden
 * by using <code>shell.auth.simple.username</code> and
 * <code>shell.auth.simple.password</code>.
 * <p>
 * If a Spring Security {@link AuthenticationManager} is detected, this configuration will
 * create a {@link CRaSHPlugin} to forward shell authentication requests to Spring
 * Security. This authentication method will get enabled if <code>shell.auth</code> is set
 * to <code>spring</code> or if no explicit <code>shell.auth</code> is provided and a
 * {@link AuthenticationManager} is available. In the latter case shell access will be
 * restricted to users having roles that match those configured in
 * {@link ManagementServerProperties}. Required roles can be overridden by
 * <code>shell.auth.spring.roles</code>.
 * <p>
 * To add customizations to the shell simply define beans of type {@link CRaSHPlugin} in
 * the application context. Those beans will get auto detected during startup and
 * registered with the underlying shell infrastructure. To configure plugins and the CRaSH
 * infrastructure add beans of type {@link CrshShellProperties} to the application
 * context.
 * <p>
 * Additional shell commands can be implemented using the guide and documentation at <a
 * href="http://www.crashub.org">crashub.org</a>. By default Boot will search for commands
 * using the following classpath scanning pattern <code>classpath*:/commands/**</code>. To
 * add different locations or override the default use
 * <code>shell.command_path_patterns</code> in your application configuration.
 * 
 * @author Christian Dupuis
 * @see ShellProperties
 */
@Configuration
@ConditionalOnClass({ PluginLifeCycle.class })
@EnableConfigurationProperties({ ShellProperties.class })
@AutoConfigureAfter({ SecurityAutoConfiguration.class,
		ManagementSecurityAutoConfiguration.class })
public class CrshAutoConfiguration {

	@Autowired
	private ShellProperties properties;

	@Bean
	@ConditionalOnExpression("'${shell.auth:simple}' == 'jaas'")
	@ConditionalOnMissingBean({ CrshShellAuthenticationProperties.class })
	public CrshShellAuthenticationProperties jaasAuthenticationProperties() {
		return new JaasAuthenticationProperties();
	}

	@Bean
	@ConditionalOnExpression("'${shell.auth:simple}' == 'key'")
	@ConditionalOnMissingBean({ CrshShellAuthenticationProperties.class })
	public CrshShellAuthenticationProperties keyAuthenticationProperties() {
		return new KeyAuthenticationProperties();
	}

	@Bean
	@ConditionalOnExpression("'${shell.auth:simple}' == 'simple'")
	@ConditionalOnMissingBean({ CrshShellAuthenticationProperties.class })
	public CrshShellAuthenticationProperties simpleAuthenticationProperties() {
		return new SimpleAuthenticationProperties();
	}

	@Bean
	@ConditionalOnMissingBean({ PluginLifeCycle.class })
	public PluginLifeCycle shellBootstrap() {
		CrshBootstrapBean bootstrapBean = new CrshBootstrapBean();
		bootstrapBean.setConfig(this.properties.asCrshShellConfig());
		return bootstrapBean;
	}

	/**
	 * Class to configure CRaSH to authenticate against Spring Security.
	 */
	@Configuration
	@ConditionalOnExpression("'${shell.auth:spring}' == 'spring'")
	@ConditionalOnBean({ AuthenticationManager.class })
	@AutoConfigureAfter(CrshAutoConfiguration.class)
	public static class AuthenticationManagerAdapterAutoConfiguration {

		@Autowired(required = false)
		private ManagementServerProperties management;

		@Bean
		public CRaSHPlugin<?> shellAuthenticationManager() {
			return new AuthenticationManagerAdapter();
		}

		@Bean
		@ConditionalOnMissingBean({ CrshShellAuthenticationProperties.class })
		public CrshShellAuthenticationProperties springAuthenticationProperties() {
			// In case no shell.auth property is provided fall back to Spring Security
			// based authentication and get role to access shell from
			// ManagementServerProperties.
			// In case shell.auth is set to spring and roles are configured using
			// shell.auth.spring.roles the below default role will be overridden by
			// ConfigurationProperties.
			SpringAuthenticationProperties authenticationProperties = new SpringAuthenticationProperties();
			if (this.management != null) {
				authenticationProperties.setRoles(new String[] { this.management
						.getSecurity().getRole() });
			}
			return authenticationProperties;
		}

	}

	/**
	 * Spring Bean used to bootstrap the CRaSH shell.
	 */
	public static class CrshBootstrapBean extends PluginLifeCycle {

		@Autowired
		private ListableBeanFactory beanFactory;

		@Autowired
		private Environment environment;

		@Autowired
		private ShellProperties properties;

		@Autowired
		private ResourcePatternResolver resourceLoader;

		@PreDestroy
		public void destroy() {
			stop();
		}

		@PostConstruct
		public void init() {
			FS commandFileSystem = createFileSystem(
					this.properties.getCommandPathPatterns(),
					this.properties.getDisabledCommands());
			FS configurationFileSystem = createFileSystem(
					this.properties.getConfigPathPatterns(), new String[0]);

			PluginDiscovery discovery = new BeanFactoryFilteringPluginDiscovery(
					this.resourceLoader.getClassLoader(), this.beanFactory,
					this.properties.getDisabledPlugins());

			PluginContext context = new PluginContext(discovery,
					createPluginContextAttributes(), commandFileSystem,
					configurationFileSystem, this.resourceLoader.getClassLoader());

			context.refresh();
			start(context);
		}

		protected FS createFileSystem(String[] pathPatterns, String[] filterPatterns) {
			Assert.notNull(pathPatterns, "PathPatterns must not be null");
			Assert.notNull(filterPatterns, "FilterPatterns must not be null");
			FS fileSystem = new FS();
			for (String pathPattern : pathPatterns) {
				try {
					fileSystem.mount(new SimpleFileSystemDriver(new DirectoryHandle(
							pathPattern, this.resourceLoader, filterPatterns)));
				}
				catch (IOException ex) {
					throw new IllegalStateException("Failed to mount file system for '"
							+ pathPattern + "'", ex);
				}
			}
			return fileSystem;
		}

		protected Map<String, Object> createPluginContextAttributes() {
			Map<String, Object> attributes = new HashMap<String, Object>();
			String bootVersion = CrshAutoConfiguration.class.getPackage()
					.getImplementationVersion();
			if (bootVersion != null) {
				attributes.put("spring.boot.version", bootVersion);
			}
			attributes.put("spring.version", SpringVersion.getVersion());
			if (this.beanFactory != null) {
				attributes.put("spring.beanfactory", this.beanFactory);
			}
			if (this.environment != null) {
				attributes.put("spring.environment", this.environment);
			}
			return attributes;
		}

	}

	/**
	 * Adapts a Spring Security {@link AuthenticationManager} for use with CRaSH.
	 */
	@SuppressWarnings("rawtypes")
	private static class AuthenticationManagerAdapter extends
			CRaSHPlugin<AuthenticationPlugin> implements AuthenticationPlugin<String> {

		private static final PropertyDescriptor<String> ROLES = PropertyDescriptor
				.create("auth.spring.roles", "ADMIN",
						"Comma separated list of roles required to access the shell");

		@Autowired
		private AuthenticationManager authenticationManager;

		@Autowired(required = false)
		@Qualifier("shellAccessDecisionManager")
		private AccessDecisionManager accessDecisionManager;

		private String[] roles = new String[] { "ADMIN" };

		@Override
		public boolean authenticate(String username, String password) throws Exception {
			Authentication token = new UsernamePasswordAuthenticationToken(username,
					password);
			try {
				// Authenticate first to make sure credentials are valid
				token = this.authenticationManager.authenticate(token);
			}
			catch (AuthenticationException ex) {
				return false;
			}

			// Test access rights if a Spring Security AccessDecisionManager is installed
			if (this.accessDecisionManager != null && token.isAuthenticated()
					&& this.roles != null) {
				try {
					this.accessDecisionManager.decide(token, this,
							SecurityConfig.createList(this.roles));
				}
				catch (AccessDeniedException ex) {
					return false;
				}
			}
			return token.isAuthenticated();
		}

		@Override
		public Class<String> getCredentialType() {
			return String.class;
		}

		@Override
		public AuthenticationPlugin<String> getImplementation() {
			return this;
		}

		@Override
		public String getName() {
			return "spring";
		}

		@Override
		public void init() {
			String rolesPropertyValue = getContext().getProperty(ROLES);
			if (rolesPropertyValue != null) {
				this.roles = StringUtils
						.commaDelimitedListToStringArray(rolesPropertyValue);
			}
		}

		@Override
		protected Iterable<PropertyDescriptor<?>> createConfigurationCapabilities() {
			return Arrays.<PropertyDescriptor<?>> asList(ROLES);
		}

	}

	/**
	 * {@link ServiceLoaderDiscovery} to expose {@link CRaSHPlugin} Beans from Spring and
	 * deal with filtering disabled plugins.
	 */
	private static class BeanFactoryFilteringPluginDiscovery extends
			ServiceLoaderDiscovery {

		private final ListableBeanFactory beanFactory;

		private final String[] disabledPlugins;

		public BeanFactoryFilteringPluginDiscovery(ClassLoader classLoader,
				ListableBeanFactory beanFactory, String[] disabledPlugins)
				throws NullPointerException {
			super(classLoader);
			this.beanFactory = beanFactory;
			this.disabledPlugins = disabledPlugins;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public Iterable<CRaSHPlugin<?>> getPlugins() {
			List<CRaSHPlugin<?>> plugins = new ArrayList<CRaSHPlugin<?>>();

			for (CRaSHPlugin<?> p : super.getPlugins()) {
				if (isEnabled(p)) {
					plugins.add(p);
				}
			}

			Collection<CRaSHPlugin> pluginBeans = this.beanFactory.getBeansOfType(
					CRaSHPlugin.class).values();
			for (CRaSHPlugin<?> pluginBean : pluginBeans) {
				if (isEnabled(pluginBean)) {
					plugins.add(pluginBean);
				}
			}

			return plugins;
		}

		protected boolean isEnabled(CRaSHPlugin<?> plugin) {
			Assert.notNull(plugin, "Plugin must not be null");

			if (ObjectUtils.isEmpty(this.disabledPlugins)) {
				return true;
			}

			Set<Class<?>> pluginClasses = ClassUtils.getAllInterfacesAsSet(plugin);
			pluginClasses.add(plugin.getClass());

			for (Class<?> pluginClass : pluginClasses) {
				if (isEnabled(pluginClass)) {
					return true;
				}
			}
			return false;
		}

		private boolean isEnabled(Class<?> pluginClass) {
			for (String disabledPlugin : this.disabledPlugins) {
				if (ClassUtils.getShortName(pluginClass).equalsIgnoreCase(disabledPlugin)
						|| ClassUtils.getQualifiedName(pluginClass).equalsIgnoreCase(
								disabledPlugin)) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * {@link FSDriver} to wrap Spring's {@link Resource} abstraction to CRaSH.
	 */
	private static class SimpleFileSystemDriver extends AbstractFSDriver<ResourceHandle> {

		private final ResourceHandle root;

		public SimpleFileSystemDriver(ResourceHandle handle) {
			this.root = handle;
		}

		@Override
		public Iterable<ResourceHandle> children(ResourceHandle handle)
				throws IOException {
			if (handle instanceof DirectoryHandle) {
				return ((DirectoryHandle) handle).members();
			}
			return Collections.emptySet();
		}

		@Override
		public long getLastModified(ResourceHandle handle) throws IOException {
			if (handle instanceof FileHandle) {
				return ((FileHandle) handle).getLastModified();
			}
			return -1;
		}

		@Override
		public boolean isDir(ResourceHandle handle) throws IOException {
			return handle instanceof DirectoryHandle;
		}

		@Override
		public String name(ResourceHandle handle) throws IOException {
			return handle.getName();
		}

		@Override
		public Iterator<InputStream> open(ResourceHandle handle) throws IOException {
			if (handle instanceof FileHandle) {
				return Collections.singletonList(((FileHandle) handle).openStream())
						.iterator();
			}
			return Collections.<InputStream> emptyList().iterator();
		}

		@Override
		public ResourceHandle root() throws IOException {
			return this.root;
		}

	}

	/**
	 * Base for handles to Spring {@link Resource}s.
	 */
	private abstract static class ResourceHandle {

		private final String name;

		public ResourceHandle(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

	}

	/**
	 * {@link ResourceHandle} for a directory.
	 */
	private static class DirectoryHandle extends ResourceHandle {

		private final ResourcePatternResolver resourceLoader;

		private final String[] filterPatterns;

		private final AntPathMatcher matcher = new AntPathMatcher();

		public DirectoryHandle(String name, ResourcePatternResolver resourceLoader,
				String[] filterPatterns) {
			super(name);
			this.resourceLoader = resourceLoader;
			this.filterPatterns = filterPatterns;
		}

		public List<ResourceHandle> members() throws IOException {
			Resource[] resources = this.resourceLoader.getResources(getName());
			List<ResourceHandle> files = new ArrayList<ResourceHandle>();
			for (Resource resource : resources) {
				if (!resource.getURL().getPath().endsWith("/") && !shouldFilter(resource)) {
					files.add(new FileHandle(resource.getFilename(), resource));
				}
			}
			return files;
		}

		private boolean shouldFilter(Resource resource) {
			for (String filterPattern : this.filterPatterns) {
				if (this.matcher.match(filterPattern, resource.getFilename())) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * {@link ResourceHandle} for a file backed by a Spring {@link Resource}.
	 */
	private static class FileHandle extends ResourceHandle {

		private final Resource resource;

		public FileHandle(String name, Resource resource) {
			super(name);
			this.resource = resource;
		}

		public InputStream openStream() throws IOException {
			return this.resource.getInputStream();
		}

		public long getLastModified() {
			try {
				return this.resource.lastModified();
			}
			catch (IOException ex) {
				return -1;
			}
		}

	}

}
