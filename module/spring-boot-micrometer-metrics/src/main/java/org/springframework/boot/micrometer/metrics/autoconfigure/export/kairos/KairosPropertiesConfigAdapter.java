/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.kairos;

import io.micrometer.kairos.KairosConfig;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link KairosProperties} to a {@link KairosConfig}.
 *
 * @author Stephane Nicoll
 */
class KairosPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<KairosProperties>
		implements KairosConfig {

	KairosPropertiesConfigAdapter(KairosProperties properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return "management.kairos.metrics.export";
	}

	@Override
	public String uri() {
		return obtain(KairosProperties::getUri, KairosConfig.super::uri);
	}

	@Override
	public @Nullable String userName() {
		return get(KairosProperties::getUserName, KairosConfig.super::userName);
	}

	@Override
	public @Nullable String password() {
		return get(KairosProperties::getPassword, KairosConfig.super::password);
	}

}
