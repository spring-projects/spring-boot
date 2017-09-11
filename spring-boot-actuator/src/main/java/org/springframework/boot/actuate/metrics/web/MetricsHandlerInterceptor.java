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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Intercepts incoming HTTP requests and records metrics about execution time and results.
 *
 * @since 2.0.0
 * @author Jon Schneider
 */
public class MetricsHandlerInterceptor extends HandlerInterceptorAdapter {
    private final ControllerMetrics controllerMetrics;

    public MetricsHandlerInterceptor(ControllerMetrics controllerMetrics) {
        this.controllerMetrics = controllerMetrics;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
							 Object handler) throws Exception {
        controllerMetrics.preHandle(request, handler);
        return super.preHandle(request, response, handler);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
								Object handler, Exception ex) throws Exception {
        controllerMetrics.record(request, response, ex);
        super.afterCompletion(request, response, handler, ex);
    }
}
