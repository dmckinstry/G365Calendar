import Toybox.Graphics;
import Toybox.Lang;
import Toybox.WatchUi;

//! Row chrome for the calendar list.
//! Draws the calendar color accent and separator line for one visible row.
class EventRowDecorationDrawable extends WatchUi.Drawable {

    private var _rowHeight as Number = 0;
    private var _accentColor as Number = Graphics.COLOR_BLUE;

    function initialize(params as Dictionary) {
        Drawable.initialize(params);
    }

    function configure(rowHeight as Number, accentColor as Number) as Void {
        _rowHeight = rowHeight;
        _accentColor = accentColor;
    }

    function draw(dc as Dc) as Void {
        if (!isVisible) {
            return;
        }

        var drawHeight = _rowHeight;
        if (drawHeight <= 0) {
            drawHeight = height;
        }

        dc.setColor(_accentColor, Graphics.COLOR_TRANSPARENT);
        dc.fillRectangle(locX, locY + 4, 4, drawHeight - 8);

        dc.setColor(Graphics.COLOR_DK_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawLine(locX, locY + drawHeight - 1, locX + width, locY + drawHeight - 1);
    }
}