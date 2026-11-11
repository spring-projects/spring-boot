/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.service.connection;

import org.testcontainers.containers.Container;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for {@link ServiceConnection @ServiceConnection} annotated
 * {@link Container} beans.
 *
 * @author Phillip Webb
 * @since 3.1.0
 */
@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Import(ServiceConnectionAutoConfigurationRegistrar.class)
public final class ServiceConnectionAutoConfiguration {

}
