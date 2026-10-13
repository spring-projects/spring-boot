/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rsocket.autoconfigure;

import org.junit.jupiter.api.Test;
import reactor.netty.http.server.WebsocketServerSpec;

import org.springframework.boot.rsocket.autoconfigure.RSocketProperties.Server.Spec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RSocketProperties}.
 *
 * @author Stephane Nicoll
 */
class RSocketPropertiesTests {

	@Test
	void defaultServerSpecValuesAreConsistent() {
		WebsocketServerSpec spec = WebsocketServerSpec.builder().build();
		Spec properties = new RSocketProperties().getServer().getSpec();
		assertThat(properties.getProtocols()).isEqualTo(spec.protocols());
		assertThat(properties.getMaxFramePayloadLength().toBytes()).isEqualTo(spec.maxFramePayloadLength());
		assertThat(properties.isHandlePing()).isEqualTo(spec.handlePing());
		assertThat(properties.isCompress()).isEqualTo(spec.compress());
	}

}
