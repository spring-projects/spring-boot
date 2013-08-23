---
layout: base_layout
---

{% capture billboard_description %}
BILLBOARD DESCRIPTION; markdown OK; <=500 chars
{% endcapture %}

{% capture main_content %}
CHILD PROJECT GRID GOES HERE
{% endcapture %}

{% include project_aggregator_include.md billboard_description=billboard_description main_content=main_content %}
