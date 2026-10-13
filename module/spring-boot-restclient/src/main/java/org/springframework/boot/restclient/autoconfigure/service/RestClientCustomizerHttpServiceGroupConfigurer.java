/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.autoconfigure.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceGroup;

/**
 * A {@link RestClientHttpServiceGroupConfigurer} to apply auto-configured
 * {@link RestClientCustomizer} beans to the group's {@link RestClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Phillip Webb
 */
class RestClientCustomizerHttpServiceGroupConfigurer implements RestClientHttpServiceGroupConfigurer {

	/**
	 * Allow user defined configurers to apply before / after ours.
	 */
	private static final int ORDER = 0;

	private final ObjectProvider<RestClientCustomizer> customizers;

	RestClientCustomizerHttpServiceGroupConfigurer(ObjectProvider<RestClientCustomizer> customizers) {
		this.customizers = customizers;
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public void configureGroups(Groups<RestClient.Builder> groups) {
		groups.forEachClient(this::configureClient);
	}

	private void configureClient(HttpServiceGroup group, RestClient.Builder builder) {
		this.customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
	}

}
