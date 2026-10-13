/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.reactor.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Reactor.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@ConfigurationProperties("spring.reactor")
public class ReactorProperties {

	/**
	 * Context Propagation support mode for Reactor operators.
	 */
	private ContextPropagationMode contextPropagation = ContextPropagationMode.LIMITED;

	public ContextPropagationMode getContextPropagation() {
		return this.contextPropagation;
	}

	public void setContextPropagation(ContextPropagationMode contextPropagation) {
		this.contextPropagation = contextPropagation;
	}

	public enum ContextPropagationMode {

		/**
		 * Context Propagation is applied to all Reactor operators.
		 */
		AUTO,

		/**
		 * Context Propagation is only applied to "tap" and "handle" Reactor operators.
		 */
		LIMITED

	}

}
