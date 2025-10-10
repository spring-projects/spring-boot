/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.context.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

/**
 * {@link EnvironmentPostProcessor} that loads and applies {@link ConfigData} to Spring's
 * {@link Environment}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Nguyen Bao Sach
 * @since 2.4.0
 */
public class ConfigDataEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	/**
	 * The default order for the processor.
	 */
	public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

	/**
	 * Property used to determine what action to take when a
	 * {@code ConfigDataLocationNotFoundException} is thrown.
	 * @see ConfigDataNotFoundAction
	 */
	public static final String ON_LOCATION_NOT_FOUND_PROPERTY = ConfigDataEnvironment.ON_NOT_FOUND_PROPERTY;

	private final DeferredLogFactory logFactory;

	private final Log logger;

	private final ConfigurableBootstrapContext bootstrapContext;

	private final @Nullable ConfigDataEnvironmentUpdateListener environmentUpdateListener;

	public ConfigDataEnvironmentPostProcessor(DeferredLogFactory logFactory,
			ConfigurableBootstrapContext bootstrapContext) {
		this(logFactory, bootstrapContext, null);
	}

	private ConfigDataEnvironmentPostProcessor(DeferredLogFactory logFactory,
			ConfigurableBootstrapContext bootstrapContext,
			@Nullable ConfigDataEnvironmentUpdateListener environmentUpdateListener) {
		this.logFactory = logFactory;
		this.logger = logFactory.getLog(getClass());
		this.bootstrapContext = bootstrapContext;
		this.environmentUpdateListener = environmentUpdateListener;
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		postProcessEnvironment(environment, application.getResourceLoader(), application.getAdditionalProfiles());
	}

	void postProcessEnvironment(ConfigurableEnvironment environment, @Nullable ResourceLoader resourceLoader,
			Collection<String> additionalProfiles) {
		this.logger.trace("Post-processing environment to add config data");
		resourceLoader = (resourceLoader != null) ? resourceLoader : new DefaultResourceLoader();
		getConfigDataEnvironment(environment, resourceLoader, additionalProfiles).processAndApply();
	}

	ConfigDataEnvironment getConfigDataEnvironment(ConfigurableEnvironment environment, ResourceLoader resourceLoader,
			Collection<String> additionalProfiles) {
		return new ConfigDataEnvironment(this.logFactory, this.bootstrapContext, environment, resourceLoader,
				additionalProfiles, this.environmentUpdateListener);
	}

	/**
	 * Apply {@link ConfigData} post-processing to an existing {@link Environment}. This
	 * method can be useful when working with an {@link Environment} that has been created
	 * directly and not necessarily as part of a {@link SpringApplication}.
	 * @param environment the environment to apply {@link ConfigData} to
	 */
	public static void applyTo(ConfigurableEnvironment environment) {
		applyTo(environment, null, null, Collections.emptyList());
	}

	/**
	 * Apply {@link ConfigData} post-processing to an existing {@link Environment}. This
	 * method can be useful when working with an {@link Environment} that has been created
	 * directly and not necessarily as part of a {@link SpringApplication}.
	 * @param environment the environment to apply {@link ConfigData} to
	 * @param resourceLoader the resource loader to use
	 * @param bootstrapContext the bootstrap context to use or {@code null} to use a
	 * throw-away context
	 * @param additionalProfiles any additional profiles that should be applied
	 */
	public static void applyTo(ConfigurableEnvironment environment, ResourceLoader resourceLoader,
			@Nullable ConfigurableBootstrapContext bootstrapContext, String... additionalProfiles) {
		applyTo(environment, resourceLoader, bootstrapContext, Arrays.asList(additionalProfiles));
	}

	/**
	 * Apply {@link ConfigData} post-processing to an existing {@link Environment}. This
	 * method can be useful when working with an {@link Environment} that has been created
	 * directly and not necessarily as part of a {@link SpringApplication}.
	 * @param environment the environment to apply {@link ConfigData} to
	 * @param resourceLoader the resource loader to use
	 * @param bootstrapContext the bootstrap context to use or {@code null} to use a
	 * throw-away context
	 * @param additionalProfiles any additional profiles that should be applied
	 */
	public static void applyTo(ConfigurableEnvironment environment, @Nullable ResourceLoader resourceLoader,
			@Nullable ConfigurableBootstrapContext bootstrapContext, Collection<String> additionalProfiles) {
		DeferredLogFactory logFactory = Supplier::get;
		bootstrapContext = (bootstrapContext != null) ? bootstrapContext : new DefaultBootstrapContext();
		ConfigDataEnvironmentPostProcessor postProcessor = new ConfigDataEnvironmentPostProcessor(logFactory,
				bootstrapContext);
		postProcessor.postProcessEnvironment(environment, resourceLoader, additionalProfiles);
	}

	/**
	 * Apply {@link ConfigData} post-processing to an existing {@link Environment}. This
	 * method can be useful when working with an {@link Environment} that has been created
	 * directly and not necessarily as part of a {@link SpringApplication}.
	 * @param environment the environment to apply {@link ConfigData} to
	 * @param resourceLoader the resource loader to use
	 * @param bootstrapContext the bootstrap context to use or {@code null} to use a
	 * throw-away context
	 * @param additionalProfiles any additional profiles that should be applied
	 * @param environmentUpdateListener optional
	 * {@link ConfigDataEnvironmentUpdateListener} that can be used to track
	 * {@link Environment} updates.
	 */
	public static void applyTo(ConfigurableEnvironment environment, @Nullable ResourceLoader resourceLoader,
			@Nullable ConfigurableBootstrapContext bootstrapContext, Collection<String> additionalProfiles,
			ConfigDataEnvironmentUpdateListener environmentUpdateListener) {
		DeferredLogFactory logFactory = Supplier::get;
		bootstrapContext = (bootstrapContext != null) ? bootstrapContext : new DefaultBootstrapContext();
		ConfigDataEnvironmentPostProcessor postProcessor = new ConfigDataEnvironmentPostProcessor(logFactory,
				bootstrapContext, environmentUpdateListener);
		postProcessor.postProcessEnvironment(environment, resourceLoader, additionalProfiles);
	}

}
