/**
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
package org.springframework.boot.actuate.metrics.binder;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.integration.support.management.*;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * @since 2.0.0
 * @author Jon Schneider
 */
public class SpringIntegrationMetrics implements MeterBinder, SmartInitializingSingleton {
    private final Iterable<Tag> tags;
    private Collection<MeterRegistry> registries = new ArrayList<>();

    private final IntegrationManagementConfigurer configurer;

    public SpringIntegrationMetrics(IntegrationManagementConfigurer configurer) {
        this(configurer, emptyList());
    }

    public SpringIntegrationMetrics(IntegrationManagementConfigurer configurer, Iterable<Tag> tags) {
        this.configurer = configurer;
        this.tags = tags;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        registry.gauge(registry.createId("spring.integration.channelNames", tags, "The number of spring integration channels"),
            configurer, c -> c.getChannelNames().length);

        registry.gauge(registry.createId("spring.integration.handlerNames", tags, "The number of spring integration handlers"),
            configurer, c -> c.getHandlerNames().length);

        registry.gauge(registry.createId("spring.integration.sourceNames", tags, "The number of spring integration sources"),
            configurer, c -> c.getSourceNames().length);

        registries.add(registry);
    }

    private void addSourceMetrics(MeterRegistry registry) {
        for (String source : configurer.getSourceNames()) {
            MessageSourceMetrics sourceMetrics = configurer.getSourceMetrics(source);
            Iterable<Tag> tagsWithSource = Tags.concat(tags, "source", source);
            registry.more().counter(registry.createId("spring.integration.source.messages", tagsWithSource, "The number of successful handler calls"),
                sourceMetrics, MessageSourceMetrics::getMessageCount);
        }
    }

    private void addHandlerMetrics(MeterRegistry registry) {
        for (String handler : configurer.getHandlerNames()) {
            MessageHandlerMetrics handlerMetrics = configurer.getHandlerMetrics(handler);

            // TODO could use improvement to dynamically commute the handler name with its ID, which can change after
            // creation as shown in the SpringIntegrationApplication sample.
            Iterable<Tag> tagsWithHandler = Tags.concat(tags, "handler", handler);

            registry.more().timeGauge(registry.createId("spring.integration.handler.duration.max", tagsWithHandler, "The maximum handler duration"),
                handlerMetrics, TimeUnit.MILLISECONDS, MessageHandlerMetrics::getMaxDuration);

            registry.more().timeGauge(registry.createId("spring.integration.handler.duration.min", tagsWithHandler, "The minimum handler duration"),
                handlerMetrics, TimeUnit.MILLISECONDS, MessageHandlerMetrics::getMinDuration);

            registry.more().timeGauge(registry.createId("spring.integration.handler.duration.mean", tagsWithHandler, "The mean handler duration"),
                handlerMetrics, TimeUnit.MILLISECONDS, MessageHandlerMetrics::getMeanDuration);

            registry.gauge(registry.createId("spring.integration.handler.activeCount", tagsWithHandler, "The number of active handlers"),
                handlerMetrics, MessageHandlerMetrics::getActiveCount);
        }
    }

    private void addChannelMetrics(MeterRegistry registry) {
        for (String channel : configurer.getChannelNames()) {
            MessageChannelMetrics channelMetrics = configurer.getChannelMetrics(channel);
            Iterable<Tag> tagsWithChannel = Tags.concat(tags, "channel", channel);

            registry.more().counter(registry.createId("spring.integration.channel.sendErrors", tagsWithChannel,
                "The number of failed sends (either throwing an exception or rejected by the channel)"),
                channelMetrics, MessageChannelMetrics::getSendErrorCount);

            registry.more().counter(registry.createId("spring.integration.channel.sends", tagsWithChannel,
                "The number of successful sends"),
                channelMetrics, MessageChannelMetrics::getSendCount);

            if (channelMetrics instanceof PollableChannelManagement) {
                registry.more().counter(registry.createId("spring.integration.receives", tagsWithChannel,
                    "The number of messages received"),
                    (PollableChannelManagement) channelMetrics, PollableChannelManagement::getReceiveCount);
            }
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        // TODO better would be to use a BeanPostProcessor
        configurer.afterSingletonsInstantiated();
        registries.forEach(registry -> {
            addChannelMetrics(registry);
            addHandlerMetrics(registry);
            addSourceMetrics(registry);
        });
    }
}
