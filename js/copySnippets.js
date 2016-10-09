window.Spring = window.Spring || {};

$(document).ready(function() {
    Spring.configureCopyButtons();
});

Spring.configureCopyButtons = function() {
    
    $("pre.highlight").each(function(index) {
            Spring.buildCopyButton($(this), index);
        }
    );
    var errorMessage = function() {
        if (/Mac/i.test(navigator.userAgent)) {
            return 'Press Cmd-C to Copy';
        }
        else {
            return 'Press Ctrl-C to Copy';
        }
    };
    var snippets = new Clipboard('.copy-button');
    snippets.on('success', function(e) {
        e.clearSelection();
        Spring.showTooltip(e.trigger, "Copied!");
    });
    snippets.on('error', function(e) {
        Spring.showTooltip(e.trigger, errorMessage());
    });
}

Spring.showTooltip = function(elem, message) {
    $(elem).tooltip({placement:'right', title:message});
    $(elem).tooltip('show');
    setTimeout(function(){$(elem).tooltip('destroy');},1000);
}

Spring.buildCopyButton = function (preEl, id) {
    var codeBlockId = "code-block-"+ id;
    var copyButtonId = "copy-button-" + id;
    preEl.attr('id', codeBlockId);
    var button = $('<button class="copy-button snippet" id="' + copyButtonId + '" data-clipboard-target="#' + codeBlockId + '"></button>');
    preEl.before(button);
}
