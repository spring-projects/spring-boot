/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.freemarker;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.template.TemplateLocation;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for FreeMarker.
 *
 * @author Andy Wilkinson
 * @author Dave Syer
 * @author Kazuki Shimizu
 * @since 1.1.0
 */
@AutoConfiguration
@ConditionalOnClass({ freemarker.template.Configuration.class, FreeMarkerConfigurationFactory.class })
@EnableConfigurationProperties(FreeMarkerProperties.class)
@Import({ FreeMarkerServletWebConfiguration.class, FreeMarkerReactiveWebConfiguration.class,
		FreeMarkerNonWebConfiguration.class })
public class FreeMarkerAutoConfiguration {

	private static final Log logger = LogFactory.getLog(FreeMarkerAutoConfiguration.class);

	private final ApplicationContext applicationContext;

	private final FreeMarkerProperties properties;

	/**
	 * Constructs a new instance of the {@code FreeMarkerAutoConfiguration} class.
	 * @param applicationContext the application context
	 * @param properties the FreeMarker properties
	 */
	public FreeMarkerAutoConfiguration(ApplicationContext applicationContext, FreeMarkerProperties properties) {
		this.applicationContext = applicationContext;
		this.properties = properties;
		checkTemplateLocationExists();
	}

	/**
	 * Checks if the template location exists.
	 *
	 * This method checks if the template location exists by iterating through the list of
	 * template locations obtained from the {@link #getLocations()} method. If none of the
	 * locations exist, a warning message is logged.
	 *
	 * The check is performed only if the logger is enabled for warning level and the
	 * property {@code checkTemplateLocation} is set to {@code true} in the configuration.
	 *
	 * If no template locations are found, a warning message is logged indicating that no
	 * template locations were found.
	 *
	 * @see #getLocations()
	 * @see TemplateLocation
	 * @see org.slf4j.Logger#isWarnEnabled()
	 * @see org.slf4j.Logger#warn(String)
	 * @see org.springframework.boot.autoconfigure.freemarker.FreeMarkerProperties#isCheckTemplateLocation()
	 * @see org.springframework.boot.autoconfigure.freemarker.FreeMarkerProperties#setCheckTemplateLocation(boolean)
	 * @see org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration
	 */
	public void checkTemplateLocationExists() {
		if (logger.isWarnEnabled() && this.properties.isCheckTemplateLocation()) {
			List<TemplateLocation> locations = getLocations();
			if (locations.stream().noneMatch(this::locationExists)) {
				String suffix = (locations.size() == 1) ? "" : "s";
				logger.warn("Cannot find template location" + suffix + ": " + locations
						+ " (please add some templates, " + "check your FreeMarker configuration, or set "
						+ "spring.freemarker.check-template-location=false)");
			}
		}
	}

	/**
	 * Retrieves the list of template locations.
	 * @return The list of template locations.
	 */
	private List<TemplateLocation> getLocations() {
		List<TemplateLocation> locations = new ArrayList<>();
		for (String templateLoaderPath : this.properties.getTemplateLoaderPath()) {
			TemplateLocation location = new TemplateLocation(templateLoaderPath);
			locations.add(location);
		}
		return locations;
	}

	/**
	 * Checks if the specified template location exists.
	 * @param location the template location to check
	 * @return true if the template location exists, false otherwise
	 */
	private boolean locationExists(TemplateLocation location) {
		return location.exists(this.applicationContext);
	}

}
