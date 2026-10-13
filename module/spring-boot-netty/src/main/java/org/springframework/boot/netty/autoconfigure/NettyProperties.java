/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.netty.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for the Netty engine.
 * <p>
 * These properties apply globally to the Netty library, used as a client or a server.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@ConfigurationProperties("spring.netty")
public class NettyProperties {

	/**
	 * Level of leak detection for reference-counted buffers. If not configured via
	 * 'ResourceLeakDetector.setLevel' or the 'io.netty.leakDetection.level' system
	 * property, default to 'simple'.
	 */
	private @Nullable LeakDetection leakDetection;

	public @Nullable LeakDetection getLeakDetection() {
		return this.leakDetection;
	}

	public void setLeakDetection(@Nullable LeakDetection leakDetection) {
		this.leakDetection = leakDetection;
	}

	public enum LeakDetection {

		/**
		 * Disable leak detection completely.
		 */
		DISABLED,

		/**
		 * Detect leaks for 1% of buffers.
		 */
		SIMPLE,

		/**
		 * Detect leaks for 1% of buffers and track where they were accessed.
		 */
		ADVANCED,

		/**
		 * Detect leaks for 100% of buffers and track where they were accessed.
		 */
		PARANOID

	}

}
