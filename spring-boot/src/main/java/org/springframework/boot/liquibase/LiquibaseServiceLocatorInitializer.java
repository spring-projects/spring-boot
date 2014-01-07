package org.springframework.boot.liquibase;

import liquibase.servicelocator.CustomResolverServiceLocator;
import liquibase.servicelocator.ServiceLocator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplicationStartEvent;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ClassUtils;

/**
 * {@link ApplicationListener} that replaces the liquibase {@link ServiceLocator} with a
 * version that works with Spring Boot executable archives.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public class LiquibaseServiceLocatorInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>,
		ApplicationListener<SpringApplicationStartEvent> {

	static final Log logger = LogFactory.getLog(LiquibaseServiceLocatorInitializer.class);

	@Override
	public void onApplicationEvent(SpringApplicationStartEvent event) {
		if (ClassUtils.isPresent("liquibase.servicelocator.ServiceLocator", null)) {
			new LiquibasePresent().replaceServiceLocator();
		}
	}

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
	}

	/**
	 * Inner class to prevent class not found issues
	 */
	private static class LiquibasePresent {

		public void replaceServiceLocator() {
			ServiceLocator.setInstance(new CustomResolverServiceLocator(
					new SpringPackageScanClassResolver()));
		}

	}

}
