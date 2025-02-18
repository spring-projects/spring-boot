/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Class to work with {@code reachability-metadata.properties}.
 *
 * @author Moritz Halbritter
 * @since 3.0.0
 */
public final class ReachabilityMetadataProperties {

	/**
	 * Location of the properties file. Must be formatted using
	 * {@link String#format(String, Object...)} with the group id, artifact id and version
	 * of the dependency.
	 */
	public static final String REACHABILITY_METADATA_PROPERTIES_LOCATION_TEMPLATE = "META-INF/native-image/%s/%s/%s/reachability-metadata.properties";

	private final Properties properties;

	private ReachabilityMetadataProperties(Properties properties) {
		this.properties = properties;
	}

	/**
	 * Returns if the dependency has been overridden.
	 * @return true if the dependency has been overridden
	 */
	public boolean isOverridden() {
		return Boolean.parseBoolean(this.properties.getProperty("override"));
	}

	/**
	 * Constructs a new instance from the given {@code InputStream}.
	 * @param inputStream {@code InputStream} to load the properties from
	 * @return loaded properties
	 * @throws IOException if loading from the {@code InputStream} went wrong
	 */
	public static ReachabilityMetadataProperties fromInputStream(InputStream inputStream) throws IOException {
		Properties properties = new Properties();
		properties.load(inputStream);
		return new ReachabilityMetadataProperties(properties);
	}

	/**
	 * Returns the location of the properties for the given coordinates.
	 * @param coordinates library coordinates for which the property file location should
	 * be returned
	 * @return location of the properties
	 */
	public static String getLocation(LibraryCoordinates coordinates) {
		return REACHABILITY_METADATA_PROPERTIES_LOCATION_TEMPLATE.formatted(coordinates.getGroupId(),
				coordinates.getArtifactId(), coordinates.getVersion());
	}

}
