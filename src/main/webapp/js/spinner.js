(function($) {
  var spinnerCode = '<span class="spinner_outer"><div class="spinner_buttons"><div class="spinner_button">' +
                    '<span class="spinner_up"><a href="#" tabindex="-1" />&#9650;</a></span></div>' +
                    '<div class="spinner_button"><span class="spinner_down"><a href="#" tabindex="-1">&#9660;</a>' +
                    '</span></div></div></span>';
  var keyCode = {
    BACKSPACE: 8,
    ENTER: 13,
    TAB: 9,
    DELETE: 46
  };

  var validKeys = [keyCode.BACKSPACE, keyCode.ENTER, keyCode.TAB, keyCode.DELETE];

  function isSpecialKey(keyCode) {
    for (var i=0; i<validKeys.length; i++)
      if (validKeys[i] == keyCode) return true;
    return false;
  }

  function invalidKey(input, keyCode, charCode) {
    if (typeof charCode == 'undefined' || charCode == 0) return false;

    var ch = String.fromCharCode(charCode || keyCode);
    if ((ch >= '0') && (ch <= '9')) {
      input.val('');
      return false;
    }
    return true;
  }

  function makeSpinner(input) {
    var lastValue = input.val();
    var $form = input.closest("form");
    function submitConditionally() {
      if (lastValue != input.val()) {
        $form.submit();
        lastValue = input.val();
      }
    }
    function change(delta) {
      var value = parseInt(input.val());
      value = (isNaN(value)) ? 0 : value;
      value = (value + delta > 9) ? 9 - delta : value;
      value = (value + delta < 0) ? 0 - delta : value;
      input.val(value + delta);
      submitConditionally();
    }

    $spinner = $(spinnerCode);
    $spinner.find(".spinner_up").bind('click', function() {
      change(1);
    });
    $spinner.find(".spinner_down").bind('click', function() {
      change(-1);
    });

    input.after($spinner);
    input.bind('keypress', function(e) {
      if (invalidKey(input, e.keyCode, e.charCode)) return false;
    });
    input.bind('keyup', function(e) {
      submitConditionally();
    });
    /*
    input.mousewheel(function(e, delta) {
      if (delta > 0) {
        change(1);
      } else if (delta < 0) {
        change(-1);
      }
      return false;
    });
    $spinner.mousewheel(function(e, delta) {
      if (delta > 0) {
        change(1);
      } else if (delta < 0) {
        change(-1);
      }
      return false;
    });
    */
  }

  $(document).ready(function() {
    $(".spinner").each(function (i, elem) {
      makeSpinner($(elem));
    });
  });
})(jQuery);
