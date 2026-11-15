package dev.uday.luffy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class BulbStateTest {
    private static final String DEVICE = "{\"id\":\"lamp-main\",\"state\":{\"on\":true,\"reachable\":true,\"brightness\":26}}";

    @Test
    public void parsesADeviceFromAnActionResponse() {
        BulbState s = BulbState.parseDevice(DEVICE);
        assertEquals("On · 26 %", s.summary());
        assertEquals(26, s.brightness());
    }

    @Test
    public void parsesTheFirstDeviceOfASnapshot() {
        assertEquals("On · 26 %", BulbState.parseSnapshot("{\"devices\":[" + DEVICE + "]}").summary());
    }

    @Test
    public void offAndUnreachableAreSaidPlainly() {
        assertEquals("Off · 40 %", BulbState.parseDevice("{\"state\":{\"on\":false,\"reachable\":true,\"brightness\":40}}").summary());
        assertEquals("Bulb unreachable", BulbState.parseDevice("{\"state\":{\"on\":null,\"reachable\":false}}").summary());
    }

    @Test
    public void garbageOrAnEmptySnapshotParsesToNull() {
        assertNull(BulbState.parseDevice("{nope"));
        assertNull(BulbState.parseDevice(null));
        assertNull(BulbState.parseSnapshot("{\"devices\":[]}"));
    }

    @Test
    public void brightnessDefaultsTo50WhenUnknown() {
        assertEquals(50, BulbState.parseDevice("{\"state\":{\"on\":true,\"reachable\":true}}").brightness());
    }
}
