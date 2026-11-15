package dev.uday.luffy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import org.junit.Test;

public class PiStatsTest {
    @Test
    public void parsesTheBridgeJsonIntoReadableLines() {
        PiStats s = PiStats.parse("{\"cpuTempC\":51.2,\"memUsedPct\":11,\"uptimeS\":8307,\"load1\":0.0,\"diskUsedPct\":37}");
        assertEquals(Arrays.asList("CPU  51.2 °C", "Memory  11 %", "Disk  37 %", "Load  0.00", "Uptime  2h 18m"), s.lines());
    }

    @Test
    public void unreadableFieldsShowADashNotACrash() {
        PiStats s = PiStats.parse("{\"cpuTempC\":null,\"memUsedPct\":11}");
        assertEquals("CPU  —", s.lines().get(0));
        assertEquals("Uptime  —", s.lines().get(4));
    }

    @Test
    public void garbageOrNullParsesToNull() {
        assertNull(PiStats.parse("{nope"));
        assertNull(PiStats.parse(null));
    }

    @Test
    public void uptimeUsesTheTwoLargestUnits() {
        assertEquals("45s", PiStats.uptime(45));
        assertEquals("5m", PiStats.uptime(300));
        assertEquals("2h 18m", PiStats.uptime(8307));
        assertEquals("1d 2h", PiStats.uptime(93784));
    }
}
