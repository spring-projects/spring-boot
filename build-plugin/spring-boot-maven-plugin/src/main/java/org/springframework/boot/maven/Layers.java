/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.maven;

import java.io.File;

import org.jspecify.annotations.Nullable;

/**
 * Layer configuration options.
 *
 * @author Madhura Bhave
 * @since 2.3.0
 */
public class Layers {

	private boolean enabled = true;

	private @Nullable File configuration;

	private @Nullable String configurationName;

	/**
	 * Whether a {@code layers.idx} file should be added to the jar.
	 * @return true if a {@code layers.idx} file should be added.
	 */
	public boolean isEnabled() {
		return this.enabled;
	}

	/**
	 * The location of the layers configuration file. If no file is provided, a default
	 * configuration is used with four layers: {@code application}, {@code resources},
	 * {@code snapshot-dependencies} and {@code dependencies}.
	 * @return the layers configuration file
	 */
	public @Nullable File getConfiguration() {
		return this.configuration;
	}

	public void setConfiguration(@Nullable File configuration) {
		this.configuration = configuration;
	}

	/**
	 * The name of the layers configuration to load from the classpath. A matching file is
	 * expected at {@code META-INF/spring/layers/<name>.xml}
	 * @return the layers configuration name to load from the classpath
	 */
	public @Nullable String getConfigurationName() {
		return this.configurationName;
	}

	public void setConfigurationName(@Nullable String configurationName) {
		this.configurationName = configurationName;
	}

}
