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
