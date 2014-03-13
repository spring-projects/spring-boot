<?xml version="1.0" encoding="UTF-8"?>

<!--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:xslthl="http://xslthl.sf.net"
		exclude-result-prefixes="xslthl"
		version='1.0'>

	<!-- Extensions -->
	<xsl:param name="use.extensions">1</xsl:param>
	<xsl:param name="tablecolumns.extension">0</xsl:param>
	<xsl:param name="callout.extensions">1</xsl:param>

	<!-- Graphics -->
	<xsl:param name="admon.graphics" select="1"/>
	<xsl:param name="admon.graphics.path">images/</xsl:param>
	<xsl:param name="admon.graphics.extension">.png</xsl:param>

	<!-- Table of Contents -->
	<xsl:param name="generate.toc">book toc,title</xsl:param>
	<xsl:param name="toc.section.depth">3</xsl:param>

	<!-- Hide revhistory -->
	<xsl:template match="revhistory" mode="titlepage.mode"/>

</xsl:stylesheet>
