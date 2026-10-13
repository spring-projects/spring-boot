/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.mergingcomplextypes.map;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("my")
public class MyProperties {

	private final Map<String, MyPojo> map = new LinkedHashMap<>();

	public Map<String, MyPojo> getMap() {
		return this.map;
	}

}
