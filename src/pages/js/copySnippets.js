window.Spring = window.Spring || {};

ZeroClipboard.setDefaults( { moviePath: siteBaseUrl + '/js/ZeroClipboard.swf' } );

$(document).ready(function() {
    Spring.configureCopyButtons();
});

Spring.configureCopyButtons = function() {
    if (ZeroClipboard.detectFlashSupport()) {
        $('.highlight pre').each(function(index) {
                Spring.buildCopyButton($(this), index);
            }
        );
    }
}

Spring.buildCopyButton = function (preEl, id) {
    var codeBlockId = "code-block-"+ id;
    var copyButtonId = "copy-button-" + id;
    preEl.attr('id', codeBlockId);
    var button = $('<button class="copy-button snippet" id="' + copyButtonId + '" data-clipboard-target="' + codeBlockId + '"></button>');
    preEl.before(button);
    var zero = new ZeroClipboard(button);
    $(zero.htmlBridge).tooltip({title: "copy to clipboard", placement: 'bottom'});
}