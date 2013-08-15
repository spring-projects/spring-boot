window.Spring = window.Spring || {};

Spring.ProjectDocumentationWidget = function () {
  var quickStartEl = $('[data-quickstart-controls]');
  var mavenWidgetEl = $('.js-quickstart-maven-widget');
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
    template: $("#project-quickstart-controls-template").text(),
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
    } else if (this.current) {
      return "spring-icon-current-version";
    } else {
      return "spring-icon-supported-version";
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
    this.dependencyTemplate = _.template($("#project-quickstart-" + snippetType + "-widget-dependency-template").text());
    this.repositoryTemplate = _.template($("#project-quickstart-" + snippetType + "-widget-repository-template").text());
    _.bindAll(this, "render");
  },

  render: function () {
    var html = $("<pre></pre>");
    html.append(this.dependencyTemplate(this.model));
    if (this.model.repository != null) {
      html.append(this.repositoryTemplate(this.model.repository));
    }
    this.$el.html(html);
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
    _.bindAll(this, "render", "renderActiveWidget", "changeDownloadSource", "_moveItemSlider");
  },

  render: function () {
    this.$el.html(
      this.template(this.model)
    );
    this.renderActiveWidget();
    this.$('.selectpicker').selectpicker();
    return this;
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