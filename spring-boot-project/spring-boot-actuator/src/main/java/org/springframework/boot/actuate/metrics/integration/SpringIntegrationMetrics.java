/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.metrics.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.integration.support.management.*;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * A {@link MeterBinder} for Spring Integration metrics.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
public class SpringIntegrationMetrics implements MeterBinder, SmartInitializingSingleton {

	private final Iterable<Tag> tags;

	private Collection<MeterRegistry> registries = new ArrayList<>();

	private final IntegrationManagementConfigurer configurer;

	public SpringIntegrationMetrics(IntegrationManagementConfigurer configurer) {
		this(configurer, Collections.emptyList());
	}

	public SpringIntegrationMetrics(IntegrationManagementConfigurer configurer,
			Iterable<Tag> tags) {
		this.configurer = configurer;
		this.tags = tags;
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		Gauge.builder("spring.integration.channelNames", this.configurer,
				c -> c.getChannelNames().length).tags(tags)
				.description("The number of spring integration channels")
				.register(registry);

		Gauge.builder("spring.integration.handlerNames", configurer,
				c -> c.getHandlerNames().length).tags(this.tags)
				.description("The number of spring integration handlers")
				.register(registry);

		Gauge.builder("spring.integration.sourceNames", configurer,
				c -> c.getSourceNames().length).tags(this.tags)
				.description("The number of spring integration sources")
				.register(registry);

		this.registries.add(registry);
	}

	private void addSourceMetrics(MeterRegistry registry) {
		for (String source : this.configurer.getSourceNames()) {
			MessageSourceMetrics sourceMetrics = this.configurer.getSourceMetrics(source);
			Iterable<Tag> tagsWithSource = Tags.concat(this.tags, "source", source);

			FunctionCounter
					.builder("spring.integration.source.messages", sourceMetrics,
							MessageSourceMetrics::getMessageCount)
					.tags(tagsWithSource)
					.description("The number of successful handler calls")
					.register(registry);
		}
	}

	private void addHandlerMetrics(MeterRegistry registry) {
		for (String handler : this.configurer.getHandlerNames()) {
			MessageHandlerMetrics handlerMetrics = this.configurer
					.getHandlerMetrics(handler);

			// TODO could use improvement to dynamically commute the handler name with its
			// ID, which can change after creation as shown in the
			// SpringIntegrationApplication sample.
			Iterable<Tag> tagsWithHandler = Tags.concat(this.tags, "handler", handler);

			TimeGauge
					.builder("spring.integration.handler.duration.max", handlerMetrics,
							TimeUnit.MILLISECONDS, MessageHandlerMetrics::getMaxDuration)
					.tags(tagsWithHandler).description("The maximum handler duration")
					.register(registry);

			TimeGauge
					.builder("spring.integration.handler.duration.min", handlerMetrics,
							TimeUnit.MILLISECONDS, MessageHandlerMetrics::getMinDuration)
					.tags(tagsWithHandler).description("The minimum handler duration")
					.register(registry);

			TimeGauge
					.builder("spring.integration.handler.duration.mean", handlerMetrics,
							TimeUnit.MILLISECONDS, MessageHandlerMetrics::getMeanDuration)
					.tags(tagsWithHandler).description("The mean handler duration")
					.register(registry);

			Gauge.builder("spring.integration.handler.activeCount", handlerMetrics,
					MessageHandlerMetrics::getActiveCount).tags(tagsWithHandler)
					.description("The number of active handlers").register(registry);
		}
	}

	private void addChannelMetrics(MeterRegistry registry) {
		for (String channel : this.configurer.getChannelNames()) {
			MessageChannelMetrics channelMetrics = this.configurer
					.getChannelMetrics(channel);
			Iterable<Tag> tagsWithChannel = Tags.concat(this.tags, "channel", channel);

			FunctionCounter
					.builder("spring.integration.channel.sendErrors", channelMetrics,
							MessageChannelMetrics::getSendErrorCount)
					.tags(tagsWithChannel)
					.description(
							"The number of failed sends (either throwing an exception or rejected by the channel)")
					.register(registry);

			FunctionCounter
					.builder("spring.integration.channel.sends", channelMetrics,
							MessageChannelMetrics::getSendCount)
					.tags(tagsWithChannel).description("The number of successful sends")
					.register(registry);

			if (channelMetrics instanceof PollableChannelManagement) {
				FunctionCounter
						.builder("spring.integration.receives",
								(PollableChannelManagement) channelMetrics,
								PollableChannelManagement::getReceiveCount)
						.tags(tagsWithChannel)
						.description("The number of messages received")
						.register(registry);
			}
		}
	}

	@Override
	public void afterSingletonsInstantiated() {
		this.registries.forEach((registry) -> {
			addChannelMetrics(registry);
			addHandlerMetrics(registry);
			addSourceMetrics(registry);
		});
	}

}
