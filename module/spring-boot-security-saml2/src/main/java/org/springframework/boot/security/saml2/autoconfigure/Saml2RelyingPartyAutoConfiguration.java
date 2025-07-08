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

package org.springframework.boot.security.saml2.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.actuate.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.servlet.SecurityAutoConfiguration;
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
public class Saml2RelyingPartyAutoConfiguration {

}
