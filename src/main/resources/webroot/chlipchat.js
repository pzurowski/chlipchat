(function (){

var login = window.prompt('Welcome to ChlipChat!\nLogin?');
var handlerID;

var ADDRESS_TO_SERVER = "chat.to.server";
var ADDRESS_TO_CLIENT = "chat.to.client";
var ADDRESS_MAINTENANCE = "chat.MAINTENANCE";
var LOCALE = 'en-GB';

var eb = new EventBus("/eventbus/");

var Notification = window.Notification || window.mozNotification || window.webkitNotification;
Notification.requestPermission(function (permission) {});

    var chat = new Chat();
    var input = new Input();
$(document).ready(function () {
    chat.setElement($('#chat'));
    input.setElement($('#input'));
    input.focus();
});

var window_focus;

$(window).focus(function() {
    window_focus = true;
}).blur(function() {
    window_focus = false;
});


//TODO: move ADDRESS_MAINTENANCE receive handling to receive
//TODO: notifications by mention, all, none

var data = Bind({
    users: []
}, {
    users: {
        dom: '#user-list',
        transform: function (value) {
            return '<li>' + this.safe(value.login) + '</li>';
        }
    }
});

var cmdHandlers = {
    handlerID: handlerIDHandler,
    userJoined: userJoinedHandler,
    userLeft: userLeftHandler,
    _default_: function(err,msg){
        console.log('unknown Message type', err, msg);
    }
};

eb.onopen = function () {
    eb.registerHandler(ADDRESS_TO_CLIENT, receive);
    eb.registerHandler(ADDRESS_TO_CLIENT, notify);
    eb.registerHandler(ADDRESS_MAINTENANCE, function (err, msg) {
        (cmdHandlers[msg.body.cmd] || cmdHandlers._default_)(err,msg);
    });
};
        function handlerIDHandler(err,msg){
            handlerID = msg.body.handlerID;
            eb.send(ADDRESS_TO_SERVER, {cmd: 'user-list', handlerID: msg.body.handlerID, login: login}, function (err, msg) {
                data.users = msg.body;
            });
            eb.send(ADDRESS_TO_SERVER, {cmd: 'init', handlerID: msg.body.handlerID, login: login}, receive);
        }
        function userJoinedHandler(err,msg){
            data.users.push({ login: msg.body.login, handlerID: msg.body.handlerID });
            chat.addUserAction(msg.body.login, 'joined');
        }
        function userLeftHandler(err,msg){
            data.users = data.users.filter(function (e) {
                return e.handlerID != msg.body.handlerID;
            });
            chat.addUserAction(msg.body.login, 'left');
        }

eb.onclose = function () {
    alert('Connection to server closed, please reload');
};

function notify(err, msg) {
    if(window_focus) return;
    var messages = Array.isArray(msg.body) ? msg.body : [msg.body];
    messages.forEach(function (message) {
        if(message.handlerID != handlerID) {
            var notification = new Notification(
                escape(message.login) + ' ' + timestamp(message.instant)
                , {
                    body: escape(message.payload),
                    icon: '/logo_small.png'
                });
            setTimeout(function(){
                notification.close()
            }, 5000);
        }
    });
}

function receive(err, msg) {
    var messages = Array.isArray(msg.body) ? msg.body : [msg.body];
    messages.forEach(function (message) {
        chat.addMessage(message);
    });
    chat.updateScroll();
}

    function Chat() {
    }
    Chat.prototype.setElement = function setElement($el) {
        this.$el = $el;
    };
    Chat.prototype.addUserAction = function addUserAction(user, action) {
        if (this.$el) {
            this.$el.append('<li><p>User <strong>' + escape(user) + '</strong> ' + action + '</p></li>');
        }
        this.updateScroll();
    };
    Chat.prototype.updateScroll = function updateScroll() {
        if (this.$el) {
            this.$el.scrollTop(this.$el[0].scrollHeight);
        }
    };
    Chat.prototype.addMessage = function (message) {
        if (this.$el) {
            this.$el.append('<li><div class="message-data"><p>' + escape(message.login) + '</p><p>' + timestamp(message.instant) + '</p></div><div class="message"><pre>' + escape(message.payload) + '</pre></div></li>');
        }
    };

    function Input() {
    }
    Input.prototype.setElement = function setElement($el) {
        this.$el = $el;
        this.$el.keypress(this.send.bind(this));
    };
    Input.prototype.focus = function focus() {
        if (this.$el) {
            this.$el.focus();
        }
    };
    Input.prototype.send = function send(event) {
        if (this.$el && !event.shiftKey && (event.keyCode == 13 || event.which == 13)) {
            var message = this.$el.val();
        if (message.length > 0) {
            eb.publish(ADDRESS_TO_SERVER, {payload: message, login: login, handlerID: handlerID});
            this.$el.val('');
        }
        return false;
    }
    return true;
    };

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

})();