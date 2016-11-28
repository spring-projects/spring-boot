package de.nerdpause.demo.multistaterequest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * A simple Spring Boot demo application showing how multiple values can be returned upon a single
 * http POST request. It allows to start a request which takes a longer period of time and runs in
 * an own thread. The request runs in two steps and in parallel the requesting web page can request
 * status updates. The updates are served using long polling.
 * 
 * @author Jan-Philipp Kappmeier
 */
@SpringBootApplication
@EnableAsync
public class MultiStateRequestDemo {

    /**
     * Starts the demo application using the Spring Boot starter.
     *
     * @param args the command line arguments
     * @throws Exception if starting the application fails
     */
    public static void main(String[] args) throws Exception {
        SpringApplication.run(MultiStateRequestDemo.class, args);
    }
}
