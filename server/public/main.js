$(document).ready(function() {
  $.getJSON("/config", {}, function(data) {
    var jsonContainer = document.getElementById("json-editor");
    var options = {};
    var jsonEditor = new JSONEditor(jsonContainer, options);
    jsonEditor.$blockScrolling = Infinity;
    jsonEditor.set(data.config);

    var codeEditor = ace.edit("code-editor");
    codeEditor.setTheme("ace/theme/mono_industrial");
    var JsonMode = ace.require("ace/mode/json").Mode;
    codeEditor.session.setMode(new JsonMode());

    var codeValue = JSON.stringify(data.config, null, "  ");
    codeEditor.session.setValue(codeValue);

    var getAndPost = function(obj) {
      $.getJSON("/config", {}, function(resp) {
        var config = resp.config;
        $.extend(config, obj);
        $.ajax("config/set", {
          type: "POST",
          data: JSON.stringify(config),
          dataType: "json",
          contentType: "application/json"
        }).done(function() {
          $("#success-msg").show();
        }).fail(function(resp) {
          $("#error-msg-content").text(resp.responseJSON.message);
          $("#error-msg").show();
        });
      });
    };

    $("#sync-to-code").click(function() {
      var obj = jsonEditor.get();
      var value = JSON.stringify(obj, null, "  ");
      codeEditor.session.setValue(value);
      $("#success-msg").show();
    });
    $("#submit-json").click(function() {
      var obj = jsonEditor.get();
      getAndPost(obj);
    });
    $("#reset-json").click(function() {
      $.ajax("config/reload", {
        type: "POST",
        data: "{}",
        dataType: "json",
        contentType: "application/json"
      }).done(function() {
        $.getJSON("config", {}, function(data) {
          jsonEditor.set(data.config);
          $("#success-msg").show();
        });
      });
    });
    $("#sync-to-json").click(function() {
      var value = codeEditor.session.getValue();
      var obj = JSON.parse(value);
      jsonEditor.set(obj);
      $("#success-msg").show();
    });
    $("#submit-code").click(function() {
      var value = codeEditor.session.getValue();
      var obj = JSON.parse(value);
      getAndPost(obj);
    });
    $("#reset-code").click(function() {
      $.ajax("config/reload", {
        type: "POST",
        data: "{}",
        dataType: "json",
        contentType: "application/json"
      }).done(function() {
        $.getJSON("config", {}, function(data) {
          var value = JSON.stringify(data.config, null, "  ");
          codeEditor.session.setValue(value);
          $("#success-msg").show();
        });
      });
    });
    $("#close-success").click(function() {
      $("#success-msg").hide();
    });
    $("#close-error").click(function() {
      $("#error-msg").hide();
    });
  });
});

