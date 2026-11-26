package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TwinConfigTest {
    @Test
    public void missingKeysMeansDisabled() {
        assertFalse(TwinConfig.parse("{}").enabled);
    }

    @Test
    public void malformedJsonMeansDisabledNotCrash() {
        assertFalse(TwinConfig.parse("{nope").enabled);
        assertFalse(TwinConfig.parse(null).enabled);
    }

    @Test
    public void enabledNeedsAUrl() {
        assertFalse(TwinConfig.parse("{\"twin_enabled\":true}").enabled);
        assertFalse(TwinConfig.parse("{\"twin_enabled\":true,\"twin_url\":\"  \"}").enabled);
    }

    @Test
    public void enabledWithUrlAndToken() {
        TwinConfig c = TwinConfig.parse("{\"twin_enabled\":true,\"twin_url\":\"http://luffy.local:8788/\",\"twin_token\":\"a b\"}");
        assertTrue(c.enabled);
        assertEquals("http://luffy.local:8788", c.url);
    }

    @Test
    public void toggleOffInConfigDisablesWithoutRebuild() {
        assertFalse(TwinConfig.parse("{\"twin_enabled\":false,\"twin_url\":\"http://x\",\"twin_token\":\"t\"}").enabled);
    }

    @Test
    public void pairUrlSetsCookieThenLandsOnTheRemoteViewWithTvDefaults() {
        TwinConfig c = TwinConfig.parse("{\"twin_enabled\":true,\"twin_url\":\"http://h:8788\",\"twin_token\":\"a b&c\"}");
        assertEquals("http://h:8788/pair?code=a+b%26c&next=%2F%3Fview%3D3d%26remote%3D1%26lite%3D1%26night%3D1%26scene%3D%2Fshared%2Froom.scene.json", c.pairUrl());
    }

    @Test
    public void pageOptionsCanBeTunedFromConfigWithoutARebuild() {
        TwinConfig c = TwinConfig.parse("{\"twin_enabled\":true,\"twin_url\":\"http://h\",\"twin_token\":\"t\",\"twin_query\":\"view=3d&tilt=50\"}");
        assertEquals("http://h/pair?code=t&next=%2F%3Fview%3D3d%26tilt%3D50", c.pairUrl());
    }

    @Test
    public void healthUrlNeedsNoToken() {
        assertEquals("http://h:8788/api/health", TwinConfig.parse("{\"twin_enabled\":true,\"twin_url\":\"http://h:8788\"}").healthUrl());
    }

    @Test
    public void interactiveModeKeyDefaultsToInfoAndIsConfigurable() {
        assertEquals(165, TwinConfig.parse("{\"twin_enabled\":true,\"twin_url\":\"http://h\"}").toggleKey);
        assertEquals(186, TwinConfig.parse("{\"twin_enabled\":true,\"twin_url\":\"http://h\",\"twin_toggle_key\":186}").toggleKey);
    }
}
