/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.freemarker;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.template.TemplateLocation;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
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
@Configuration
@ConditionalOnClass({ freemarker.template.Configuration.class,
		FreeMarkerConfigurationFactory.class })
@EnableConfigurationProperties(FreeMarkerProperties.class)
@Import({ FreeMarkerServletWebConfiguration.class,
		FreeMarkerReactiveWebConfiguration.class, FreeMarkerNonWebConfiguration.class })
public class FreeMarkerAutoConfiguration {

	private static final Log logger = LogFactory
			.getLog(FreeMarkerAutoConfiguration.class);

	private final ApplicationContext applicationContext;

	private final FreeMarkerProperties properties;

	public FreeMarkerAutoConfiguration(ApplicationContext applicationContext,
			FreeMarkerProperties properties) {
		this.applicationContext = applicationContext;
		this.properties = properties;
	}

	@PostConstruct
	public void checkTemplateLocationExists() {
		if (logger.isWarnEnabled() && this.properties.isCheckTemplateLocation()) {
			List<TemplateLocation> locations = getLocations();
			if (locations.stream().noneMatch(this::locationExists)) {
				logger.warn("Cannot find template location(s): " + locations
						+ " (please add some templates, "
						+ "check your FreeMarker configuration, or set "
						+ "spring.freemarker.checkTemplateLocation=false)");
			}
		}
	}

	private List<TemplateLocation> getLocations() {
		List<TemplateLocation> locations = new ArrayList<>();
		for (String templateLoaderPath : this.properties.getTemplateLoaderPath()) {
			TemplateLocation location = new TemplateLocation(templateLoaderPath);
			locations.add(location);
		}
		return locations;
	}

	private boolean locationExists(TemplateLocation location) {
		return location.exists(this.applicationContext);
	}

}
