/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.relaxedbinding.mapsfromenvironmentvariables;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("my.props")
public class MyMapsProperties {

	private final Map<String, String> values = new LinkedHashMap<>();

	public Map<String, String> getValues() {
		return this.values;
	}

}
