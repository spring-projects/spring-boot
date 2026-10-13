/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.client.autoconfigure.test.scan;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractStub.StubFactory;

public final class DummyBlockingGrpc {

	private DummyBlockingGrpc() {
	}

	public static DummyBlockingStub newBlockingStub(io.grpc.Channel channel) {
		return AbstractBlockingStub.newStub((StubFactory<DummyBlockingStub>) DummyBlockingStub::new, channel);
	}

	public static class DummyBlockingStub extends AbstractBlockingStub<DummyBlockingStub> {

		protected DummyBlockingStub(Channel channel, CallOptions callOptions) {
			super(channel, callOptions);
		}

		@Override
		protected DummyBlockingStub build(Channel channel, CallOptions callOptions) {
			return new DummyBlockingStub(channel, callOptions);
		}

	}

}
