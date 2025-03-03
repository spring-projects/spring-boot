/*
 * Copyright 2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.cassandra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metadata.NodeState;
import com.datastax.oss.driver.internal.core.context.InternalDriverContext;
import com.datastax.oss.driver.internal.core.metadata.DefaultNode;
import com.datastax.oss.driver.internal.core.metadata.NodeStateEvent;
import com.datastax.oss.driver.internal.core.session.DefaultSession;
import com.datastax.oss.driver.internal.core.util.concurrent.CompletableFutures;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;

import org.springframework.context.Lifecycle;

public class CassandraSessionLifecycle implements Lifecycle {

	private final DefaultSession session;

	private final InternalDriverContext context;

	// We don't require auto startup through SmartLifecycle
	private boolean running = true;

	private CracResource resource = new CracResource();

	private Map<Node, NodeState> lastState;

	public CassandraSessionLifecycle(CqlSession session) {
		this.session = (DefaultSession) session;
		this.context = (InternalDriverContext) session.getContext();
		// The resource is registered to support automatic checkpoint on refresh
		// before DefaultLifecycleProcessor is started.
		Core.getGlobalContext().register(this.resource);
	}

	@Override
	public synchronized void start() {
		if (this.running) {
			return;
		}
		this.running = true;
		// After start we can remove the resource and let it be GCed;
		// this will be handled by Lifecycle invocations.
		this.resource = null;
		if (this.lastState == null) {
			return;
		}
		for (var e : this.lastState.entrySet()) {
			NodeStateEvent changed = NodeStateEvent.changed(NodeState.FORCED_DOWN, e.getValue(),
					(DefaultNode) e.getKey());
			this.context.getEventBus().fire(changed);
		}
		this.lastState = null;
		this.context.getControlConnection().reconnectNow();
	}

	@Override
	public synchronized void stop() {
		if (!this.running) {
			return;
		}
		this.running = false;

		try {
			// ControlConnection would try to reconnect when it receives the event that
			// node was brought down; seeing the channel to this node already closed
			// prevents that.
			this.context.getControlConnection().channel().close().get();

			this.lastState = new HashMap<>();
			ArrayList<CompletionStage<Void>> closeFutures = new ArrayList<>();
			for (var e : this.session.getPools().entrySet()) {
				Node node = e.getKey();
				NodeState currentState = node.getState();
				this.lastState.put(node, currentState);
				closeFutures.add(e.getValue().closeFuture());
				this.context.getEventBus()
					.fire(NodeStateEvent.changed(currentState, NodeState.FORCED_DOWN, (DefaultNode) node));
			}

			CompletableFutures.allDone(closeFutures).toCompletableFuture().get();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(ex);
		}
		catch (ExecutionException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	private final class CracResource implements org.crac.Resource {

		@Override
		public void beforeCheckpoint(Context<? extends Resource> context) {
			stop();
		}

		@Override
		public void afterRestore(Context<? extends Resource> context) {
			start();
		}

	}

}
