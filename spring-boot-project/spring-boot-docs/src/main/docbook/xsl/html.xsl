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
		xmlns:xslthl="http://xslthl.sf.net"
		xmlns:d="http://docbook.org/ns/docbook"
		exclude-result-prefixes="xslthl"
		version='1.0'>

	<xsl:import href="urn:docbkx:stylesheet/highlight.xsl"/>
	<xsl:import href="common.xsl"/>

	<!-- Only use scaling in FO -->
	<xsl:param name="ignore.image.scaling">1</xsl:param>

	<!-- Use code syntax highlighting -->
	<xsl:param name="highlight.source">1</xsl:param>

	<!-- Activate Graphics -->
	<xsl:param name="callout.graphics" select="1" />
	<xsl:param name="callout.defaultcolumn">120</xsl:param>
	<xsl:param name="callout.graphics.path">images/callouts/</xsl:param>
	<xsl:param name="callout.graphics.extension">.png</xsl:param>

	<xsl:param name="table.borders.with.css" select="1"/>
	<xsl:param name="html.stylesheet.type">text/css</xsl:param>

	<xsl:param name="admonition.title.properties">text-align: left</xsl:param>

	<!-- Leave image paths as relative when navigating XInclude -->
	<xsl:param name="keep.relative.image.uris" select="1"/>

	<!-- Label Chapters and Sections (numbering) -->
	<xsl:param name="chapter.autolabel" select="1"/>
	<xsl:param name="section.autolabel" select="1"/>
	<xsl:param name="section.autolabel.max.depth" select="2"/>
	<xsl:param name="section.label.includes.component.label" select="1"/>
	<xsl:param name="table.footnote.number.format" select="'1'"/>

	<!-- Remove "Chapter" from the Chapter titles... -->
	<xsl:param name="local.l10n.xml" select="document('')"/>
	<l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0">
		<l:l10n language="en">
			<l:context name="title-numbered">
				<l:template name="chapter" text="%n.&#160;%t"/>
				<l:template name="section" text="%n&#160;%t"/>
			</l:context>
		</l:l10n>
	</l:i18n>

	<!-- Syntax Highlighting -->
	<xsl:template match='xslthl:keyword' mode="xslthl">
		<span class="hl-keyword"><xsl:apply-templates mode="xslthl"/></span>
	</xsl:template>

	<xsl:template match='xslthl:comment' mode="xslthl">
		<span class="hl-comment"><xsl:apply-templates mode="xslthl"/></span>
	</xsl:template>

	<xsl:template match='xslthl:oneline-comment' mode="xslthl">
		<span class="hl-comment"><xsl:apply-templates mode="xslthl"/></span>
	</xsl:template>

	<xsl:template match='xslthl:multiline-comment' mode="xslthl">
		<span class="hl-multiline-comment"><xsl:apply-templates mode="xslthl"/></span>
	</xsl:template>

	<xsl:template match='xslthl:tag' mode="xslthl">
		<span class="hl-tag"><xsl:apply-templates mode="xslthl"/></span>
	</xsl:template>

	<xsl:template match='xslthl:attribute' mode="xslthl">
		<span class="hl-attribute"><xsl:apply-templates mode="xslthl"/></span>
	</xsl:template>

	<xsl:template match='xslthl:value' mode="xslthl">
		<span class="hl-value"><xsl:apply-templates mode="xslthl"/></span>
	</xsl:template>

	<xsl:template match='xslthl:string' mode="xslthl">
		<span class="hl-string"><xsl:apply-templates mode="xslthl"/></span>
	</xsl:template>

	<!-- Custom Title Page -->
	<xsl:template match="d:author" mode="titlepage.mode">
		<xsl:if test="name(preceding-sibling::*[1]) = 'author'">
			<xsl:text>, </xsl:text>
		</xsl:if>
		<span class="{name(.)}">
			<xsl:call-template name="person.name"/>
			<xsl:apply-templates mode="titlepage.mode" select="./contrib"/>
		</span>
	</xsl:template>
	<xsl:template match="d:authorgroup" mode="titlepage.mode">
		<div class="{name(.)}">
			<h2>Authors</h2>
			<xsl:apply-templates mode="titlepage.mode"/>
		</div>
	</xsl:template>

	<!-- Title Links -->
	<xsl:template name="anchor">
		<xsl:param name="node" select="."/>
		<xsl:param name="conditional" select="1"/>
		<xsl:variable name="id">
			<xsl:call-template name="object.id">
				<xsl:with-param name="object" select="$node"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:if test="$conditional = 0 or $node/@id or $node/@xml:id">
			<xsl:element name="a">
				<xsl:attribute name="name">
					<xsl:value-of select="$id"/>
				</xsl:attribute>
				<xsl:attribute name="href">
					<xsl:text>#</xsl:text>
					<xsl:value-of select="$id"/>
				</xsl:attribute>
			</xsl:element>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>
