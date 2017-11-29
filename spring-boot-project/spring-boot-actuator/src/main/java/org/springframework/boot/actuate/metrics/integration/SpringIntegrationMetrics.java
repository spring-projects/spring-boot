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
import java.util.function.ToDoubleFunction;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.binder.MeterBinder;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.integration.support.management.IntegrationManagementConfigurer;
import org.springframework.integration.support.management.MessageChannelMetrics;
import org.springframework.integration.support.management.MessageHandlerMetrics;
import org.springframework.integration.support.management.MessageSourceMetrics;
import org.springframework.integration.support.management.PollableChannelManagement;

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
		registerGauge(registry, this.configurer, this.tags,
				"spring.integration.channelNames",
				"The number of spring integration channels",
				(configurer) -> configurer.getChannelNames().length);
		registerGauge(registry, this.configurer, this.tags,
				"spring.integration.handlerNames",
				"The number of spring integration handlers",
				(configurer) -> configurer.getHandlerNames().length);
		registerGauge(registry, this.configurer, this.tags,
				"spring.integration.sourceNames",
				"The number of spring integration sources",
				(configurer) -> configurer.getSourceNames().length);
		this.registries.add(registry);
	}

	private void addSourceMetrics(MeterRegistry registry) {
		for (String source : this.configurer.getSourceNames()) {
			MessageSourceMetrics sourceMetrics = this.configurer.getSourceMetrics(source);
			Iterable<Tag> tagsWithSource = Tags.concat(this.tags, "source", source);
			registerFunctionCounter(registry, sourceMetrics, tagsWithSource,
					"spring.integration.source.messages",
					"The number of successful handler calls",
					MessageSourceMetrics::getMessageCount);
		}
	}

	private void addHandlerMetrics(MeterRegistry registry) {
		for (String handler : this.configurer.getHandlerNames()) {
			MessageHandlerMetrics handlerMetrics = this.configurer
					.getHandlerMetrics(handler);
			Iterable<Tag> tagsWithHandler = Tags.concat(this.tags, "handler", handler);
			registerTimedGauge(registry, handlerMetrics, tagsWithHandler,
					"spring.integration.handler.duration.max",
					"The maximum handler duration",
					MessageHandlerMetrics::getMaxDuration);
			registerTimedGauge(registry, handlerMetrics, tagsWithHandler,
					"spring.integration.handler.duration.min",
					"The minimum handler duration",
					MessageHandlerMetrics::getMinDuration);
			registerTimedGauge(registry, handlerMetrics, tagsWithHandler,
					"spring.integration.handler.duration.mean",
					"The mean handler duration", MessageHandlerMetrics::getMeanDuration);
			registerGauge(registry, handlerMetrics, tagsWithHandler,
					"spring.integration.handler.activeCount",
					"The number of active handlers",
					MessageHandlerMetrics::getActiveCount);
		}
	}

	private void addChannelMetrics(MeterRegistry registry) {
		for (String channel : this.configurer.getChannelNames()) {
			MessageChannelMetrics channelMetrics = this.configurer
					.getChannelMetrics(channel);
			Iterable<Tag> tagsWithChannel = Tags.concat(this.tags, "channel", channel);
			registerFunctionCounter(registry, channelMetrics, tagsWithChannel,
					"spring.integration.channel.sendErrors",
					"The number of failed sends (either throwing an exception or rejected by the channel)",
					MessageChannelMetrics::getSendErrorCount);
			registerFunctionCounter(registry, channelMetrics, tagsWithChannel,
					"spring.integration.channel.sends", "The number of successful sends",
					MessageChannelMetrics::getSendCount);
			if (channelMetrics instanceof PollableChannelManagement) {
				registerFunctionCounter(registry,
						(PollableChannelManagement) channelMetrics, tagsWithChannel,
						"spring.integration.receives", "The number of messages received",
						PollableChannelManagement::getReceiveCount);
			}
		}
	}

	private <T> void registerGauge(MeterRegistry registry, T object, Iterable<Tag> tags,
			String name, String description, ToDoubleFunction<T> value) {
		Gauge.Builder<?> builder = Gauge.builder(name, object, value);
		builder.tags(this.tags).description(description).register(registry);
	}

	private <T> void registerTimedGauge(MeterRegistry registry, T object,
			Iterable<Tag> tags, String name, String description,
			ToDoubleFunction<T> value) {
		TimeGauge.Builder<?> builder = TimeGauge.builder(name, object,
				TimeUnit.MILLISECONDS, value);
		builder.tags(tags).description(description).register(registry);
	}

	private <T> void registerFunctionCounter(MeterRegistry registry, T object,
			Iterable<Tag> tags, String name, String description,
			ToDoubleFunction<T> value) {
		FunctionCounter.builder(name, object, value).tags(tags).description(description)
				.register(registry);
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
