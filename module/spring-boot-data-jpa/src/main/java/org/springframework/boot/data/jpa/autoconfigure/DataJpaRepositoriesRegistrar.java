/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jpa.autoconfigure;

import java.lang.annotation.Annotation;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.util.StringUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} used to auto-configure Spring Data JPA
 * Repositories.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Scott Frederick
 */
class DataJpaRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

	private @Nullable BootstrapMode bootstrapMode;

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableJpaRepositories.class;
	}

	@Override
	protected Class<?> getConfiguration() {
		return EnableJpaRepositoriesConfiguration.class;
	}

	@Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
		return new JpaRepositoryConfigExtension();
	}

	@Override
	protected BootstrapMode getBootstrapMode() {
		return (this.bootstrapMode == null) ? BootstrapMode.DEFAULT : this.bootstrapMode;
	}

	@Override
	public void setEnvironment(Environment environment) {
		super.setEnvironment(environment);
		configureBootstrapMode(environment);
	}

	private void configureBootstrapMode(Environment environment) {
		String property = environment.getProperty("spring.data.jpa.repositories.bootstrap-mode");
		if (StringUtils.hasText(property)) {
			this.bootstrapMode = BootstrapMode.valueOf(property.toUpperCase(Locale.ENGLISH));
		}
	}

	@EnableJpaRepositories
	private static final class EnableJpaRepositoriesConfiguration {

	}

}
