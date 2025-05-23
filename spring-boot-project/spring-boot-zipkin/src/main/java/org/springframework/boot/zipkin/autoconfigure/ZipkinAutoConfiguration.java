/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.zipkin.autoconfigure;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;

import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.Encoding;
import zipkin2.reporter.HttpEndpointSupplier;
import zipkin2.reporter.HttpEndpointSuppliers;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Zipkin.
 *
 * @author Moritz Halbritter
 * @author Moritz Halbritter
 * @author Stefan Bratanov
 * @author Wick Dynex
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(Encoding.class)
@EnableConfigurationProperties(ZipkinProperties.class)
public class ZipkinAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ZipkinConnectionDetails.class)
	PropertiesZipkinConnectionDetails zipkinConnectionDetails(ZipkinProperties properties) {
		return new PropertiesZipkinConnectionDetails(properties);
	}

	@Bean
	@ConditionalOnMissingBean
	Encoding encoding(ZipkinProperties properties) {
		return switch (properties.getEncoding()) {
			case JSON -> Encoding.JSON;
			case PROTO3 -> Encoding.PROTO3;
		};
	}

	@Bean
	@ConditionalOnMissingBean(BytesMessageSender.class)
	@ConditionalOnClass(HttpClient.class)
	ZipkinHttpClientSender httpClientSender(ZipkinProperties properties, Encoding encoding,
			ObjectProvider<ZipkinHttpClientBuilderCustomizer> customizers,
			ObjectProvider<ZipkinConnectionDetails> connectionDetailsProvider,
			ObjectProvider<HttpEndpointSupplier.Factory> endpointSupplierFactoryProvider) {
		ZipkinConnectionDetails connectionDetails = connectionDetailsProvider
			.getIfAvailable(() -> new PropertiesZipkinConnectionDetails(properties));
		HttpEndpointSupplier.Factory endpointSupplierFactory = endpointSupplierFactoryProvider
			.getIfAvailable(HttpEndpointSuppliers::constantFactory);
		Builder httpClientBuilder = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout());
		customizers.orderedStream().forEach((customizer) -> customizer.customize(httpClientBuilder));
		return new ZipkinHttpClientSender(encoding, endpointSupplierFactory, connectionDetails.getSpanEndpoint(),
				httpClientBuilder.build(), properties.getReadTimeout());
	}

}
