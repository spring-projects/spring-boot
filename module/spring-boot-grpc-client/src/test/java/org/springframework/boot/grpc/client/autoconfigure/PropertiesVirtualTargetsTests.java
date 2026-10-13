/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.client.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.Channel;
import org.springframework.grpc.client.VirtualTargets;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesVirtualTargets}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class PropertiesVirtualTargetsTests {

	@Test
	void getTargetWhenHasMatchingChannel() {
		GrpcClientProperties properties = createProperties("test", "my-server:8888");
		VirtualTargets targets = new PropertiesVirtualTargets(new MockEnvironment(), properties);
		assertThat(targets.getTarget("test")).isEqualTo("my-server:8888");
	}

	@Test
	void getTargetWhenDefaultAndDefaultChannelDefined() {
		GrpcClientProperties properties = createProperties("default", "my-server:8888");
		VirtualTargets targets = new PropertiesVirtualTargets(new MockEnvironment(), properties);
		assertThat(targets.getTarget("default")).isEqualTo("my-server:8888");
	}

	@Test
	void getTargetWhenDefaultAndNoDefaultChannelDefined() {
		GrpcClientProperties properties = new GrpcClientProperties();
		VirtualTargets targets = new PropertiesVirtualTargets(new MockEnvironment(), properties);
		assertThat(targets.getTarget("default")).isEqualTo("localhost:9090");
	}

	@Test
	void getTargetWhenChannelHasStaticTargetReturnsStrippedTarget() {
		GrpcClientProperties properties = createProperties("test", "static://my-server:8888");
		VirtualTargets targets = new PropertiesVirtualTargets(new MockEnvironment(), properties);
		assertThat(targets.getTarget("test")).isEqualTo("my-server:8888");
	}

	@Test
	void getTargetWhenChannelHasTcpTargetReturnsStrippedTarget() {
		GrpcClientProperties properties = createProperties("test", "tcp://my-server:8888");
		VirtualTargets targets = new PropertiesVirtualTargets(new MockEnvironment(), properties);
		assertThat(targets.getTarget("test")).isEqualTo("my-server:8888");
	}

	@Test
	void getTargetWhenChannelHasOtherUrlTarget() {
		GrpcClientProperties properties = createProperties("test", "foo://my-server:8888");
		VirtualTargets targets = new PropertiesVirtualTargets(new MockEnvironment(), properties);
		assertThat(targets.getTarget("test")).isEqualTo("foo://my-server:8888");
	}

	@Test
	void getTargetWhenStaticReturnsStripped() {
		GrpcClientProperties properties = new GrpcClientProperties();
		VirtualTargets targets = new PropertiesVirtualTargets(new MockEnvironment(), properties);
		assertThat(targets.getTarget("static://my-server:8888")).isEqualTo("my-server:8888");
	}

	@Test
	void getTargetWhenTcpReturnsStripped() {
		GrpcClientProperties properties = new GrpcClientProperties();
		VirtualTargets targets = new PropertiesVirtualTargets(new MockEnvironment(), properties);
		assertThat(targets.getTarget("tcp://my-server:8888")).isEqualTo("my-server:8888");
	}

	@Test
	void getTargetWhenUnixUrlDoesNotPrependStatic() {
		GrpcClientProperties properties = new GrpcClientProperties();
		VirtualTargets targets = new PropertiesVirtualTargets(new MockEnvironment(), properties);
		assertThat(targets.getTarget("foo://bar")).isEqualTo("foo://bar");
	}

	@Test
	void getTargetWhenUrlReturnsAsIs() {
		GrpcClientProperties properties = new GrpcClientProperties();
		VirtualTargets targets = new PropertiesVirtualTargets(new MockEnvironment(), properties);
		assertThat(targets.getTarget("unix:foo")).isEqualTo("unix:foo");
	}

	@Test
	void getTargetUrlWhenHasColonWithoutSlashReturnsAsIs() {
		GrpcClientProperties properties = new GrpcClientProperties();
		VirtualTargets targets = new PropertiesVirtualTargets(new MockEnvironment(), properties);
		assertThat(targets.getTarget("localhost:123/bar")).isEqualTo("localhost:123/bar");
	}

	@Test
	void getTargetWhenNotChannelNameResolvesPlaceholders() {
		GrpcClientProperties properties = new GrpcClientProperties();
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("channelName", "foo");
		VirtualTargets targets = new PropertiesVirtualTargets(environment, properties);
		assertThat(targets.getTarget("my-server-${channelName}:8888")).isEqualTo("my-server-foo:8888");
	}

	private GrpcClientProperties createProperties(String name, String target) {
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channel = new Channel();
		channel.setTarget(target);
		properties.getChannel().put(name, channel);
		return properties;
	}

}
