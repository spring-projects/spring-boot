/*
 * Copyright 2012-2015 the original author or authors.
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

package sample.metrics.atsd;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.lang.reflect.Method;

/**
 * Aspect that profiles methods marked with {@link Measured} annotation
 */

@Component
@Aspect
public class MeasuredAspect {
	private CounterService counterService;
	private GaugeService gaugeService;

	@Autowired
	public void setCounterService(CounterService counterService) {
		this.counterService = counterService;
	}

	@Autowired
	public void setGaugeService(GaugeService gaugeService) {
		this.gaugeService = gaugeService;
	}

	@Around("@annotation(sample.metrics.atsd.Measured) or @within(sample.metrics.atsd.Measured)")
	public Object doProfiling(ProceedingJoinPoint pjp) throws Throwable {
		MethodInvocationProceedingJoinPoint mjp = (MethodInvocationProceedingJoinPoint) pjp;
		Class<?> clazz = mjp.getSourceLocation().getWithinType();
		Method method = ((MethodSignature) mjp.getSignature()).getMethod();
		String name = composeMetricName(method, clazz);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		try {
			return pjp.proceed();
		} catch (Throwable throwable) {
			name += ".failed";
			throw throwable;
		} finally {
			stopWatch.stop();
			this.gaugeService.submit(name, stopWatch.getTotalTimeMillis());
			this.counterService.increment(name);
		}
	}

	private String composeMetricName(Method method, Class<?> clazz) {
		return "method." + clazz.getName() + '.' + method.getName();
	}
}
