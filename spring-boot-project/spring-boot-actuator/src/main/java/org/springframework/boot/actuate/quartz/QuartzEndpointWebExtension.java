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

	/**
     * Constructs a new QuartzEndpointWebExtension with the specified delegate, showValues, and roles.
     * 
     * @param delegate the QuartzEndpoint to delegate to
     * @param showValues the Show object representing the values to show
     * @param roles the Set of roles allowed to access the endpoint
     */
    public QuartzEndpointWebExtension(QuartzEndpoint delegate, Show showValues, Set<String> roles) {
		this.delegate = delegate;
		this.showValues = showValues;
		this.roles = roles;
	}

	/**
     * Retrieves the groups of Quartz jobs or triggers.
     * 
     * @param jobsOrTriggers The type of groups to retrieve (either "jobs" or "triggers").
     * @return The response containing the descriptor of the Quartz groups.
     * @throws SchedulerException If an error occurs while retrieving the groups.
     */
    @ReadOperation
	public WebEndpointResponse<QuartzGroupsDescriptor> quartzJobOrTriggerGroups(@Selector String jobsOrTriggers)
			throws SchedulerException {
		return handle(jobsOrTriggers, this.delegate::quartzJobGroups, this.delegate::quartzTriggerGroups);
	}

	/**
     * Retrieves the summary of Quartz jobs or triggers in a specific group.
     * 
     * @param jobsOrTriggers The type of entities to retrieve (jobs or triggers).
     * @param group The group name to filter the entities.
     * @return The summary of the Quartz jobs or triggers in the specified group.
     * @throws SchedulerException If an error occurs while retrieving the summary.
     */
    @ReadOperation
	public WebEndpointResponse<Object> quartzJobOrTriggerGroup(@Selector String jobsOrTriggers, @Selector String group)
			throws SchedulerException {
		return handle(jobsOrTriggers, () -> this.delegate.quartzJobGroupSummary(group),
				() -> this.delegate.quartzTriggerGroupSummary(group));
	}

	/**
     * Retrieves information about a Quartz job or trigger.
     * 
     * @param securityContext The security context.
     * @param jobsOrTriggers The type of object to retrieve (job or trigger).
     * @param group The group of the job or trigger.
     * @param name The name of the job or trigger.
     * @return The response containing the information about the requested job or trigger.
     * @throws SchedulerException If an error occurs while retrieving the job or trigger.
     */
    @ReadOperation
	public WebEndpointResponse<Object> quartzJobOrTrigger(SecurityContext securityContext,
			@Selector String jobsOrTriggers, @Selector String group, @Selector String name) throws SchedulerException {
		boolean showUnsanitized = this.showValues.isShown(securityContext, this.roles);
		return handle(jobsOrTriggers, () -> this.delegate.quartzJob(group, name, showUnsanitized),
				() -> this.delegate.quartzTrigger(group, name, showUnsanitized));
	}

	/**
     * Handles the request for either "jobs" or "triggers" and returns the appropriate response.
     * 
     * @param jobsOrTriggers The type of request, either "jobs" or "triggers".
     * @param jobAction The supplier function to execute for job requests.
     * @param triggerAction The supplier function to execute for trigger requests.
     * @return The response containing the result of the executed action.
     * @throws SchedulerException If an error occurs while executing the action.
     */
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

	/**
     * Handles null values by returning a WebEndpointResponse object.
     * 
     * @param value the value to be handled
     * @return a WebEndpointResponse object containing the value if it is not null, otherwise a WebEndpointResponse object with a status of STATUS_NOT_FOUND
     */
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

	/**
     * QuartzEndpointWebExtensionRuntimeHints class.
     */
    static class QuartzEndpointWebExtensionRuntimeHints implements RuntimeHintsRegistrar {

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		/**
         * Registers the runtime hints for the QuartzEndpointWebExtension class.
         * 
         * @param hints the runtime hints object
         * @param classLoader the class loader to use for reflection
         */
        @Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			this.bindingRegistrar.registerReflectionHints(hints.reflection(), QuartzGroupsDescriptor.class,
					QuartzJobDetailsDescriptor.class, QuartzJobGroupSummaryDescriptor.class,
					QuartzTriggerGroupSummaryDescriptor.class);
		}

	}

}
