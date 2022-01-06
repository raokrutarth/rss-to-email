function onMoodUpdate(newMood) {
  // trim existing query params and reset mood filter
  window.location.href = window.location.href.split('?')[0] + "?positivity=" + newMood;
}