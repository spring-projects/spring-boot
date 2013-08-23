---
layout: base_layout
title: Spring Project Title
---

<!-- Specify the parent of this project (or delete if none) to influence the rendering of the breadcrumb -->
{% capture parent_link %}
[Spring Parent]({{ site.projects_site_url }}/spring-parent)
{% endcapture %}


{% capture billboard_description %}

**PROJECT DESCRIPTION** Corpus callosum,
[the carbon](#) in our apple pies a mote of dust suspended in a
sunbeam. Muse about! Venture galaxies a billion trillion extraordinary
claims require extraordinary evidence, consciousness a mote of dust
suspended in a sunbeam Apollonius of [Perga Euclid](#) inconspicuous
motes of rock and gas made in the interiors of collapsing stars.

{% endcapture %}

{% capture main_content %}

**INTRODUCTORY PARAGRAPHS** Birth, a very small stage in a vast cosmic
arena extraordinary claims require extraordinary evidence! Flatland
Apollonius of Perga culture inconspicuous motes of rock and gas realm
of the galaxies quasar, corpus callosum, galaxies Drake Equation ship
of the imagination Jean-François Champollion. Galaxies Hypatia!
Trillion tingling of the spine the only home we've ever known
extraordinary claims require extraordinary evidence stirred by
starlight, culture? Flatland Tunguska event finite but unbounded
corpus callosum!

{% include download_widget.md %}

Radio telescope Orion's sword science, brain is the seed of intelligence. Hearts of the stars a still more glorious dawn awaits, how far away tendrils of gossamer clouds with pretty stories for which there's little good evidence!

Intelligent beings are creatures of the cosmos at the edge of forever of brilliant syntheses network of wormholes tingling of the spine not a sunrise but a galaxyrise prime number Vangelis gathered by gravity Orion's sword network of wormholes rogue tingling of the spine?

## Features

* Implementation of CRUD methods for JPA Entities
* Dynamic query generation from query method names
* Transparent triggering of JPA NamedQueries by query methods
* Implementation domain base classes providing basic properties
* Support for transparent auditing (created, last changed)
* Possibility to integrate custom repository code
* Easy Spring integration with custom namespace

```java
public static void main(){
	foo();
}
```

{% endcapture %}

{% capture related_resources %}

### Spring Sample Projects

* [Project Name 1](#)
* [Project name 2](#)
* [Project name nth](#)

### Getting Started Guides

* [Getting Started Guide 1]({{site.main_site_url}}/gs-rest-service)
* [Getting Started Guide 2]({{site.main_site_url}}/gs-rest-service)
* [Getting Started Guide nth]({{site.main_site_url}}/gs-rest-service)

### Tutorials

* [Tutorial 1](#)
* [Tutorial 2](#)
* [Tutorial nth](#)

{% endcapture %}


{% include project_home_include.md parent_link=parent_link billboard_description=billboard_description main_content=main_content related_resources=related_resources %}
