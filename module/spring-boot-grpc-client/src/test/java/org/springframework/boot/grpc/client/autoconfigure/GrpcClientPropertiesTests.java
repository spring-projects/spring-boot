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

package org.springframework.boot.grpc.client.autoconfigure;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.Channel;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GrpcClientProperties}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class GrpcClientPropertiesTests {

	@Test
	@WithResource(name = "client.yaml", content = """
			channel:
			  a:
			   target: static://my-server:8888
			  b:
			   user-agent: me""")
	void defaultValues() throws Exception {
		GrpcClientProperties properties = bind();
		Channel channelA = properties.getChannel().get("a");
		assertThat(channelA).isNotNull();
		assertThat(channelA.getUserAgent()).isNull();
		assertThat(channelA.isBypassCertificateValidation()).isFalse();
		assertThat(channelA.getInbound().getMessage().getMaxSize()).isEqualTo(DataSize.ofBytes(4194304));
		assertThat(channelA.getInbound().getMetadata().getMaxSize()).isEqualTo(DataSize.ofBytes(8192));
		assertThat(channelA.getDefault().getDeadline()).isNull();
		assertThat(channelA.getIdle().getTimeout()).isEqualTo(Duration.ofSeconds(20));
		assertThat(channelA.getKeepalive().getTime()).isEqualTo(Duration.ofMinutes(5));
		assertThat(channelA.getKeepalive().getTimeout()).isEqualTo(Duration.ofSeconds(20));
		assertThat(channelA.getKeepalive().isWithoutCalls()).isFalse();
		assertThat(channelA.getSsl().getEnabled()).isNull();
		assertThat(channelA.getSsl().getBundle()).isNull();
		Channel channelB = properties.getChannel().get("b");
		assertThat(channelB).isNotNull();
		assertThat(channelB.getTarget()).isEqualTo("static://localhost:9090");
	}

	@Test
	@WithResource(name = "client.yaml", content = """
			channel:
			  test:
			    target: static://my-server:8888
			    user-agent: me
			    bypass-certificate-validation: true
			    inbound:
			      message:
			        max-size: 200MB
			      metadata:
			        max-size: 1GB
			    default:
			      deadline: 1s
			      load-balancing-policy: pick_first
			    idle:
			      timeout: 1m
			    keepalive:
			      time: 200s
			      timeout: 60000ms
			      without-calls: true
			    ssl:
			      enabled: true
			      bundle: my-bundle
			    health:
			      enabled: true
			      service-name: my-service""")
	void specificProperties() throws Exception {
		GrpcClientProperties properties = bind();
		Channel channel = properties.getChannel().get("test");
		assertThat(channel).isNotNull();
		assertThat(channel.getTarget()).isEqualTo("static://my-server:8888");
		assertThat(channel.getUserAgent()).isEqualTo("me");
		assertThat(channel.isBypassCertificateValidation()).isTrue();
		assertThat(channel.getInbound().getMessage().getMaxSize()).isEqualTo(DataSize.ofMegabytes(200));
		assertThat(channel.getInbound().getMetadata().getMaxSize()).isEqualTo(DataSize.ofGigabytes(1));
		assertThat(channel.getDefault().getDeadline()).isEqualTo(Duration.ofSeconds(1));
		assertThat(channel.getIdle().getTimeout()).isEqualTo(Duration.ofMinutes(1));
		assertThat(channel.getKeepalive().getTime()).isEqualTo(Duration.ofSeconds(200));
		assertThat(channel.getKeepalive().getTimeout()).isEqualTo(Duration.ofMillis(60000));
		assertThat(channel.getKeepalive().isWithoutCalls()).isTrue();
		assertThat(channel.getSsl().getEnabled()).isTrue();
		assertThat(channel.getSsl().getBundle()).isEqualTo("my-bundle");
	}

	@Test
	@WithResource(name = "client.yaml", content = """
			channel:
			  test:
			    idle:
			      timeout: 1
			    keepalive:
			      time: 60
			      timeout: 5""")
	void withoutKeepAliveUnitsSpecified() throws Exception {
		GrpcClientProperties properties = bind();
		Channel channel = properties.getChannel().get("test");
		assertThat(channel).isNotNull();
		assertThat(channel.getIdle().getTimeout()).isEqualTo(Duration.ofSeconds(1));
		assertThat(channel.getKeepalive().getTime()).isEqualTo(Duration.ofSeconds(60));
		assertThat(channel.getKeepalive().getTimeout()).isEqualTo(Duration.ofSeconds(5));
	}

	@Test
	@WithResource(name = "client.yaml", content = """
			channel:
			  test:
			    inbound:
			      message:
			        max-size: 1000
			      metadata:
			        max-size: 256""")
	void withoutInboundSizeUnitsSpecified() throws Exception {
		GrpcClientProperties properties = bind();
		Channel channel = properties.getChannel().get("test");
		assertThat(channel).isNotNull();
		assertThat(channel.getInbound().getMessage().getMaxSize()).isEqualTo(DataSize.ofBytes(1000));
		assertThat(channel.getInbound().getMetadata().getMaxSize()).isEqualTo(DataSize.ofBytes(256));
	}

	private GrpcClientProperties bind() throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		new YamlPropertySourceLoader().load("client.yaml", new ClassPathResource("client.yaml"))
			.forEach(environment.getPropertySources()::addLast);
		return Binder.get(environment)
			.bind("", Bindable.of(GrpcClientProperties.class))
			.orElseGet(GrpcClientProperties::new);
	}

}
