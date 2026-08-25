/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.ldap.autoconfigure;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.ldap.autoconfigure.LdapAutoConfiguration.LdapAutoConfigurationRuntimeHints;
import org.springframework.boot.ldap.autoconfigure.LdapProperties.Template;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.env.Environment;
import org.springframework.ldap.convert.ConverterUtils;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.DirContextAuthenticationStrategy;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.odm.core.ObjectDirectoryMapper;
import org.springframework.ldap.odm.core.impl.DefaultObjectDirectoryMapper;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for LDAP.
 *
 * @author Eddú Meléndez
 * @author Vedran Pavic
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(ContextSource.class)
@EnableConfigurationProperties(LdapProperties.class)
@ImportRuntimeHints(LdapAutoConfigurationRuntimeHints.class)
public final class LdapAutoConfiguration {

	private static final String SOCKET_FACTORY_ENV_KEY = "java.naming.ldap.factory.socket";

	@Bean
	@ConditionalOnMissingBean(LdapConnectionDetails.class)
	PropertiesLdapConnectionDetails propertiesLdapConnectionDetails(LdapProperties properties, Environment environment,
			ObjectProvider<SslBundles> sslBundles) {
		return new PropertiesLdapConnectionDetails(properties, environment, sslBundles.getIfAvailable());
	}

	@Bean
	@ConditionalOnMissingBean
	LdapContextSource ldapContextSource(LdapConnectionDetails connectionDetails, LdapProperties properties,
			ObjectProvider<DirContextAuthenticationStrategy> dirContextAuthenticationStrategy) {
		LdapContextSource source = new LdapContextSource();
		dirContextAuthenticationStrategy.ifUnique(source::setAuthenticationStrategy);
		PropertyMapper propertyMapper = PropertyMapper.get();
		propertyMapper.from(connectionDetails.getUsername()).to(source::setUserDn);
		propertyMapper.from(connectionDetails.getPassword()).to(source::setPassword);
		propertyMapper.from(properties.getAnonymousReadOnly()).to(source::setAnonymousReadOnly);
		propertyMapper.from(properties.getReferral())
			.as(((referral) -> referral.name().toLowerCase(Locale.ROOT)))
			.to(source::setReferral);
		propertyMapper.from(connectionDetails.getBase()).to(source::setBase);
		propertyMapper.from(connectionDetails.getUrls()).to(source::setUrls);
		source.setBaseEnvironmentProperties(baseEnvironmentProperties(connectionDetails, properties));
		return source;
	}

	/**
	 * Returns the JNDI environment shared by the anonymous and the authenticated
	 * environment of the context source. The SSL bundle is applied here rather than
	 * through a {@link DirContextAuthenticationStrategy} as the strategy is not consulted
	 * when read-only operations use an anonymous environment.
	 * <p>
	 * A socket factory in the base environment conflicts with the bundle and is rejected,
	 * but only when the bundle also came from the properties. A bundle from another
	 * {@link LdapConnectionDetails} bean takes precedence over the properties instead.
	 * @param connectionDetails the connection details
	 * @param properties the LDAP properties
	 * @return the base environment properties
	 */
	private Map<String, Object> baseEnvironmentProperties(LdapConnectionDetails connectionDetails,
			LdapProperties properties) {
		Map<String, Object> baseEnvironment = new LinkedHashMap<>(properties.getBaseEnvironment());
		SslBundle sslBundle = connectionDetails.getSslBundle();
		if (sslBundle != null) {
			Assert.state(usesLdaps(connectionDetails.getUrls()),
					"SSL bundle has been configured but not all LDAP URLs use the 'ldaps' scheme");
			if (connectionDetails instanceof PropertiesLdapConnectionDetails) {
				Assert.state(!baseEnvironment.containsKey(SOCKET_FACTORY_ENV_KEY),
						() -> "SSL bundle has been configured but '" + SOCKET_FACTORY_ENV_KEY
								+ "' has also been set in the base environment. Use either an SSL bundle or your own socket factory, not both");
			}
			LdapSslSocketFactory.setSslBundle(sslBundle);
			baseEnvironment.put(SOCKET_FACTORY_ENV_KEY, LdapSslSocketFactory.class.getName());
		}
		return Collections.unmodifiableMap(baseEnvironment);
	}

	private boolean usesLdaps(String[] urls) {
		return !ObjectUtils.isEmpty(urls)
				&& Arrays.stream(urls).allMatch((url) -> url.toLowerCase(Locale.ROOT).startsWith("ldaps://"));
	}

	@Bean
	@ConditionalOnMissingBean
	ObjectDirectoryMapper objectDirectoryMapper() {
		ApplicationConversionService conversionService = new ApplicationConversionService();
		ConverterUtils.addDefaultConverters(conversionService);
		DefaultObjectDirectoryMapper objectDirectoryMapper = new DefaultObjectDirectoryMapper();
		objectDirectoryMapper.setConversionService(conversionService);
		return objectDirectoryMapper;
	}

	@Bean
	@ConditionalOnMissingBean(LdapOperations.class)
	LdapTemplate ldapTemplate(LdapProperties properties, ContextSource contextSource,
			ObjectDirectoryMapper objectDirectoryMapper) {
		Template template = properties.getTemplate();
		PropertyMapper propertyMapper = PropertyMapper.get();
		LdapTemplate ldapTemplate = new LdapTemplate(contextSource);
		ldapTemplate.setObjectDirectoryMapper(objectDirectoryMapper);
		propertyMapper.from(template.isIgnorePartialResultException())
			.to(ldapTemplate::setIgnorePartialResultException);
		propertyMapper.from(template.isIgnoreNameNotFoundException()).to(ldapTemplate::setIgnoreNameNotFoundException);
		propertyMapper.from(template.isIgnoreSizeLimitExceededException())
			.to(ldapTemplate::setIgnoreSizeLimitExceededException);
		return ldapTemplate;
	}

	static class LdapAutoConfigurationRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.reflection().registerType(LdapSslSocketFactory.class, MemberCategory.INVOKE_PUBLIC_METHODS);
		}

	}

}
