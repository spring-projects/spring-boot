/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * A simple {@link InfoContributor} that exposes a map.
 *
 * @author Dave Syer
 * @since 1.4.0
 */
public class MapInfoContributor implements InfoContributor {

	private final Map<String, @Nullable Object> info;

	public MapInfoContributor(Map<String, Object> info) {
		this.info = new LinkedHashMap<>(info);
	}

	@Override
	public void contribute(Info.Builder builder) {
		builder.withDetails(this.info);
	}

}
