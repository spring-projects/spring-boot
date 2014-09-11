/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded.jetty;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Base NoOp implementation of DeferredInitializable and Handler for unit tests.
 *
 * @author Bradley M Handy
 */
public class MockDeferredInitializableHandler implements DeferredInitializable, Handler {

    private Server server;
    private boolean started;

    @Override
    public void performDeferredInitialization() throws Exception {
        // Do nothing here.
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Do nothing here.
    }

    @Override
    public void setServer(Server server) {
        this.server = server;
    }

    @Override
    public Server getServer() {
        return this.server;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void start() throws Exception {
        this.started = true;
    }

    @Override
    public void stop() throws Exception {
        this.started = false;
    }

    @Override
    public boolean isRunning() {
        return isStarted();
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean isStarting() {
        return false;
    }

    @Override
    public boolean isStopping() {
        return false;
    }

    @Override
    public boolean isStopped() {
        return !isStarted();
    }

    @Override
    public boolean isFailed() {
        return false;
    }

    @Override
    public void addLifeCycleListener(Listener listener) {
        // Do nothing here for now.
    }

    @Override
    public void removeLifeCycleListener(Listener listener) {
        // Do nothing here for now.
    }

}
