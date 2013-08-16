beforeEach(function() {
  $('body').append('<div id="jasmine_content"></div>');
});

afterEach(function() {
  $('body #jasmine_content').remove();
});