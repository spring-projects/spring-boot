/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ldap.testcontainers;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.ldap.autoconfigure.LdapAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.OpenLdapContainer;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OpenLdapContainerConnectionDetailsFactory}.
 *
 * @author Philipp Kessler
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class OpenLdapContainerConnectionDetailsFactoryIntegrationTests {

	@Container
	@ServiceConnection
	static final OpenLdapContainer openLdap = TestImage.forContainer(OpenLdapContainer.class)
		.withEnv("LDAP_TLS", "false");

	@Autowired
	private LdapTemplate ldapTemplate;

	@Test
	void connectionCanBeMadeToLdapContainer() {
		List<String> cn = this.ldapTemplate.search(LdapQueryBuilder.query().where("objectclass").is("dcObject"),
				(AttributesMapper<String>) (attributes) -> attributes.get("dc").get().toString());
		assertThat(cn).singleElement().isEqualTo("example");
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ LdapAutoConfiguration.class })
	static class TestConfiguration {

	}

}
