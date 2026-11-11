/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.json;

import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * A {@link ContextCustomizerFactory} that produces a {@link ContextCustomizer} that warns
 * the user when multiple occurrences of {@code JSONObject} are found on the class path.
 *
 * @author Andy Wilkinson
 */
class DuplicateJsonObjectContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		return new DuplicateJsonObjectContextCustomizer();
	}

	private static final class DuplicateJsonObjectContextCustomizer implements ContextCustomizer {

		private final Log logger = LogFactory.getLog(DuplicateJsonObjectContextCustomizer.class);

		@Override
		public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
			List<URL> jsonObjects = findJsonObjects();
			if (jsonObjects.size() > 1) {
				logDuplicateJsonObjectsWarning(jsonObjects);
			}
		}

		private List<URL> findJsonObjects() {
			try {
				Enumeration<URL> resources = getClass().getClassLoader().getResources("org/json/JSONObject.class");
				return Collections.list(resources);
			}
			catch (Exception ex) {
				// Continue
			}
			return Collections.emptyList();
		}

		private void logDuplicateJsonObjectsWarning(List<URL> jsonObjects) {
			StringBuilder message = new StringBuilder(
					String.format("%n%nFound multiple occurrences of org.json.JSONObject on the class path:%n%n"));
			for (URL jsonObject : jsonObjects) {
				message.append(String.format("\t%s%n", jsonObject));
			}
			message.append(
					String.format("%nYou may wish to exclude one of them to ensure predictable runtime behavior%n"));
			this.logger.warn(message);
		}

		@Override
		public boolean equals(Object obj) {
			return (obj != null) && (getClass() == obj.getClass());
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

	}

}
