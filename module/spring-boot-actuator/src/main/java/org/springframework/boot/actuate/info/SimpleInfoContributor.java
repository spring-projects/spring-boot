/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A simple {@link InfoContributor} that exposes a single detail.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class SimpleInfoContributor implements InfoContributor {

	private final String prefix;

	private final @Nullable Object detail;

	public SimpleInfoContributor(String prefix, @Nullable Object detail) {
		Assert.notNull(prefix, "'prefix' must not be null");
		this.prefix = prefix;
		this.detail = detail;
	}

	@Override
	public void contribute(Info.Builder builder) {
		if (this.detail != null) {
			builder.withDetail(this.prefix, this.detail);
		}
	}

}
