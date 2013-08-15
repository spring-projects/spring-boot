{% include documentation.html %}

{% capture badges %}
{% if site.sample %}
{% include samples/badges.md %}
{% else %}
{% include badges.md %}
{% endif %}
{% endcapture %}

{% capture additional %}
{% if site.sample %}
{% include samples/additional.md %}
{% else %}
{% include additional.md %}
{% endif %}
{% endcapture %}

{%unless badges contains 'not found in _includes directory' %}
<div class="right-pane-widget--container no-top-border">
  <div class="project-sub-link--wrapper">
{{ badges | markdownify }}
 </div>
</div>
{%endunless%}

{%unless additional contains 'not found in _includes directory' %}
<div class="right-pane-widget--container no-top-border project-additional-resource--wrapper">
{{ additional | markdownify }}
</div>
{%endunless%}
