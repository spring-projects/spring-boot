/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.servlet.context.testcomponents.servlet;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

@WebServlet("/test-multipart")
@MultipartConfig(location = "test", maxFileSize = 1024, maxRequestSize = 2048, fileSizeThreshold = 512)
public class TestMultipartServlet extends HttpServlet {

}
