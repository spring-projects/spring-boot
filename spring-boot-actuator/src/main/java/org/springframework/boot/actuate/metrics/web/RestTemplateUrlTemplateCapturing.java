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
package org.springframework.boot.actuate.metrics.web;

import java.net.URI;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * Captures the still-templated URI because currently the ClientHttpRequestInterceptor
 * currently only gives us the means to retrieve the substituted URI.
 *
 * @since 2.0.0
 * @author Jon Schneider
 */
@Aspect
public class RestTemplateUrlTemplateCapturing {
    @Around("execution(* org.springframework.web.client.RestOperations+.*(String, ..))")
    Object captureUrlTemplate(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            String urlTemplate = (String) joinPoint.getArgs()[0];
            RestTemplateUrlTemplateHolder.setRestTemplateUrlTemplate(urlTemplate);
            return joinPoint.proceed();
        } finally {
            RestTemplateUrlTemplateHolder.clear();
        }
    }

    @Around("execution(* org.springframework.web.client.RestOperations+.*(java.net.URI, ..))")
    Object captureUrlTemplateFromURI(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            URI urlTemplate = (URI) joinPoint.getArgs()[0];
            RestTemplateUrlTemplateHolder.setRestTemplateUrlTemplate(urlTemplate.toString());
            return joinPoint.proceed();
        } finally {
            RestTemplateUrlTemplateHolder.clear();
        }
    }
}
