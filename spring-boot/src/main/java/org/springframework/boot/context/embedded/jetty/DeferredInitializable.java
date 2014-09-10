package org.springframework.boot.context.embedded.jetty;

public interface DeferredInitializable {

    void performDeferredInitialization() throws Exception;

}
