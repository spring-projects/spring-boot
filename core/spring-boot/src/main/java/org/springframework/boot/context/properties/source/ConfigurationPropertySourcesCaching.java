/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * {@link ConfigurationPropertyCaching} for an {@link Iterable iterable} set of
 * {@link ConfigurationPropertySource} instances.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertySourcesCaching implements ConfigurationPropertyCaching {

	private final @Nullable Iterable<ConfigurationPropertySource> sources;

	ConfigurationPropertySourcesCaching(@Nullable Iterable<ConfigurationPropertySource> sources) {
		this.sources = sources;
	}

	@Override
	public void enable() {
		forEach(ConfigurationPropertyCaching::enable);
	}

	@Override
	public void disable() {
		forEach(ConfigurationPropertyCaching::disable);
	}

	@Override
	public void setTimeToLive(Duration timeToLive) {
		forEach((caching) -> caching.setTimeToLive(timeToLive));
	}

	@Override
	public void clear() {
		forEach(ConfigurationPropertyCaching::clear);
	}

	@Override
	public CacheOverride override() {
		CacheOverrides override = new CacheOverrides();
		forEach(override::add);
		return override;
	}

	private void forEach(Consumer<ConfigurationPropertyCaching> action) {
		if (this.sources != null) {
			for (ConfigurationPropertySource source : this.sources) {
				ConfigurationPropertyCaching caching = CachingConfigurationPropertySource.find(source);
				if (caching != null) {
					action.accept(caching);
				}
			}
		}
	}

	/**
	 * Composite {@link CacheOverride}.
	 */
	private final class CacheOverrides implements CacheOverride {

		private List<CacheOverride> overrides = new ArrayList<>();

		void add(ConfigurationPropertyCaching caching) {
			this.overrides.add(caching.override());
		}

		@Override
		public void close() {
			this.overrides.forEach(CacheOverride::close);
		}

	}

}
