/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.json;

import java.net.URL;
import java.util.ArrayList;
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

	private static class DuplicateJsonObjectContextCustomizer
			implements ContextCustomizer {

		private final Log logger = LogFactory
				.getLog(DuplicateJsonObjectContextCustomizer.class);

		@Override
		public void customizeContext(ConfigurableApplicationContext context,
				MergedContextConfiguration mergedConfig) {
			List<URL> jsonObjects = findJsonObjects();
			if (jsonObjects.size() > 1) {
				logDuplicateJsonObjectsWarning(jsonObjects);
			}
		}

		private List<URL> findJsonObjects() {
			List<URL> jsonObjects = new ArrayList<URL>();
			try {
				Enumeration<URL> resources = getClass().getClassLoader()
						.getResources("org/json/JSONObject.class");
				while (resources.hasMoreElements()) {
					jsonObjects.add(resources.nextElement());
				}
			}
			catch (Exception ex) {
				// Continue
			}
			return jsonObjects;
		}

		private void logDuplicateJsonObjectsWarning(List<URL> jsonObjects) {
			StringBuilder message = new StringBuilder(
					String.format("%n%nFound multiple occurrences of"
							+ " org.json.JSONObject on the class path:%n%n"));
			for (URL jsonObject : jsonObjects) {
				message.append(String.format("\t%s%n", jsonObject));
			}
			message.append(String.format("%nYou may wish to exclude one of them to ensure"
					+ " predictable runtime behavior%n"));
			this.logger.warn(message);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || obj.getClass() != getClass()) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

	}

}
