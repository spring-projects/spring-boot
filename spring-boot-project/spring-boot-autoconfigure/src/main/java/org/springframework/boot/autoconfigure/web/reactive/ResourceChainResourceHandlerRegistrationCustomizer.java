/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.web.reactive.config.ResourceChainRegistration;
import org.springframework.web.reactive.config.ResourceHandlerRegistration;
import org.springframework.web.reactive.resource.AppCacheManifestTransformer;
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

	@Autowired
	private ResourceProperties resourceProperties = new ResourceProperties();

	@Override
	public void customize(ResourceHandlerRegistration registration) {
		ResourceProperties.Chain properties = this.resourceProperties.getChain();
		configureResourceChain(properties, registration.resourceChain(properties.isCache()));
	}

	private void configureResourceChain(ResourceProperties.Chain properties, ResourceChainRegistration chain) {
		ResourceProperties.Strategy strategy = properties.getStrategy();
		if (properties.isCompressed()) {
			chain.addResolver(new EncodedResourceResolver());
		}
		if (strategy.getFixed().isEnabled() || strategy.getContent().isEnabled()) {
			chain.addResolver(getVersionResourceResolver(strategy));
		}
		if (properties.isHtmlApplicationCache()) {
			chain.addTransformer(new AppCacheManifestTransformer());
		}
	}

	private ResourceResolver getVersionResourceResolver(ResourceProperties.Strategy properties) {
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
