/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.ldap.autoconfigure;

import javax.naming.ldap.LdapContext;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.data.ldap.repository.support.LdapRepositoryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's LDAP Repositories.
 *
 * @author Eddú Meléndez
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ LdapContext.class, LdapRepository.class })
@ConditionalOnBooleanProperty(name = "spring.data.ldap.repositories.enabled", matchIfMissing = true)
@ConditionalOnMissingBean(LdapRepositoryFactoryBean.class)
@Import(DataLdapRepositoriesRegistrar.class)
public final class DataLdapRepositoriesAutoConfiguration {

}
