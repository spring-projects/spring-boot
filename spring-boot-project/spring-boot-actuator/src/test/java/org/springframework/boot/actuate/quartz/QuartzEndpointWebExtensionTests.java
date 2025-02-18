/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.quartz;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.actuate.quartz.QuartzEndpoint.QuartzGroupsDescriptor;
import org.springframework.boot.actuate.quartz.QuartzEndpoint.QuartzJobDetailsDescriptor;
import org.springframework.boot.actuate.quartz.QuartzEndpoint.QuartzJobGroupSummaryDescriptor;
import org.springframework.boot.actuate.quartz.QuartzEndpoint.QuartzTriggerGroupSummaryDescriptor;
import org.springframework.boot.actuate.quartz.QuartzEndpointWebExtension.QuartzEndpointWebExtensionRuntimeHints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link QuartzEndpointWebExtension}.
 *
 * @author Moritz Halbritter
 * @author Madhura Bhave
 */
class QuartzEndpointWebExtensionTests {

	private QuartzEndpointWebExtension webExtension;

	private QuartzEndpoint delegate;

	@BeforeEach
	void setup() {
		this.delegate = mock(QuartzEndpoint.class);
	}

	@Test
	void whenShowValuesIsNever() throws Exception {
		this.webExtension = new QuartzEndpointWebExtension(this.delegate, Show.NEVER, Collections.emptySet());
		this.webExtension.quartzJobOrTrigger(null, "jobs", "a", "b");
		this.webExtension.quartzJobOrTrigger(null, "triggers", "a", "b");
		then(this.delegate).should().quartzJob("a", "b", false);
		then(this.delegate).should().quartzTrigger("a", "b", false);
	}

	@Test
	void whenShowValuesIsAlways() throws Exception {
		this.webExtension = new QuartzEndpointWebExtension(this.delegate, Show.ALWAYS, Collections.emptySet());
		this.webExtension.quartzJobOrTrigger(null, "a", "b", "c");
		this.webExtension.quartzJobOrTrigger(null, "jobs", "a", "b");
		this.webExtension.quartzJobOrTrigger(null, "triggers", "a", "b");
		then(this.delegate).should().quartzJob("a", "b", true);
		then(this.delegate).should().quartzTrigger("a", "b", true);
	}

	@Test
	void whenShowValuesIsWhenAuthorizedAndSecurityContextIsAuthorized() throws Exception {
		SecurityContext securityContext = mock(SecurityContext.class);
		given(securityContext.getPrincipal()).willReturn(mock(Principal.class));
		this.webExtension = new QuartzEndpointWebExtension(this.delegate, Show.WHEN_AUTHORIZED, Collections.emptySet());
		this.webExtension.quartzJobOrTrigger(securityContext, "jobs", "a", "b");
		this.webExtension.quartzJobOrTrigger(securityContext, "triggers", "a", "b");
		then(this.delegate).should().quartzJob("a", "b", true);
		then(this.delegate).should().quartzTrigger("a", "b", true);
	}

	@Test
	void whenShowValuesIsWhenAuthorizedAndSecurityContextIsNotAuthorized() throws Exception {
		SecurityContext securityContext = mock(SecurityContext.class);
		this.webExtension = new QuartzEndpointWebExtension(this.delegate, Show.WHEN_AUTHORIZED, Collections.emptySet());
		this.webExtension.quartzJobOrTrigger(securityContext, "jobs", "a", "b");
		this.webExtension.quartzJobOrTrigger(securityContext, "triggers", "a", "b");
		then(this.delegate).should().quartzJob("a", "b", false);
		then(this.delegate).should().quartzTrigger("a", "b", false);
	}

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new QuartzEndpointWebExtensionRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		Set<Class<?>> bindingTypes = Set.of(QuartzGroupsDescriptor.class, QuartzJobDetailsDescriptor.class,
				QuartzJobGroupSummaryDescriptor.class, QuartzTriggerGroupSummaryDescriptor.class);
		for (Class<?> bindingType : bindingTypes) {
			assertThat(RuntimeHintsPredicates.reflection()
				.onType(bindingType)
				.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS))
				.accepts(runtimeHints);
		}
	}

}
