/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.client.autoconfigure.test.scan;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractStub.StubFactory;

public final class DummyBlockingV2Grpc {

	private DummyBlockingV2Grpc() {
	}

	public static DummyBlockingV2Stub newBlockingV2Stub(io.grpc.Channel channel) {
		return AbstractBlockingStub.newStub((StubFactory<DummyBlockingV2Stub>) DummyBlockingV2Stub::new, channel);
	}

	public static class DummyBlockingV2Stub extends AbstractBlockingStub<DummyBlockingV2Stub> {

		protected DummyBlockingV2Stub(Channel channel, CallOptions callOptions) {
			super(channel, callOptions);
		}

		@Override
		protected DummyBlockingV2Stub build(Channel channel, CallOptions callOptions) {
			return new DummyBlockingV2Stub(channel, callOptions);
		}

	}

}
