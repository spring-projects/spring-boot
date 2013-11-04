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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.properties.CrshProperties;
import org.springframework.boot.actuate.properties.CrshProperties.AuthenticationProperties;
import org.springframework.boot.actuate.properties.CrshProperties.JaasAuthenticationProperties;
import org.springframework.boot.actuate.properties.CrshProperties.KeyAuthenticationProperties;
import org.springframework.boot.actuate.properties.CrshProperties.SimpleAuthenticationProperties;
import org.springframework.boot.actuate.properties.CrshProperties.SpringAuthenticationProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.SpringVersion;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for embedding an extensible shell
 * into a Spring Boot enabled application. By default a SSH daemon is started on port 2000
 * with a default username <code>user</code> and password (default password is logged
 * during application startup).
 * 
 * <p>
 * This configuration will auto detect the existence of a Spring Security
 * {@link AuthenticationManager} and will delegate authentication requests for shell
 * access to this detected instance.
 * 
 * <p>
 * To add customizations to the shell simply define beans of type {@link CRaSHPlugin} in
 * the application context. Those beans will get auto detected during startup and
 * registered with the underlying shell infrastructure.
 * 
 * <p>
 * Additional shell commands can be implemented using the guide and documentation at <a
 * href="http://www.crashub.org">crashub.org</a>. By default Boot will search for commands
 * using the following classpath scanning pattern <code>classpath*:/commands/**</code>. To
 * add different locations or override the default use
 * <code>shell.command_path_patterns</code> in your application configuration.
 * 
 * @author Christian Dupuis
 */
@Configuration
@ConditionalOnClass({ PluginLifeCycle.class })
@EnableConfigurationProperties({ CrshProperties.class })
@AutoConfigureAfter(SecurityAutoConfiguration.class)
public class CrshAutoConfiguration {

	@Autowired
	private CrshProperties properties;

	@Bean
	@ConditionalOnExpression("#{environment['shell.auth'] == 'jaas'}")
	@ConditionalOnMissingBean({ AuthenticationProperties.class })
	public AuthenticationProperties jaasAuthenticationProperties() {
		return new JaasAuthenticationProperties();
	}

	@Bean
	@ConditionalOnExpression("#{environment['shell.auth'] == 'key'}")
	@ConditionalOnMissingBean({ AuthenticationProperties.class })
	public AuthenticationProperties keyAuthenticationProperties() {
		return new KeyAuthenticationProperties();
	}

	@Bean
	@ConditionalOnExpression("#{environment['shell.auth'] == 'simple'}")
	@ConditionalOnMissingBean({ AuthenticationProperties.class })
	public AuthenticationProperties simpleAuthenticationProperties() {
		return new SimpleAuthenticationProperties();
	}

	@Bean
	@ConditionalOnExpression("#{environment['shell.auth'] == 'spring'}")
	@ConditionalOnMissingBean({ AuthenticationProperties.class })
	public AuthenticationProperties SpringAuthenticationProperties() {
		return new SpringAuthenticationProperties();
	}

	@Bean
	@ConditionalOnBean({ AuthenticationManager.class })
	public CRaSHPlugin<?> shellAuthenticationManager() {
		return new AuthenticationManagerAdapter();
	}

	@Bean
	@ConditionalOnMissingBean({ PluginLifeCycle.class })
	public PluginLifeCycle shellBootstrap() {
		CrshBootstrap bs = new CrshBootstrap();
		bs.setConfig(this.properties.mergeProperties(new Properties()));
		return bs;
	}

	public static class CrshBootstrap extends PluginLifeCycle {

		@Autowired
		private ListableBeanFactory beanFactory;

		@Autowired
		private CrshProperties properties;

		@Autowired
		private ResourcePatternResolver resourceLoader;

		@PreDestroy
		public void destroy() {
			stop();
		}

		@PostConstruct
		public void init() throws Exception {
			FS commandFileSystem = createFileSystem(this.properties
					.getCommandPathPatterns());
			FS confFileSystem = createFileSystem(this.properties.getConfigPathPatterns());

			PluginDiscovery discovery = new BeanFactoryFilteringPluginDiscovery(
					this.resourceLoader.getClassLoader(), this.beanFactory,
					this.properties.getDisabledPlugins());

			PluginContext context = new PluginContext(discovery,
					createPluginContextAttributes(), commandFileSystem, confFileSystem,
					this.resourceLoader.getClassLoader());

			context.refresh();
			start(context);
		}

		protected FS createFileSystem(String[] pathPatterns) throws IOException,
				URISyntaxException {
			Assert.notNull(pathPatterns);
			FS cmdFS = new FS();
			for (String pathPattern : pathPatterns) {
				cmdFS.mount(new SimpleFileSystemDriver(new DirectoryHandle(pathPattern,
						this.resourceLoader)));
			}
			return cmdFS;
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
			return attributes;
		}

	}

	@SuppressWarnings("rawtypes")
	private static class AuthenticationManagerAdapter extends
			CRaSHPlugin<AuthenticationPlugin> implements AuthenticationPlugin<String> {

		private static final PropertyDescriptor<String> ROLES = PropertyDescriptor
				.create("auth.spring.roles", "ADMIN",
						"Comma separated list of roles required to access the shell");

		@Autowired(required = false)
		private AccessDecisionManager accessDecisionManager;

		@Autowired
		private AuthenticationManager authenticationManager;

		private String[] roles = new String[] { "ROLE_ADMIN" };

		@Override
		public boolean authenticate(String username, String password) throws Exception {
			// Authenticate first to make credentials are valid
			Authentication token = new UsernamePasswordAuthenticationToken(username,
					password);
			try {
				token = this.authenticationManager.authenticate(token);
			}
			catch (AuthenticationException ae) {
				return false;
			}

			// Test access rights if a Spring Security AccessDecisionManager is installed
			if (this.accessDecisionManager != null && token.isAuthenticated()
					&& this.roles != null) {
				try {
					this.accessDecisionManager.decide(token, this,
							SecurityConfig.createList(this.roles));
				}
				catch (AccessDeniedException e) {
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

	private static class BeanFactoryFilteringPluginDiscovery extends
			ServiceLoaderDiscovery {

		private ListableBeanFactory beanFactory;

		private String[] disabledPlugins;

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
				if (!shouldFilter(p)) {
					plugins.add(p);
				}
			}

			Collection<CRaSHPlugin> springPlugins = this.beanFactory.getBeansOfType(
					CRaSHPlugin.class).values();
			for (CRaSHPlugin<?> p : springPlugins) {
				if (!shouldFilter(p)) {
					plugins.add(p);
				}
			}

			return plugins;
		}

		@SuppressWarnings("rawtypes")
		protected boolean shouldFilter(CRaSHPlugin<?> plugin) {
			Assert.notNull(plugin);

			Set<Class> classes = ClassUtils.getAllInterfacesAsSet(plugin);
			classes.add(plugin.getClass());

			for (Class<?> clazz : classes) {
				if (this.disabledPlugins != null && this.disabledPlugins.length > 0) {
					for (String disabledPlugin : this.disabledPlugins) {
						if (ClassUtils.getShortName(clazz).equalsIgnoreCase(
								disabledPlugin)
								|| ClassUtils.getQualifiedName(clazz).equalsIgnoreCase(
										disabledPlugin)) {
							return true;
						}
					}
				}
			}
			return false;
		}

	}

	private static class SimpleFileSystemDriver extends AbstractFSDriver<ResourceHandle> {

		private ResourceHandle root;

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

	private static class DirectoryHandle extends ResourceHandle {

		private ResourcePatternResolver resourceLoader;

		public DirectoryHandle(String name, ResourcePatternResolver resourceLoader) {
			super(name);
			this.resourceLoader = resourceLoader;
		}

		public List<ResourceHandle> members() throws IOException {
			Resource[] resources = this.resourceLoader.getResources(getName());
			List<ResourceHandle> files = new ArrayList<ResourceHandle>();
			for (Resource resource : resources) {
				if (!resource.getURL().getPath().endsWith("/")) {
					files.add(new FileHandle(resource.getFilename(), resource));
				}
			}
			return files;
		}

	}

	private static class FileHandle extends ResourceHandle {

		private Resource resource;

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
			catch (IOException e) {
			}
			return -1;
		}

	}

	private abstract static class ResourceHandle {

		private String name;

		public ResourceHandle(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

	}

}
