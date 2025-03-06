/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive;

import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
import org.springframework.web.reactive.config.ResourceChainRegistration;
import org.springframework.web.reactive.config.ResourceHandlerRegistration;
import org.springframework.web.reactive.resource.EncodedResourceResolver;
import org.springframework.web.reactive.resource.ResourceResolver;
import org.springframework.web.reactive.resource.VersionResourceResolver;

/**
 * {@link ResourceHandlerRegistrationCustomizer} used by auto-configuration to customize
 * the resource chain.
 *
 * @author Brian Clozel
 */
class ResourceChainResourceHandlerRegistrationCustomizer implements ResourceHandlerRegistrationCustomizer {

	private final Resources resourceProperties;

	ResourceChainResourceHandlerRegistrationCustomizer(Resources resources) {
		this.resourceProperties = resources;
	}

	@Override
	public void customize(ResourceHandlerRegistration registration) {
		Resources.Chain properties = this.resourceProperties.getChain();
		configureResourceChain(properties, registration.resourceChain(properties.isCache()));
	}

	private void configureResourceChain(Resources.Chain properties, ResourceChainRegistration chain) {
		Resources.Chain.Strategy strategy = properties.getStrategy();
		if (properties.isCompressed()) {
			chain.addResolver(new EncodedResourceResolver());
		}
		if (strategy.getFixed().isEnabled() || strategy.getContent().isEnabled()) {
			chain.addResolver(getVersionResourceResolver(strategy));
		}
	}

	private ResourceResolver getVersionResourceResolver(Resources.Chain.Strategy properties) {
		VersionResourceResolver resolver = new VersionResourceResolver();
		if (properties.getFixed().isEnabled()) {
			String version = properties.getFixed().getVersion();
			String[] paths = properties.getFixed().getPaths();
			resolver.addFixedVersionStrategy(version, paths);
		}
		if (properties.getContent().isEnabled()) {
			String[] paths = properties.getContent().getPaths();
			resolver.addContentVersionStrategy(paths);
		}
		return resolver;
	}

}
