{% include documentation.html %}

<div class="right-pane-widget--container no-top-border">
  <div class="project-sub-link--wrapper">
{% capture projects %}
{% if site.test %}
{% include test/projects.md %}
{% else %}
{% include projects.md %}
{% endif %}
{% endcapture %}
{{ projects | markdownify }}
 </div>
</div>
<div class="right-pane-widget--container no-top-border project-additional-resource--wrapper">
{% capture additional %}
{% if site.test %}
{% include test/additional.md %}
{% else %}
{% include additional.md %}
{% endif %}
{% endcapture %}
{{ additional | markdownify }}
</div>
