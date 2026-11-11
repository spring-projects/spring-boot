/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webclient.autoconfigure.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceGroup;

/**
 * A {@link WebClientHttpServiceGroupConfigurer} to apply auto-configured
 * {@link WebClientCustomizer} beans to the group's {@link WebClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Phillip Webb
 */
class WebClientCustomizerHttpServiceGroupConfigurer implements WebClientHttpServiceGroupConfigurer {

	/**
	 * Allow user defined configurers to apply before / after ours.
	 */
	private static final int ORDER = 0;

	private final ObjectProvider<WebClientCustomizer> customizers;

	WebClientCustomizerHttpServiceGroupConfigurer(ObjectProvider<WebClientCustomizer> customizers) {
		this.customizers = customizers;
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public void configureGroups(Groups<WebClient.Builder> groups) {
		groups.forEachClient(this::configureClient);
	}

	private void configureClient(HttpServiceGroup group, WebClient.Builder builder) {
		this.customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
	}

}
