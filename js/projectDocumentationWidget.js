window.Spring = window.Spring || {};

/* ERB style templates conflict with Jekyll HTML escaping */
_.templateSettings = {
  evaluate    : /\{@([\s\S]+?)@\}/g,
  interpolate : /\{@=([\s\S]+?)@\}/g,
  escape      : /\{@-([\s\S]+?)@\}/g
};

Spring.ProjectDocumentationWidget = function () {
  var quickStartEl = $('[data-download-widget-controls]');
  var mavenWidgetEl = $('.js-download-maven-widget');
  var documentationEl = $('.js-documentation-widget');

  var projectUrl = apiBaseUrl + "/project_metadata/" + projectId;
  var promise = Spring.loadProject(projectUrl);

  promise.then(function (project) {
    Spring.buildDocumentationWidget(documentationEl, project);
    Spring.buildQuickStartWidget(quickStartEl, mavenWidgetEl, project);
  });
};

Spring.buildDocumentationWidget = function (documentationEl, project) {
  new Spring.DocumentationWidgetView({
    el: documentationEl,
    model: project,
    template: $("#project-documentation-widget-template").text()
  }).render();

}
Spring.buildQuickStartWidget = function (quickStartEl, mavenWidgetEl, project) {
  new Spring.QuickStartSelectorView({
    el: quickStartEl,
    model: project,
    template: $("#project-download-widget-controls-template").text(),
    snippetWidgetEl: mavenWidgetEl
  }).render();
}

Spring.loadProject = function (url) {
  return $.ajax(url, {
    dataType: 'jsonp',
    processData: false
  }).then(function (value) {
      return new Spring.Project(value);
    });
}

Spring.Release = function (data) {
  _.extend(this, data);
}

Spring.Release.prototype = {
  statusIconClass: function () {
    if (this.preRelease) {
      return "spring-icon-pre-release";
    } else if (this.generalAvailability) {
      return "spring-icon-ga-release";
    } else {
      return "spring-icon-snapshot-release";
    }
  }
}

Spring.Project = function (data) {
  _.extend(this, data);
  var self = this;
  this.releases = _.map(this.projectReleases, function (r) {
    return new Spring.Release(r);
  });

  return this;
};

Spring.DocumentationWidgetView = Backbone.View.extend({
  initialize: function () {
    this.template = _.template(this.options.template);
    _.bindAll(this, "render");
  },

  render: function () {
    this.$el.html(
      this.template(this.model)
    );
    return this;
  }
});

Spring.SnippetView = Backbone.View.extend({
  initialize: function () {
    var snippetType = this.options.snippetType;
    var downloadTemplate = $(document.createElement('div')).html($("#project-download-" + snippetType + "-widget-template").text());
    var repositoryTemplate = $(document.createElement('div')).html($("#project-repository-" + snippetType + "-widget-template").text());
    this.combinedTemplate = _.template(
    	"<div class=\"highlight\"><pre><code>" +
        downloadTemplate.find("code:first").html() +
        "{@ if (typeof(repository) != \"undefined\") { @}" +
        repositoryTemplate.find("code:first").html() +
        "{@ } @}" +
        "</code></pre></div>"
    );
    _.bindAll(this, "render");
  },

  render: function () {

    var html = $(this.combinedTemplate(this.model));
    this.$el.html(html);
    Spring.buildCopyButton(html.find(":first"), "snippet");
    return this;
  },

  remove: function() {
    this.undelegateEvents();
    this.$el.empty();
    this.unbind();
  }
});

Spring.QuickStartSelectorView = Backbone.View.extend({
  events: {
    "change .selector": "renderActiveWidget",
    "click .js-item": "changeDownloadSource"
  },

  initialize: function () {
    this.template = _.template(this.options.template);
    this.snippetWidgetEl = this.options.snippetWidgetEl;
    _.bindAll(this, "render", "renderActiveWidget", "changeDownloadSource", "_moveItemSlider", "selectCurrent");
  },

  render: function () {
    this.$el.html(
      this.template(this.model)
    );
    this.renderActiveWidget();
    this.selectCurrent();
    this.$('.selectpicker').selectpicker();
    return this;
  },

  selectCurrent: function() {
      var selectedIndex = $('.selectpicker [data-current="true"]').val();
      if(selectedIndex == undefined) {
        selectedIndex = 0;
      }
      this.$('.selectpicker').val(selectedIndex).change();
  },

  renderActiveWidget: function() {
    if(this.activeWidget != null) this.activeWidget.remove();

    this.activeWidget = new Spring.SnippetView({
      el: this.snippetWidgetEl,
      model: this.model.releases[this.$('.selector :selected').val()],
      snippetType: this.$('.js-active').data('snippet-type')
    });
    this.activeWidget.render();

  },

  changeDownloadSource: function (event) {
    var target = $(event.target);

    target.addClass("js-active");
    target.siblings().removeClass("js-active");

    this._moveItemSlider();
    this.renderActiveWidget();
  },

  _moveItemSlider: function () {
    var activeItem = $(".js-item-slider--wrapper .js-item.js-active");
    if (activeItem.length == 0) {
      return;
    } else {
      var activeItemPosition = activeItem.position();
      var activeItemOffset = activeItemPosition.left;
      var activeItemWidth = activeItem.outerWidth();

      var slider = $(".js-item--slider");
      var sliderPosition = slider.position();
      var sliderOffset = sliderPosition.left;
      var sliderTarget = activeItemOffset - sliderOffset;

      slider.width(activeItemWidth);
      slider.css("margin-left", sliderTarget);
    }
  }

});
