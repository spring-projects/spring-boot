/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.saml2.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Security's SAML 2.0
 * authentication support.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
@AutoConfiguration(before = { SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class })
@ConditionalOnClass(RelyingPartyRegistrationRepository.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@Import({ Saml2RelyingPartyRegistrationConfiguration.class, Saml2LoginConfiguration.class })
@EnableConfigurationProperties(Saml2RelyingPartyProperties.class)
public final class Saml2RelyingPartyAutoConfiguration {

}
