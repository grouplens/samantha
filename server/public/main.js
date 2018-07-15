$(document).ready(function() {
  $.getJSON("/config", {}, function(data) {
    var jsonContainer = document.getElementById("json-editor");
    var options = {};
    var jsonEditor = new JSONEditor(jsonContainer, options);
    jsonEditor.set(data.config);

    var codeEditor = ace.edit("code-editor");
    codeEditor.setTheme("ace/theme/mono_industrial");
    var JsonMode = ace.require("ace/mode/json").Mode;
    codeEditor.session.setMode(new JsonMode());

    var value = JSON.stringify(data.config, null, "  ");
    codeEditor.session.setValue(value);
  });
});

