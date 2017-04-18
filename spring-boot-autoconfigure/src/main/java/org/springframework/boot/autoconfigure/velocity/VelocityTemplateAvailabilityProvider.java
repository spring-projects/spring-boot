/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.velocity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.autoconfigure.template.PathBasedTemplateAvailabilityProvider;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;

/**
 * {@link TemplateAvailabilityProvider} that provides availability information for
 * Velocity view templates.
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 * @deprecated as of 1.4 following the deprecation of Velocity support in Spring Framework
 * 4.3
 */
@Deprecated
public class VelocityTemplateAvailabilityProvider
		extends PathBasedTemplateAvailabilityProvider {

	public VelocityTemplateAvailabilityProvider() {
		super("org.apache.velocity.app.VelocityEngine",
				VelocityTemplateAvailabilityProperties.class, "spring.velocity");
	}

	static class VelocityTemplateAvailabilityProperties
			extends TemplateAvailabilityProperties {

		private List<String> resourceLoaderPath = new ArrayList<String>(
				Arrays.asList(VelocityProperties.DEFAULT_RESOURCE_LOADER_PATH));

		VelocityTemplateAvailabilityProperties() {
			super(VelocityProperties.DEFAULT_PREFIX, VelocityProperties.DEFAULT_SUFFIX);
		}

		@Override
		protected List<String> getLoaderPath() {
			return this.resourceLoaderPath;
		}

		public List<String> getResourceLoaderPath() {
			return this.resourceLoaderPath;
		}

		public void setResourceLoaderPath(List<String> resourceLoaderPath) {
			this.resourceLoaderPath = resourceLoaderPath;
		}

	}

}
