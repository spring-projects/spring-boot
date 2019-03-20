package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Collections;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicatorNameFactory;
import org.springframework.boot.actuate.health.StickyHealthIndicatorDecorator;

/**
 * Bean post processor that decorates Health Indicators with sticky UP status.
 *
 * @author Vladislav Fefelov
 */
public class StickyHealthIndicatorDecoratorBeanPostProcessor implements BeanPostProcessor {

	private final HealthIndicatorNameFactory nameFactory;

	private final Set<String> healthIndicatorNamesToDecorate;

	public StickyHealthIndicatorDecoratorBeanPostProcessor(HealthIndicatorNameFactory nameFactory,
														   Set<String> healthIndicatorNamesToDecorate) {
		this.nameFactory = nameFactory;
		this.healthIndicatorNamesToDecorate = healthIndicatorNamesToDecorate != null
			? healthIndicatorNamesToDecorate : Collections.emptySet();
	}

	public StickyHealthIndicatorDecoratorBeanPostProcessor(Set<String> healthIndicatorNamesToDecorate) {
		this(new HealthIndicatorNameFactory(), healthIndicatorNamesToDecorate);
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (!(bean instanceof HealthIndicator)) {
			return bean;
		}

		final String healthIndicatorName = nameFactory.apply(beanName);

		if (!healthIndicatorNamesToDecorate.contains(healthIndicatorName)) {
			return bean;
		}

		final HealthIndicator healthIndicator = (HealthIndicator) bean;

		return new StickyHealthIndicatorDecorator(healthIndicator);
	}

}
