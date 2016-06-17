/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.integration;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.integration.http.support.HttpContextUtils;

/**
 * Configuration properties for Spring Integration.
 *
 * @author Artem Bilan
 * @since 1.4
 * @see org.springframework.integration.context.IntegrationProperties
 */
@ConfigurationProperties("spring.integration")
public class IntegrationProperties {

	private final Channels channels = new Channels();

	/**
	 * The number of threads available in the default taskScheduler.
	 */
	private int taskSchedulerPoolSize = 10;

	/**
	 * When {@code true}, messages that arrive at a gateway reply channel will throw an exception.
	 */
	private boolean throwExceptionOnLateReply = false;

	/**
	 * When {@code true}, Messaging Annotation Support requires a declaration of the {@code @MessageEndpoint}.
	 */
	private boolean componentAnnotationRequired = false;

	/**
	 * Path that serves as the base URI for the Integration Graph Controller.
	 */
	@NotNull
	@Pattern(regexp = "/[^?#]*", message = "Path must start with /")
	private String graphControllerPath = HttpContextUtils.GRAPH_CONTROLLER_DEFAULT_PATH;


	public String getGraphControllerPath() {
		return this.graphControllerPath;
	}

	public void setGraphControllerPath(String graphControllerPath) {
		this.graphControllerPath = graphControllerPath;
	}

	public Channels getChannels() {
		return this.channels;
	}

	public int getTaskSchedulerPoolSize() {
		return this.taskSchedulerPoolSize;
	}

	public void setTaskSchedulerPoolSize(int taskSchedulerPoolSize) {
		this.taskSchedulerPoolSize = taskSchedulerPoolSize;
	}

	public boolean isThrowExceptionOnLateReply() {
		return this.throwExceptionOnLateReply;
	}

	public void setThrowExceptionOnLateReply(boolean throwExceptionOnLateReply) {
		this.throwExceptionOnLateReply = throwExceptionOnLateReply;
	}

	public boolean isComponentAnnotationRequired() {
		return this.componentAnnotationRequired;
	}

	public void setComponentAnnotationRequired(boolean componentAnnotationRequired) {
		this.componentAnnotationRequired = componentAnnotationRequired;
	}

	public static class Channels {

		/**
		 * When true, {@code input-channel}s will be automatically declared as {@code DirectChannel}s
		 * when not explicitly found in the application context.
		 */
		private boolean autoCreate = true;

		/**
		 * This property provides the default number of subscribers allowed on, say, a {@code DirectChannel}.
		 */
		private int maxUnicastSubscribers = Integer.MAX_VALUE;

		/**
		 * This property provides the default number of subscribers allowed on, say, a {@code PublishSubscribeChannel}.
		 */
		private int maxBroadcastSubscribers = Integer.MAX_VALUE;

		public boolean isAutoCreate() {
			return this.autoCreate;
		}

		public void setAutoCreate(boolean autoCreate) {
			this.autoCreate = autoCreate;
		}

		public int getMaxUnicastSubscribers() {
			return this.maxUnicastSubscribers;
		}

		public void setMaxUnicastSubscribers(int maxUnicastSubscribers) {
			this.maxUnicastSubscribers = maxUnicastSubscribers;
		}

		public int getMaxBroadcastSubscribers() {
			return this.maxBroadcastSubscribers;
		}

		public void setMaxBroadcastSubscribers(int maxBroadcastSubscribers) {
			this.maxBroadcastSubscribers = maxBroadcastSubscribers;
		}

	}

}
