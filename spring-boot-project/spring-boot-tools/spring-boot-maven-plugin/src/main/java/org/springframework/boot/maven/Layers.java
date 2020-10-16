/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.maven;

import java.io.File;

/**
 * Layer configuration options.
 *
 * @author Madhura Bhave
 * @since 2.3.0
 */
public class Layers {

	private boolean enabled;

	private boolean includeLayerTools = true;

	private File configuration;

	/**
	 * Whether a {@code layers.idx} file should be added to the jar.
	 * @return true if a {@code layers.idx} file should be added.
	 */
	public boolean isEnabled() {
		return this.enabled;
	}

	/**
	 * Whether to include the layer tools jar.
	 * @return true if layer tools should be included
	 */
	public boolean isIncludeLayerTools() {
		return this.includeLayerTools;
	}

	/**
	 * The location of the layers configuration file. If no file is provided, a default
	 * configuration is used with four layers: {@code application}, {@code resources},
	 * {@code snapshot-dependencies} and {@code dependencies}.
	 * @return the layers configuration file
	 */
	public File getConfiguration() {
		return this.configuration;
	}

	public void setConfiguration(File configuration) {
		this.configuration = configuration;
	}

}
