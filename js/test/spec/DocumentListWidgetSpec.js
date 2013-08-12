describe("ProjectDocumentationWidget", function () {

  describe("rendering", function () {
    beforeEach(function () {
      var project = new Spring.Project({
        name: "Spring Data JPA",
        releases: [
          {version: "1.4.0.RC1", referenceUrl: "http://localhost/1.4.0/ref", apiUrl: "http://localhost/1.4.0/api", status: "pre"},
          {version: "1.3.4", referenceUrl: "http://example.com/1.3.4/ref", apiUrl: "http://example.com/1.3.4/api", status: "current"},
          {version: "1.2.1", referenceUrl: "http://spring.io/1.2.1/ref", apiUrl: "http://spring.io/1.2.1/api", status: "supported"}
        ]
      });

      var projectDocumentationWidget = new Spring.ProjectDocumentationWidgetView(
        {el: "#jasmine_content", model: project}
      );
      projectDocumentationWidget.render();
    });

    it("shows the project name in the title", function () {
      expect($('#jasmine_content')).toContainText("Spring Data JPA");
    });

    it("lists out each release's version", function () {
      expect($('#jasmine_content')).toContainText("1.4.0.RC1");
      expect($('#jasmine_content')).toContainText("1.3.4");
      expect($('#jasmine_content')).toContainText("1.2.1");
    });

    it("lists out each release's reference doc link", function () {
      expect($('#jasmine_content a[href="http://localhost/1.4.0/ref"]')).toExist();
      expect($('#jasmine_content a[href="http://example.com/1.3.4/ref"]')).toExist();
      expect($('#jasmine_content a[href="http://spring.io/1.2.1/ref"]')).toExist();
    });

    it("lists out each release's api doc link", function () {
      expect($('#jasmine_content a[href="http://localhost/1.4.0/api"]')).toExist();
      expect($('#jasmine_content a[href="http://example.com/1.3.4/api"]')).toExist();
      expect($('#jasmine_content a[href="http://spring.io/1.2.1/api"]')).toExist();
    });

    it("lists out each release's status", function () {
      expect($('#jasmine_content li:first .icon-projects-pre')).toExist();
      expect($('#jasmine_content li:nth-child(2) .icon-projects-current')).toExist();
    });
  });

});
