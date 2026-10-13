/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link GenericContainer} for OpenLDAP.
 *
 * @author Philipp Kessler
 */
public class OpenLdapContainer extends GenericContainer<OpenLdapContainer> {

	private static final int DEFAULT_LDAP_PORT = 389;

	public OpenLdapContainer(DockerImageName dockerImageName) {
		super(dockerImageName);
		addExposedPorts(DEFAULT_LDAP_PORT);
	}

}
