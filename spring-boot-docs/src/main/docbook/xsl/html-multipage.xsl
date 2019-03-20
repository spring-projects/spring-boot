<?xml version="1.0" encoding="UTF-8"?>

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				version='1.0'>

	<xsl:import href="urn:docbkx:stylesheet"/>
	<xsl:import href="html.xsl"/>

	<xsl:param name="html.stylesheet">css/manual-multipage.css</xsl:param>

	<xsl:param name="chunk.section.depth">'5'</xsl:param>
	<xsl:param name="use.id.as.filename">'1'</xsl:param>

	<!-- Replace chunk-element-content from chunk-common to add firstpage class to body -->
	<xsl:template name="chunk-element-content">
		<xsl:param name="prev"/>
		<xsl:param name="next"/>
		<xsl:param name="nav.context"/>
		<xsl:param name="content">
			<xsl:apply-imports/>
		</xsl:param>

  		<xsl:call-template name="user.preroot"/>

		<html>
			<xsl:call-template name="html.head">
			<xsl:with-param name="prev" select="$prev"/>
			<xsl:with-param name="next" select="$next"/>
			</xsl:call-template>
			<body>
				<xsl:if test="count($prev) = 0">
					<xsl:attribute name="class">firstpage</xsl:attribute>
				</xsl:if>
				<xsl:call-template name="body.attributes"/>
				<xsl:call-template name="user.header.navigation"/>
				<xsl:call-template name="header.navigation">
					<xsl:with-param name="prev" select="$prev"/>
					<xsl:with-param name="next" select="$next"/>
					<xsl:with-param name="nav.context" select="$nav.context"/>
				</xsl:call-template>
				<xsl:call-template name="user.header.content"/>
				<xsl:copy-of select="$content"/>
				<xsl:call-template name="user.footer.content"/>
				<xsl:call-template name="footer.navigation">
					<xsl:with-param name="prev" select="$prev"/>
					<xsl:with-param name="next" select="$next"/>
					<xsl:with-param name="nav.context" select="$nav.context"/>
				</xsl:call-template>
				<xsl:call-template name="user.footer.navigation"/>
			</body>
		</html>
		<xsl:value-of select="$chunk.append"/>
	</xsl:template>
</xsl:stylesheet>
