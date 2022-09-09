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

package org.springframework.boot.actuate.autoconfigure.tracing;

import org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryConfigurations.MicrometerConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryConfigurations.SdkConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryConfigurations.TracerConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OpenTelemetry.
 *
 * It uses imports on {@link OpenTelemetryConfigurations} to guarantee the correct
 * configuration ordering.
 *
 * @author Moritz Halbritter
 * @since 3.0.0
 */
@AutoConfiguration(before = MicrometerTracingAutoConfiguration.class)
@Import({ SdkConfiguration.class, TracerConfiguration.class, MicrometerConfiguration.class })
@ConditionalOnEnabledTracing
public class OpenTelemetryAutoConfiguration {

}
