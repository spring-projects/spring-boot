/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.Set;

import org.quartz.SchedulerException;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.actuate.quartz.QuartzEndpoint.QuartzGroupsDescriptor;
import org.springframework.boot.actuate.quartz.QuartzEndpoint.QuartzJobDetailsDescriptor;
import org.springframework.boot.actuate.quartz.QuartzEndpoint.QuartzJobGroupSummaryDescriptor;
import org.springframework.boot.actuate.quartz.QuartzEndpoint.QuartzTriggerGroupSummaryDescriptor;
import org.springframework.boot.actuate.quartz.QuartzEndpointWebExtension.QuartzEndpointWebExtensionRuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * {@link EndpointWebExtension @EndpointWebExtension} for the {@link QuartzEndpoint}.
 *
 * @author Stephane Nicoll
 * @since 2.5.0
 */
@EndpointWebExtension(endpoint = QuartzEndpoint.class)
@ImportRuntimeHints(QuartzEndpointWebExtensionRuntimeHints.class)
public class QuartzEndpointWebExtension {

	private final QuartzEndpoint delegate;

	private final Show showValues;

	private final Set<String> roles;

	public QuartzEndpointWebExtension(QuartzEndpoint delegate, Show showValues, Set<String> roles) {
		this.delegate = delegate;
		this.showValues = showValues;
		this.roles = roles;
	}

	@ReadOperation
	public WebEndpointResponse<QuartzGroupsDescriptor> quartzJobOrTriggerGroups(@Selector String jobsOrTriggers)
			throws SchedulerException {
		return handle(jobsOrTriggers, this.delegate::quartzJobGroups, this.delegate::quartzTriggerGroups);
	}

	@ReadOperation
	public WebEndpointResponse<Object> quartzJobOrTriggerGroup(@Selector String jobsOrTriggers, @Selector String group)
			throws SchedulerException {
		return handle(jobsOrTriggers, () -> this.delegate.quartzJobGroupSummary(group),
				() -> this.delegate.quartzTriggerGroupSummary(group));
	}

	@ReadOperation
	public WebEndpointResponse<Object> quartzJobOrTrigger(SecurityContext securityContext,
			@Selector String jobsOrTriggers, @Selector String group, @Selector String name) throws SchedulerException {
		boolean showUnsanitized = this.showValues.isShown(securityContext, this.roles);
		return handle(jobsOrTriggers, () -> this.delegate.quartzJob(group, name, showUnsanitized),
				() -> this.delegate.quartzTrigger(group, name, showUnsanitized));
	}

	private <T> WebEndpointResponse<T> handle(String jobsOrTriggers, ResponseSupplier<T> jobAction,
			ResponseSupplier<T> triggerAction) throws SchedulerException {
		if ("jobs".equals(jobsOrTriggers)) {
			return handleNull(jobAction.get());
		}
		if ("triggers".equals(jobsOrTriggers)) {
			return handleNull(triggerAction.get());
		}
		return new WebEndpointResponse<>(WebEndpointResponse.STATUS_BAD_REQUEST);
	}

	private <T> WebEndpointResponse<T> handleNull(T value) {
		if (value != null) {
			return new WebEndpointResponse<>(value);
		}
		return new WebEndpointResponse<>(WebEndpointResponse.STATUS_NOT_FOUND);
	}

	@FunctionalInterface
	private interface ResponseSupplier<T> {

		T get() throws SchedulerException;

	}

	static class QuartzEndpointWebExtensionRuntimeHints implements RuntimeHintsRegistrar {

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			this.bindingRegistrar.registerReflectionHints(hints.reflection(), QuartzGroupsDescriptor.class,
					QuartzJobDetailsDescriptor.class, QuartzJobGroupSummaryDescriptor.class,
					QuartzTriggerGroupSummaryDescriptor.class);
		}

	}

}
