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

package org.springframework.boot.actuate.metrics.graphite;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.metrics.Metric;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GraphiteMetricWriter}.
 *
 * @author Mark Sailes
 */
public class GraphiteMetricWriterTest {

    private static final String HOST = "localhost";
    private static final int PORT = 2003;
    private static final String PREFIX = "metrics.mysystem";

    private GraphiteMetricWriter graphiteMetricWriter;
    private GraphiteMetricWriter.SocketProvider mockSocketFactory;

    @Before
    public void setup() {
        this.mockSocketFactory = mock(GraphiteMetricWriter.SocketProvider.class);
        this.graphiteMetricWriter = new GraphiteMetricWriter(PREFIX, HOST, PORT, this.mockSocketFactory);
    }

    @Test
    public void testMetricIsWrittenToSocket() throws IOException {
        Socket socket = mock(Socket.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(mockSocketFactory.socket(HOST, PORT)).thenReturn(socket);
        when(socket.getOutputStream()).thenReturn(outputStream);
        Metric<Number> metric = new Metric<>("volume", 11, new Date(1488375459));

        this.graphiteMetricWriter.set(metric);

        verify(outputStream).write("metrics.mysystem.volume 11 1488375\n".getBytes());
        verify(outputStream).close();
        verify(socket).close();
    }
}