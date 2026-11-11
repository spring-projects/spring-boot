/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.origin.Origin;

/**
 * {@link BindHandler} to set the {@link Origin} of bound {@link ConfigDataLocation}
 * objects.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ConfigDataLocationBindHandler extends AbstractBindHandler {

	@Override
	public @Nullable Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context,
			Object result) {
		OriginMapper originMapper = new OriginMapper(context.getConfigurationProperty());
		if (result instanceof ConfigDataLocation location) {
			return originMapper.map(location);
		}
		if (result instanceof List<?> locations) {
			return locations.stream().map(originMapper::mapIfPossible).collect(Collectors.toCollection(ArrayList::new));
		}
		if (result instanceof ConfigDataLocation[] locations) {
			return Arrays.stream(locations).map(originMapper::mapIfPossible).toArray(ConfigDataLocation[]::new);
		}
		return result;
	}

	private record OriginMapper(@Nullable ConfigurationProperty property) {

		@Nullable Object mapIfPossible(@Nullable Object object) {
			return (object instanceof ConfigDataLocation location) ? map(location) : object;
		}

		@Nullable ConfigDataLocation map(@Nullable ConfigDataLocation location) {
			if (location == null) {
				return null;
			}
			Origin origin = Origin.from(location);
			return (origin != null) ? location : location.withOrigin(Origin.from(property()));
		}

	}

}
