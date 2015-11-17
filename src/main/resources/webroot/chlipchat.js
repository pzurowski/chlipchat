var login = window.prompt('Welcome to ChlipChat!\nLogin?');

const ADDRESS_TO_SERVER = "chat.to.server";
const ADDRESS_TO_CLIENT = "chat.to.client";
const LOCALE = 'en-GB';

var eb = new EventBus("/eventbus/");

var chatWindow;
var inputWindow;

$(document).ready(function () {
    chatWindow = $('#chat');
    inputWindow = $('#input');
});

eb.onopen = function () {
    eb.registerHandler(ADDRESS_TO_CLIENT, receive);
    eb.send(ADDRESS_TO_SERVER, {TYPE: 'INIT'}, receive);
};

function receive(err, msg) {
    var messages = Array.isArray(msg.body) ? msg.body : [msg.body];
    messages.forEach(function (message) {
        chatWindow.append(timestamp(message.instant) + ' <strong>' + escape(message.login) + '</strong>: ' + escape(message.payload) + "\n");
    });
    chatWindow.scrollTop(chatWindow[0].scrollHeight);
}

function send(event) {
    if (event.keyCode == 13 || event.which == 13) {
        var message = inputWindow.val();
        if (message.length > 0) {
            eb.publish(ADDRESS_TO_SERVER, {payload: message, login: login});
            inputWindow.val('');
        }
    }
}

function timestamp(instant) {
    return new Date(instant).toLocaleString(locale());
}

function locale() {
    return (navigator.languages && navigator.languages[0]) || navigator.language || LOCALE;
}

function escape(s) {
    return s.replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}