{% include documentation.md %}

{% capture badges %}
{% if site.sample %}
{% include samples/badges.md %}
{% else %}
{% include badges.md %}
{% endif %}
{% endcapture %}

{%unless badges contains 'not found in _includes directory' %}
<div class="right-pane-widget--container no-top-border">
<div class="project-sub-link--wrapper">
{{ badges | markdownify }}
</div>
</div>
{%endunless%}

<div class="right-pane-widget--container no-top-border project-sidebar-resource--wrapper">
{{ include.related_resources | markdownify }}
</div>