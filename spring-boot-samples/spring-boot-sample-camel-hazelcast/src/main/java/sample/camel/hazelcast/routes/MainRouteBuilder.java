package sample.camel.hazelcast.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MainRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        fromF("hazelcast:seda:hazelcastTest?concurrentConsumers=%d", 10)
                .routeId("HelloWorldRoute")
                .log("Hello World!!!")
                .end();
    }
}
