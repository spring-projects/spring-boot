/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.tomcat;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Manager;
import org.apache.catalina.startup.Tomcat;
import org.junit.Test;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.web.embedded.jetty.JettyWebServer;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class TomcatMetricsBinderTests {

    @Test
    public void reportExpectedMetrics() {
        ApplicationStartedEvent event = mock(ApplicationStartedEvent.class);
        AnnotationConfigServletWebServerApplicationContext context = mock(
                AnnotationConfigServletWebServerApplicationContext.class);
        given(event.getApplicationContext()).willReturn(context);
        TomcatWebServer webServer = mock(TomcatWebServer.class);
        given(context.getWebServer()).willReturn(webServer);
        Tomcat tomcat = mock(Tomcat.class);
        given(webServer.getTomcat()).willReturn(tomcat);
        Host host = mock(Host.class);
        given(tomcat.getHost()).willReturn(host);
        Context container = mock(Context.class);
        given(host.findChildren()).willReturn(new Container[] {container});
        Manager manager = mock(Manager.class);
        given(container.getManager()).willReturn(manager);

        MeterRegistry meterRegistry = bindToMeterRegistry(event);

        meterRegistry.get("tomcat.sessions.active.max").gauge();
    }

    @Test(expected = MeterNotFoundException.class)
    public void doNotReportMetricsForNonWebServerApplicationContext() {
        ApplicationStartedEvent event = mock(ApplicationStartedEvent.class);
        GenericApplicationContext context = mock(GenericApplicationContext.class);
        given(event.getApplicationContext()).willReturn(context);

        MeterRegistry meterRegistry = bindToMeterRegistry(event);

        meterRegistry.get("tomcat.sessions.active.max").gauge();
    }

    @Test(expected = MeterNotFoundException.class)
    public void doNotReportMetricsForNonTomcatWebServer() {
        ApplicationStartedEvent event = mock(ApplicationStartedEvent.class);
        AnnotationConfigServletWebServerApplicationContext context = mock(
                AnnotationConfigServletWebServerApplicationContext.class);
        given(event.getApplicationContext()).willReturn(context);
        JettyWebServer webServer = mock(JettyWebServer.class);
        given(context.getWebServer()).willReturn(webServer);

        MeterRegistry meterRegistry = bindToMeterRegistry(event);

        meterRegistry.get("tomcat.sessions.active.max").gauge();
    }

    @Test(expected = MeterNotFoundException.class)
    public void doNotReportMetricsForEmptyTomcatContainer() {
        ApplicationStartedEvent event = mock(ApplicationStartedEvent.class);
        AnnotationConfigServletWebServerApplicationContext context = mock(
                AnnotationConfigServletWebServerApplicationContext.class);
        given(event.getApplicationContext()).willReturn(context);
        TomcatWebServer webServer = mock(TomcatWebServer.class);
        given(context.getWebServer()).willReturn(webServer);
        Tomcat tomcat = mock(Tomcat.class);
        given(webServer.getTomcat()).willReturn(tomcat);
        Host host = mock(Host.class);
        given(tomcat.getHost()).willReturn(host);
        given(host.findChildren()).willReturn(new Container[0]);

        MeterRegistry meterRegistry = bindToMeterRegistry(event);

        meterRegistry.get("tomcat.sessions.active.max").gauge();
    }

    @Test(expected = MeterNotFoundException.class)
    public void doNotReportMetricsForNonCatalinaContext() {
        ApplicationStartedEvent event = mock(ApplicationStartedEvent.class);
        AnnotationConfigServletWebServerApplicationContext context = mock(
                AnnotationConfigServletWebServerApplicationContext.class);
        given(event.getApplicationContext()).willReturn(context);
        TomcatWebServer webServer = mock(TomcatWebServer.class);
        given(context.getWebServer()).willReturn(webServer);
        Tomcat tomcat = mock(Tomcat.class);
        given(webServer.getTomcat()).willReturn(tomcat);
        Host host = mock(Host.class);
        given(tomcat.getHost()).willReturn(host);
        Engine engine = mock(Engine.class);
        given(host.findChildren()).willReturn(new Container[] {engine});

        MeterRegistry meterRegistry = bindToMeterRegistry(event);

        meterRegistry.get("tomcat.sessions.active.max").gauge();
    }

    private MeterRegistry bindToMeterRegistry(ApplicationStartedEvent event) {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        TomcatMetricsBinder metricsBinder = new TomcatMetricsBinder(meterRegistry);
        metricsBinder.onApplicationEvent(event);
        return meterRegistry;
    }
}
