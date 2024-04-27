/*
 * Copyright 2024 - 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.autoconfigure.astra;

import java.nio.file.Path;

import com.datastax.oss.driver.api.core.CqlSession;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.context.annotation.Bean;


/**
 * @author Cédrick Lunven
 * @author Mick Semb Wever
 * @since 3.2.6
 */
@AutoConfiguration(before = CassandraAutoConfiguration.class)
@ConditionalOnClass({CqlSession.class})
@EnableConfigurationProperties(AstraExtraSettings.class)
public class AstraConfiguration {

    @Bean
    public CqlSessionBuilderCustomizer sessionBuilderCustomizer(AstraExtraSettings astraSettings) {
        if (null != astraSettings.getSecureConnectBundle()) {
            Path bundle = astraSettings.getSecureConnectBundle().toPath();
            return builder -> builder.withCloudSecureConnectBundle(bundle);
        }
        return (cqlSessionBuilder) -> {/* noop */};
    }

}
