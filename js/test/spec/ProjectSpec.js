describe("Project", function () {

  describe("rendering", function () {
    var project;

    beforeEach(function () {
      project = new Spring.Project({
        id: "spring-data-jpa",
        name: "Spring Data JPA",
        projectReleases: [
          {fullName: "1.4.0.RC1", refDocUrl: "http://localhost/1.4.0/ref", apiDocUrl: "http://localhost/1.4.0/api", preRelease: true, current: false, supported: false},
          {fullName: "1.3.4", refDocUrl: "http://example.com/1.3.4/ref", apiDocUrl: "http://example.com/1.3.4/api", preRelease: false, current: true, supported: false},
          {fullName: "1.2.1", refDocUrl: "http://spring.io/1.2.1/ref", apiDocUrl: "http://spring.io/1.2.1/api", preRelease: false, current: false, supported: true}
        ]
      });
    });

    describe("releases", function() {
      var releases;
      beforeEach(function() {
        releases = project.releases;
      });

      it("has a release for each project release", function() {
        expect(releases.length).toEqual(3);
      });

      it("has a version", function() {
        expect(releases[0].fullName).toEqual("1.4.0.RC1");
      });

      it("has a documentation", function() {
        expect(releases[0].refDocUrl).toEqual("http://localhost/1.4.0/ref");
        expect(releases[0].apiDocUrl).toEqual("http://localhost/1.4.0/api");
      });

      it("has a statusIconClass", function() {
        expect(releases[0].statusIconClass()).toEqual("icon-projects-pre");
        expect(releases[1].statusIconClass()).toEqual("icon-projects-current");
        expect(releases[2].statusIconClass()).toEqual("icon-projects-supported");
      });
    });
  });
});
