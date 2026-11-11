/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

import java.util.Map;
import java.util.Properties;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.info.BuildInfoContributor.BuildInfoContributorRuntimeHints;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * An {@link InfoContributor} that exposes {@link BuildProperties}.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@ImportRuntimeHints(BuildInfoContributorRuntimeHints.class)
public class BuildInfoContributor extends InfoPropertiesInfoContributor<BuildProperties> {

	public BuildInfoContributor(BuildProperties properties) {
		super(properties, Mode.FULL);
	}

	@Override
	public void contribute(Info.Builder builder) {
		builder.withDetail("build", generateContent());
	}

	@Override
	protected PropertySource<?> toSimplePropertySource() {
		Properties props = new Properties();
		copyIfSet(props, "group");
		copyIfSet(props, "artifact");
		copyIfSet(props, "name");
		copyIfSet(props, "version");
		copyIfSet(props, "time");
		return new PropertiesPropertySource("build", props);
	}

	@Override
	protected void postProcessContent(Map<String, Object> content) {
		replaceValue(content, "time", getProperties().getTime());
	}

	static class BuildInfoContributorRuntimeHints implements RuntimeHintsRegistrar {

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			this.bindingRegistrar.registerReflectionHints(hints.reflection(), BuildProperties.class);
		}

	}

}
