package dev.uday.luffy;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ActionsTest {
    @Test
    public void buildsTheBridgeActionJson() {
        assertEquals("{\"type\":\"toggle\"}", Actions.toggle());
        assertEquals("{\"type\":\"power\",\"on\":false}", Actions.power(false));
        assertEquals("{\"type\":\"brightness\",\"value\":30}", Actions.brightness(30));
        assertEquals("{\"type\":\"mode\",\"name\":\"warm\"}", Actions.mode("warm"));
        assertEquals("{\"type\":\"color\",\"hex\":\"#ff0000\"}", Actions.color("#ff0000"));
    }

    @Test
    public void brightnessIsClampedTo0To100() {
        assertEquals("{\"type\":\"brightness\",\"value\":100}", Actions.brightness(250));
        assertEquals("{\"type\":\"brightness\",\"value\":0}", Actions.brightness(-5));
    }
}
