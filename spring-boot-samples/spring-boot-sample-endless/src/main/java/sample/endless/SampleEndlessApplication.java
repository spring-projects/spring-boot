/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.endless;

import java.util.concurrent.CountDownLatch;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

@SpringBootApplication
public class SampleEndlessApplication {

    // This simple example shows how a spring application can start and continuously does
    // "something" until the application is stopped or interrupted.
    // In this example a scheduled, non-blocking thread is logging.

    private static final Logger LOG = LoggerFactory.getLogger(SampleEndlessApplication.class);

    /**
     * A waiting service to keep the application alive as long as no shutdown was initiated.
     */
    @Service
    public class ShutdownService {

        private final CountDownLatch countDownLatch;

        public ShutdownService() {
            this.countDownLatch = new CountDownLatch(1);
        }

        /**
         * Will release the blocking {@link #await()}.
         */
        @PreDestroy
        void countDown() {
            this.countDownLatch.countDown();
        }

        /**
         * This method is blocking until the application is shutting down.
         *
         * @throws InterruptedException if the waiting thread got interrupted
         */
        void await() throws InterruptedException {
            this.countDownLatch.await();
        }
    }

    public static void main(final String[] args) {
        final ConfigurableApplicationContext ctx = SpringApplication.run(SampleEndlessApplication.class, args);

        // keep the application from exiting the main thread, the running logic is non-blocking
        try {
            // If the shutdown (hook) is triggered, the Spring Application will close its context and all
            // "@PreDestroy" methods will be called.
            // Thus leading to the ShutdownService releasing the waiting thread and the application shuts down.
            final ShutdownService shutdownService = ctx.getBean(ShutdownService.class);
            shutdownService.await();

            LOG.info("Application shutting down.");
        } catch (final InterruptedException e) {
            // The main thread was interrupted. Close the context gracefully.
            LOG.info("Application thread interrupted. Shutting down.");
            ctx.close();
            Thread.currentThread().interrupt();
        }
    }
}
