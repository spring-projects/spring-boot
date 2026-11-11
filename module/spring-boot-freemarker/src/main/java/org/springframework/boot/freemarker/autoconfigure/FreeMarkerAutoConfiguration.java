/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.freemarker.autoconfigure;

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
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ freemarker.template.Configuration.class, FreeMarkerConfigurationFactory.class })
@EnableConfigurationProperties(FreeMarkerProperties.class)
@Import({ FreeMarkerServletWebConfiguration.class, FreeMarkerReactiveWebConfiguration.class,
		FreeMarkerNonWebConfiguration.class })
public final class FreeMarkerAutoConfiguration {

	private static final Log logger = LogFactory.getLog(FreeMarkerAutoConfiguration.class);

	private final ApplicationContext applicationContext;

	private final FreeMarkerProperties properties;

	FreeMarkerAutoConfiguration(ApplicationContext applicationContext, FreeMarkerProperties properties) {
		this.applicationContext = applicationContext;
		this.properties = properties;
		checkTemplateLocationExists();
	}

	private void checkTemplateLocationExists() {
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
