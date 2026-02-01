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

import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Condition that matches when LDAP StartTLS should be used.
 * <p>
 * StartTLS mode is active when:
 * <ul>
 * <li>SSL bundle is configured ({@code spring.ldap.ssl.bundle})</li>
 * <li>SSL is not explicitly disabled ({@code spring.ldap.ssl.enabled != false})</li>
 * <li>StartTLS is either explicitly enabled ({@code spring.ldap.ssl.start-tls=true}) or
 * the URL scheme is {@code ldap://} (not {@code ldaps://})</li>
 * </ul>
 *
 * @author Massimo Deiana
 */
class OnLdapStartTlsCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String bundle = context.getEnvironment().getProperty("spring.ldap.ssl.bundle");
		if (!StringUtils.hasLength(bundle)) {
			return ConditionOutcome.noMatch("No SSL bundle configured");
		}
		String enabled = context.getEnvironment().getProperty("spring.ldap.ssl.enabled");
		if ("false".equalsIgnoreCase(enabled)) {
			return ConditionOutcome.noMatch("SSL is explicitly disabled");
		}
		String startTls = context.getEnvironment().getProperty("spring.ldap.ssl.start-tls");
		if ("false".equalsIgnoreCase(startTls)) {
			return ConditionOutcome.noMatch("StartTLS is explicitly disabled (using LDAPS mode)");
		}
		if ("true".equalsIgnoreCase(startTls)) {
			return ConditionOutcome.match("StartTLS is explicitly enabled");
		}
		String urls = context.getEnvironment().getProperty("spring.ldap.urls");
		if (urls != null && urls.toLowerCase(Locale.ROOT).contains("ldaps://")) {
			return ConditionOutcome.noMatch("URL scheme is ldaps://, using LDAPS mode");
		}
		return ConditionOutcome.match("SSL bundle configured with ldap:// URL (StartTLS mode)");
	}

}
