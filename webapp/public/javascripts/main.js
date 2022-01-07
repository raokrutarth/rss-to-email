function onMoodUpdate(newMood) {
  var queryParams = new URLSearchParams(window.location.search);
  queryParams.set("positivity", newMood);
  window.location.search = queryParams.toString();
}