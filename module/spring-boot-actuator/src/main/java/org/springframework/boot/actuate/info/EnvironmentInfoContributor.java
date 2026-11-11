/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

import java.util.Map;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * An {@link InfoContributor} that provides all environment entries prefixed with info.
 *
 * @author Meang Akira Tanaka
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @since 1.4.0
 */
public class EnvironmentInfoContributor implements InfoContributor {

	private static final Bindable<Map<String, Object>> STRING_OBJECT_MAP = Bindable.mapOf(String.class, Object.class);

	private final ConfigurableEnvironment environment;

	public EnvironmentInfoContributor(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	@Override
	public void contribute(Info.Builder builder) {
		Binder binder = Binder.get(this.environment);
		binder.bind("info", STRING_OBJECT_MAP).ifBound(builder::withDetails);
	}

}
