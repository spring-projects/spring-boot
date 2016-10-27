/*
 * Copyright 2012-2013 the original author or authors.
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

package sample.endless.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

@Component
public class ScheduledPrinterService {

    private final ScheduledExecutorService scheduledPrinter;

    public ScheduledPrinterService() {
        this.scheduledPrinter = Executors.newSingleThreadScheduledExecutor();
    }

    @PostConstruct
    public void startPrinting() {
        // start the scheduled printer
        final AtomicLong counter = new AtomicLong();
        scheduledPrinter.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                System.out.println("Hello World No. " + counter.incrementAndGet() + "!");
            }
        }, 0L, 1L, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void close() {
        // closing the scheduled logger
        scheduledPrinter.shutdown();
    }
}
