/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.PartEventHttpMessageReader;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for multipart support in Spring
 * WebFlux.
 *
 * @author Chris Bono
 * @author Brian Clozel
 * @since 2.6.0
 */
@AutoConfiguration
@ConditionalOnClass({ DefaultPartHttpMessageReader.class, WebFluxConfigurer.class })
@ConditionalOnWebApplication(type = Type.REACTIVE)
@EnableConfigurationProperties(ReactiveMultipartProperties.class)
public class ReactiveMultipartAutoConfiguration {

	@Bean
	@Order(0)
	CodecCustomizer defaultPartHttpMessageReaderCustomizer(ReactiveMultipartProperties multipartProperties) {
		return (configurer) -> configurer.defaultCodecs().configureDefaultCodec((codec) -> {
			if (codec instanceof DefaultPartHttpMessageReader defaultPartHttpMessageReader) {
				PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
				map.from(multipartProperties::getMaxInMemorySize)
					.asInt(DataSize::toBytes)
					.to(defaultPartHttpMessageReader::setMaxInMemorySize);
				map.from(multipartProperties::getMaxHeadersSize)
					.asInt(DataSize::toBytes)
					.to(defaultPartHttpMessageReader::setMaxHeadersSize);
				map.from(multipartProperties::getMaxDiskUsagePerPart)
					.asInt(DataSize::toBytes)
					.to(defaultPartHttpMessageReader::setMaxDiskUsagePerPart);
				map.from(multipartProperties::getMaxParts).to(defaultPartHttpMessageReader::setMaxParts);
				map.from(multipartProperties::getFileStorageDirectory)
					.as(Paths::get)
					.to((dir) -> configureFileStorageDirectory(defaultPartHttpMessageReader, dir));
				map.from(multipartProperties::getHeadersCharset).to(defaultPartHttpMessageReader::setHeadersCharset);
			}
			else if (codec instanceof PartEventHttpMessageReader partEventHttpMessageReader) {
				PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
				map.from(multipartProperties::getMaxInMemorySize)
					.asInt(DataSize::toBytes)
					.to(partEventHttpMessageReader::setMaxInMemorySize);
				map.from(multipartProperties::getMaxHeadersSize)
					.asInt(DataSize::toBytes)
					.to(partEventHttpMessageReader::setMaxHeadersSize);
				map.from(multipartProperties::getHeadersCharset).to(partEventHttpMessageReader::setHeadersCharset);
			}
		});
	}

	private void configureFileStorageDirectory(DefaultPartHttpMessageReader defaultPartHttpMessageReader,
			Path fileStorageDirectory) {
		try {
			defaultPartHttpMessageReader.setFileStorageDirectory(fileStorageDirectory);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to configure multipart file storage directory", ex);
		}
	}

}
