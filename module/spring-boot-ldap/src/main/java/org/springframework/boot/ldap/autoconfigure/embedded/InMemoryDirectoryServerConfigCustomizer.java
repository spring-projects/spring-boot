/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ldap.autoconfigure.embedded;

import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * auto-configured embedded LDAP {@link InMemoryDirectoryServerConfig configuration}.
 *
 * @author Eymen Onar
 * @since 4.3.0
 */
@FunctionalInterface
public interface InMemoryDirectoryServerConfigCustomizer {

	/**
	 * Customize the configuration.
	 * @param config the {@link InMemoryDirectoryServerConfig} to customize
	 */
	void customize(InMemoryDirectoryServerConfig config);

}
