$( document ).ready(function() {
  var container = document.getElementById("json-editor");
  var options = {};
  var editor = new JSONEditor(container, options);
  $.getJSON("/config", {}, function(data) {
    editor.set(data.config);
  });
});

