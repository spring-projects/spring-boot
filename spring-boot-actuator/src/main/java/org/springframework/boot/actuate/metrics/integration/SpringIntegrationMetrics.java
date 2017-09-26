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

import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
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
		bindChannelNames(registry);
		bindHandlerNames(registry);
		bindSourceNames(registry);
		this.registries.add(registry);
	}

	private void bindChannelNames(MeterRegistry registry) {
		Id id = registry.createId("spring.integration.channelNames", this.tags,
				"The number of spring integration channels");
		registry.gauge(id, this.configurer, (c) -> c.getChannelNames().length);
	}

	private void bindHandlerNames(MeterRegistry registry) {
		Id id = registry.createId("spring.integration.handlerNames", this.tags,
				"The number of spring integration handlers");
		registry.gauge(id, this.configurer, (c) -> c.getHandlerNames().length);
	}

	private void bindSourceNames(MeterRegistry registry) {
		Id id = registry.createId("spring.integration.sourceNames", this.tags,
				"The number of spring integration sources");
		registry.gauge(id, this.configurer, (c) -> c.getSourceNames().length);
	}

	private void addSourceMetrics(MeterRegistry registry) {
		for (String sourceName : this.configurer.getSourceNames()) {
			addSourceMetrics(registry, sourceName);
		}
	}

	private void addSourceMetrics(MeterRegistry registry, String sourceName) {
		MessageSourceMetrics sourceMetrics = this.configurer.getSourceMetrics(sourceName);
		Iterable<Tag> tags = Tags.concat(this.tags, "source", sourceName);
		Id id = registry.createId("spring.integration.source.messages", tags,
				"The number of successful handler calls");
		registry.more().counter(id, sourceMetrics, MessageSourceMetrics::getMessageCount);
	}

	private void addHandlerMetrics(MeterRegistry registry) {
		for (String handler : this.configurer.getHandlerNames()) {
			addHandlerMetrics(registry, handler);
		}
	}

	private void addHandlerMetrics(MeterRegistry registry, String handler) {
		MessageHandlerMetrics handlerMetrics = this.configurer.getHandlerMetrics(handler);
		// FIXME could use improvement to dynamically compute the handler name with its
		// ID, which can change after
		// creation as shown in the SpringIntegrationApplication sample.
		Iterable<Tag> tags = Tags.concat(this.tags, "handler", handler);
		addDurationMax(registry, handlerMetrics, tags);
		addDurationMin(registry, handlerMetrics, tags);
		addDurationMean(registry, handlerMetrics, tags);
		addActiveCount(registry, handlerMetrics, tags);
	}

	private void addDurationMax(MeterRegistry registry,
			MessageHandlerMetrics handlerMetrics, Iterable<Tag> tags) {
		Id id = registry.createId("spring.integration.handler.duration.max", tags,
				"The maximum handler duration");
		registry.more().timeGauge(id, handlerMetrics, TimeUnit.MILLISECONDS,
				MessageHandlerMetrics::getMaxDuration);
	}

	private void addDurationMin(MeterRegistry registry,
			MessageHandlerMetrics handlerMetrics, Iterable<Tag> tags) {
		Id id = registry.createId("spring.integration.handler.duration.min", tags,
				"The minimum handler duration");
		registry.more().timeGauge(id, handlerMetrics, TimeUnit.MILLISECONDS,
				MessageHandlerMetrics::getMinDuration);
	}

	private void addDurationMean(MeterRegistry registry,
			MessageHandlerMetrics handlerMetrics, Iterable<Tag> tags) {
		Id id = registry.createId("spring.integration.handler.duration.mean", tags,
				"The mean handler duration");
		registry.more().timeGauge(id, handlerMetrics, TimeUnit.MILLISECONDS,
				MessageHandlerMetrics::getMeanDuration);
	}

	private void addActiveCount(MeterRegistry registry,
			MessageHandlerMetrics handlerMetrics, Iterable<Tag> tags) {
		Id id = registry.createId("spring.integration.handler.activeCount", tags,
				"The number of active handlers");
		registry.gauge(id, handlerMetrics, MessageHandlerMetrics::getActiveCount);
	}

	private void addChannelMetrics(MeterRegistry registry) {
		for (String channel : this.configurer.getChannelNames()) {
			addChannelMetrics(registry, channel);
		}
	}

	private void addChannelMetrics(MeterRegistry registry, String channel) {
		Iterable<Tag> tags = Tags.concat(this.tags, "channel", channel);
		MessageChannelMetrics channelMetrics = this.configurer.getChannelMetrics(channel);
		addSendErrors(registry, tags, channelMetrics);
		addChannelSends(registry, tags, channelMetrics);
		if (channelMetrics instanceof PollableChannelManagement) {
			addReceives(registry, tags, channelMetrics);
		}
	}

	private void addSendErrors(MeterRegistry registry, Iterable<Tag> tags,
			MessageChannelMetrics channelMetrics) {
		Id id = registry.createId("spring.integration.channel.sendErrors", tags,
				"The number of failed sends (either throwing an exception or "
						+ "rejected by the channel)");
		registry.more().counter(id, channelMetrics,
				MessageChannelMetrics::getSendErrorCount);
	}

	private void addReceives(MeterRegistry registry, Iterable<Tag> tags,
			MessageChannelMetrics channelMetrics) {
		Id id = registry.createId("spring.integration.channel.receives", tags,
				"The number of messages received");
		registry.more().counter(id, (PollableChannelManagement) channelMetrics,
				PollableChannelManagement::getReceiveCount);
	}

	private void addChannelSends(MeterRegistry registry, Iterable<Tag> tags,
			MessageChannelMetrics channelMetrics) {
		Id id = registry.createId("spring.integration.channel.sends", tags,
				"The number of successful sends");
		registry.more().counter(id, channelMetrics, MessageChannelMetrics::getSendCount);
	}

	@Override
	public void afterSingletonsInstantiated() {
		// FIXME better would be to use a BeanPostProcessor
		this.configurer.afterSingletonsInstantiated();
		this.registries.forEach((registry) -> {
			addChannelMetrics(registry);
			addHandlerMetrics(registry);
			addSourceMetrics(registry);
		});
	}

}
