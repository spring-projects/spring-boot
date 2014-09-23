package org.springframework.boot.context.event;

import org.springframework.core.env.ConfigurableEnvironment;

/**
 * TODO h.stolzenberg: Document
 *
 * @author h.stolzenberg
 * @since 1.2.0
 */
public interface ProvidesEnvironmentEvent {
	abstract ConfigurableEnvironment getEnvironment();
}
