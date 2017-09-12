/**
 * Copyright 2012-2017 the original author or authors.
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
package org.springframework.boot.actuate.metrics.scheduling;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.stats.quantile.WindowSketchQuantiles;
import io.micrometer.core.instrument.util.AnnotationUtils;

/**
 * @since 2.0.0
 * @author Jon Schneider
 */
@Aspect
public class ScheduledMethodMetrics {
    private static final Log logger = LogFactory.getLog(ScheduledMethodMetrics.class);

    private final MeterRegistry registry;

    public ScheduledMethodMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @Around("execution (@org.springframework.scheduling.annotation.Scheduled  * *.*(..))")
    public Object timeScheduledOperation(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String signature = pjp.getSignature().toShortString();

        if (method.getDeclaringClass().isInterface()) {
            try {
                method = pjp.getTarget().getClass().getDeclaredMethod(pjp.getSignature().getName(),
                        method.getParameterTypes());
            } catch (final SecurityException | NoSuchMethodException e) {
                logger.warn("Unable to perform metrics timing on " + signature, e);
                return pjp.proceed();
            }
        }

        Timer shortTaskTimer = null;
        LongTaskTimer longTaskTimer = null;

        for (Timed timed : AnnotationUtils.findTimed(method).toArray(Timed[]::new)) {
            if(timed.longTask())
                longTaskTimer = registry.more().longTaskTimer(registry.createId(timed.value(), Tags.zip(timed.extraTags()),
                    "Timer of @Scheduled long task"));
            else {
                Timer.Builder timerBuilder = Timer.builder(timed.value())
                        .tags(timed.extraTags())
                        .description("Timer of @Scheduled task");

                if(timed.quantiles().length > 0) {
                    timerBuilder = timerBuilder.quantiles(WindowSketchQuantiles.quantiles(timed.quantiles()).create());
                }

                shortTaskTimer = timerBuilder.register(registry);
            }
        }

        if(shortTaskTimer != null && longTaskTimer != null) {
            final Timer finalTimer = shortTaskTimer;
            return recordThrowable(longTaskTimer, () -> recordThrowable(finalTimer, pjp::proceed));
        }
        else if(shortTaskTimer != null) {
            return recordThrowable(shortTaskTimer, pjp::proceed);
        }
        else if(longTaskTimer != null) {
            return recordThrowable(longTaskTimer, pjp::proceed);
        }

        return pjp.proceed();
    }

    private Object recordThrowable(LongTaskTimer timer, ThrowableCallable f) throws Throwable {
        long id = timer.start();
        try {
            return f.call();
        } finally {
            timer.stop(id);
        }
    }

    private Object recordThrowable(Timer timer, ThrowableCallable f) throws Throwable {
        long start = registry.config().clock().monotonicTime();
        try {
            return f.call();
        } finally {
            timer.record(registry.config().clock().monotonicTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    private interface ThrowableCallable {
        Object call() throws Throwable;
    }
}