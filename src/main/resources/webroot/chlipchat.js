var login = window.prompt('Welcome to ChlipChat! \n Login?');

var eb = new EventBus("/eventbus/");
var b;
eb.onopen = function () {
    eb.registerHandler("chat.to.client", function (err, msg) {
        b = jQuery.parseJSON(msg.body);
        $('#chat').append(b.t + ' <strong>' + escapeHTML(b.p.l) + '</strong>: ' + escapeHTML(b.p.m) + "\n");
        $("#chat").scrollTop($("#chat")[0].scrollHeight);
    });
    eb.registerHandler("init", function (err, msg) {
        b = jQuery.parseJSON(msg.body);
        $('#chat').append(b.t + ' <strong>' + escapeHTML(b.p.l) + '</strong>: ' + escapeHTML(b.p.m) + "\n");
        $("#chat").scrollTop($("#chat")[0].scrollHeight);
    });
};

function send(event) {
    if (event.keyCode == 13 || event.which == 13) {
        var message = $('#input').val();
        if (message.length > 0) {
            eb.publish("chat.to.server", {m: message, l: login});
            $('#input').val("");
        }
    }
}

function escapeHTML(s) {
    return s.replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}