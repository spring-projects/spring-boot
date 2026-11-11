/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.contributor;

import java.util.Map;
import java.util.function.Function;

import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * Base class for health contributor configurations that can combine source beans into a
 * composite.
 *
 * @param <I> the health indicator type
 * @param <B> the bean type
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 4.0.0
 */
public abstract class CompositeHealthContributorConfiguration<I extends HealthIndicator, B>
		extends AbstractCompositeHealthContributorConfiguration<HealthContributor, I, B> {

	/**
	 * Creates a {@code CompositeHealthContributorConfiguration} that will use the given
	 * {@code indicatorFactory} to create {@link HealthIndicator} instances.
	 * @param indicatorFactory the function to create health indicator instances
	 */
	public CompositeHealthContributorConfiguration(Function<B, I> indicatorFactory) {
		super(indicatorFactory);
	}

	@Override
	protected final HealthContributor createComposite(Map<String, B> beans) {
		return CompositeHealthContributor.fromMap(beans, this::createIndicator);
	}

}
