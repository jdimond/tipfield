$(document).ready(function() {
  $(".game").each(function(i, elem) {
    $(elem).find(".close").click(function() {
      $(elem).fadeOut(1000, function() {
        $(elem).nextAll('tr:not(.removed)').filter(":hidden").first().fadeIn(1000);
        $(elem).addClass("removed")
      });
    });
  });
});
