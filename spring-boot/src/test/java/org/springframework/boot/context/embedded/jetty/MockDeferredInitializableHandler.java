package org.springframework.boot.context.embedded.jetty;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
        // To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeLifeCycleListener(Listener listener) {
        // To change body of implemented methods use File | Settings | File Templates.
    }

}
