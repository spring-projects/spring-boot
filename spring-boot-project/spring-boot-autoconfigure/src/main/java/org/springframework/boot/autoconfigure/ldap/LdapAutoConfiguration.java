/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.ldap;

import java.util.Collections;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.ldap.LdapProperties.Template;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.DirContextAuthenticationStrategy;
import org.springframework.ldap.core.support.LdapContextSource;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for LDAP.
 *
 * @author Eddú Meléndez
 * @author Vedran Pavic
 * @since 1.5.0
 */
@AutoConfiguration
@ConditionalOnClass(ContextSource.class)
@EnableConfigurationProperties(LdapProperties.class)
public class LdapAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(LdapConnectionDetails.class)
	PropertiesLdapConnectionDetails propertiesLdapConnectionDetails(LdapProperties properties,
			Environment environment) {
		return new PropertiesLdapConnectionDetails(properties, environment);
	}

	@Bean
	@ConditionalOnMissingBean
	public LdapContextSource ldapContextSource(LdapConnectionDetails connectionDetails, LdapProperties properties,
			ObjectProvider<DirContextAuthenticationStrategy> dirContextAuthenticationStrategy) {
		LdapContextSource source = new LdapContextSource();
		dirContextAuthenticationStrategy.ifUnique(source::setAuthenticationStrategy);
		PropertyMapper propertyMapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
		propertyMapper.from(connectionDetails.getUsername()).to(source::setUserDn);
		propertyMapper.from(connectionDetails.getPassword()).to(source::setPassword);
		propertyMapper.from(properties.getAnonymousReadOnly()).to(source::setAnonymousReadOnly);
		propertyMapper.from(connectionDetails.getBase()).to(source::setBase);
		propertyMapper.from(connectionDetails.getUrls()).to(source::setUrls);
		propertyMapper.from(properties.getBaseEnvironment())
			.to((baseEnvironment) -> source.setBaseEnvironmentProperties(Collections.unmodifiableMap(baseEnvironment)));
		return source;
	}

	@Bean
	@ConditionalOnMissingBean(LdapOperations.class)
	public LdapTemplate ldapTemplate(LdapProperties properties, ContextSource contextSource) {
		Template template = properties.getTemplate();
		PropertyMapper propertyMapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
		LdapTemplate ldapTemplate = new LdapTemplate(contextSource);
		propertyMapper.from(template.isIgnorePartialResultException())
			.to(ldapTemplate::setIgnorePartialResultException);
		propertyMapper.from(template.isIgnoreNameNotFoundException()).to(ldapTemplate::setIgnoreNameNotFoundException);
		propertyMapper.from(template.isIgnoreSizeLimitExceededException())
			.to(ldapTemplate::setIgnoreSizeLimitExceededException);
		return ldapTemplate;
	}

}
