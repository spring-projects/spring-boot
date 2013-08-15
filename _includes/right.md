{% include documentation.html %}

{% capture projects %}
{% if site.test %}
{% include test/projects.md %}
{% else %}
{% include projects.md %}
{% endif %}
{% endcapture %}

{% capture additional %}
{% if site.test %}
{% include test/additional.md %}
{% else %}
{% include additional.md %}
{% endif %}
{% endcapture %}

{%unless projects contains 'not found in _includes directory' %}
<div class="right-pane-widget--container no-top-border">
  <div class="project-sub-link--wrapper">
{{ projects | markdownify }}
 </div>
</div>
{%endunless%}

{%unless additional contains 'not found in _includes directory' %}
<div class="right-pane-widget--container no-top-border project-additional-resource--wrapper">
{{ additional | markdownify }}
</div>
{%endunless%}
