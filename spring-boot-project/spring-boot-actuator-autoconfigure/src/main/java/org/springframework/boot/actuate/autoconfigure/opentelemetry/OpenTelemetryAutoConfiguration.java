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

package org.springframework.boot.actuate.autoconfigure.opentelemetry;

import io.micrometer.tracing.exporter.SpanExportingPredicate;
import io.micrometer.tracing.exporter.SpanFilter;
import io.micrometer.tracing.exporter.SpanReporter;
import io.micrometer.tracing.otel.bridge.CompositeSpanExporter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ConditionalOnClass(AutoConfiguredOpenTelemetrySdk.class)
@EnableConfigurationProperties(TracingProperties.class)
public class OpenTelemetryAutoConfiguration {

    /**
     * Default value for application name if {@code spring.application.name} is not set.
     */
    private static final String DEFAULT_APPLICATION_NAME = "application";

    private final TracingProperties tracingProperties;

    OpenTelemetryAutoConfiguration(TracingProperties tracingProperties) {
        this.tracingProperties = tracingProperties;
    }


    @Bean
    @ConditionalOnMissingBean
    public OpenTelemetry openTelemetry(
            Environment environment,
            Optional<Sampler> sampler,
            Optional<ContextPropagators> contextPropagators,
            ObjectProvider<TextMapPropagator> textMapPropagators,
            ObjectProvider<SpanProcessor> spanProcessors,
            ObjectProvider<SpanExporter> spanExporters,
            ObjectProvider<SpanExportingPredicate> spanExportingPredicates,
            ObjectProvider<SpanReporter> spanReporters,
            ObjectProvider<SpanFilter> spanFilters) {
        AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder();
        builder.addPropertiesSupplier(() -> readPropertiesStartingWith("otel", environment));

        // user can use otel config, which will be used with the lowest priority:
        // https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure

        builder.addResourceCustomizer((resource, configProperties) -> customizeResource(environment, resource));

        builder.addPropagatorCustomizer(
                (otelPropagator, configProperties) ->
                        contextPropagators.map(ContextPropagators::getTextMapPropagator)
                                .or(() -> textMapPropagator(textMapPropagators))
                                .orElse(otelPropagator));

        builder.addSamplerCustomizer(
                (otelSampler, configProperties) -> sampler
                                                           .or(this::configSampler)
                                                           .orElse(otelSampler));

        builder.addTracerProviderCustomizer((sdkTracerProviderBuilder, configProperties) -> {
            spanProcessors.orderedStream().forEach(sdkTracerProviderBuilder::addSpanProcessor);

            return sdkTracerProviderBuilder;
        });

        builder.addSpanExporterCustomizer((spanExporter, configProperties) -> {
            ArrayList<SpanExporter> exporters = spanExporters.orderedStream().collect(Collectors.toCollection(ArrayList::new));
            exporters.add(spanExporter);
            List<SpanExportingPredicate> predicates = spanExportingPredicates.orderedStream().toList();
            List<SpanReporter> reporters = spanReporters.orderedStream().toList();
            List<SpanFilter> filters = spanFilters.orderedStream().toList();

            return exporters.size() == 1 && predicates.isEmpty() && reporters.isEmpty() && filters.isEmpty() ?
                           spanExporter :
                           new CompositeSpanExporter(exporters, predicates, reporters, filters);

        });

        return builder.build().getOpenTelemetrySdk();
    }

    private Map<String, String> readPropertiesStartingWith(String prefix, Environment environment) {
        //todo not possible unless you use some workarounds - see https://stackoverflow.com/questions/23506471/access-all-environment-properties-as-a-map-or-properties-object
        return Collections.emptyMap();
    }

    private static Resource customizeResource(Environment environment, Resource resource) {
        if (resource.getAttribute(ResourceAttributes.SERVICE_NAME) == null) {
            //todo is there already a default service name - and if not, why should we set it to "application"?
            String applicationName = environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME);
            return resource.toBuilder().put(ResourceAttributes.SERVICE_NAME, applicationName).build();
        }

        return resource;
    }

    private static Optional<TextMapPropagator> textMapPropagator(
            ObjectProvider<TextMapPropagator> textMapPropagators) {
        List<TextMapPropagator> propagators = textMapPropagators.orderedStream().toList();

        return propagators.isEmpty() ? Optional.empty() : Optional.of(TextMapPropagator.composite(propagators));
    }

    private Optional<Sampler> configSampler() {
        float probability = this.tracingProperties.getSampling().getProbability();

        return probability != TracingProperties.Sampling.DEFAULT_PROBABILITY ?
                       Optional.of(Sampler.parentBased(Sampler.traceIdRatioBased(probability))) :
                       Optional.empty();
    }

}
