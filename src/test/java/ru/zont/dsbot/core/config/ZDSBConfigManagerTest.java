package ru.zont.dsbot.core.config;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import ru.zont.dsbot.core.StandardTest;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class ZDSBConfigManagerTest extends StandardTest {
    public static final String TAG_COPY_CONFIGS = "CopyConfigs";

    private static final File cfgDir = new File("cfg-test");
    private static final String defaultValue = "default-value";
    private ZDSBConfigManager<TestConfig, ZDSBBotConfig> manager;

    private static class TestConfig extends ZDSBBasicConfig {
        protected TestConfig(String configName, File dir, ZDSBConfig inherit) {
            super(configName, dir, inherit);
            prefix = new Entry(defaultValue);
        }

        public Entry default_entry_field = new Entry(defaultValue);
        public Entry default_entry_field_no_inherit = new Entry(defaultValue, true);
    }

    @BeforeEach
    void setUp(TestInfo info) throws IOException {
        if (cfgDir.exists())
            FileUtils.forceDelete(cfgDir);
        manager = new ZDSBConfigManager<>(cfgDir.getPath(), TestConfig.class);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (cfgDir.exists())
            FileUtils.forceDelete(cfgDir);
    }

    @Test
    void guildConfig() {
        final String id = "133726118635208716";
        final TestConfig cfg = manager.guildConfig(id);
        final TestConfig gCfg = manager.globalConfig();

        assertTrue(new File(cfgDir, "guild-" + id + ".properties").isFile());
        assertEquals(gCfg.prefix.getValue(), cfg.prefix.getValue());
    }

    @Test
    void cfgUpdate() throws IOException {
        final String id = "133726118635208716";
        final TestConfig cfg = manager.guildConfig(id);
        final TestConfig gCfg = manager.globalConfig();
        final File file = new File(cfgDir, "guild-" + id + ".properties");

        assertTrue(file.isFile());
        assertEquals(file, cfg.getConfigFile());
        assertEquals(gCfg.prefix.getValue(), cfg.prefix.getValue());

        copyResource("/local_config_test.properties", file);

        assertEquals("//", cfg.prefix.getValue());
        assertEquals(defaultValue, gCfg.prefix.getValue());

        copyResource("/global_config_test.properties", gCfg.getConfigFile());

        assertEquals("//", cfg.prefix.getValue());
        assertEquals("zdsb.", gCfg.prefix.getValue());
    }

    @Test
    void overridedOnly() throws IOException {
        final String id = "133726118635208716";
        final TestConfig cfg = manager.guildConfig(id);
        final TestConfig gCfg = manager.globalConfig();

        copyResource("/local_config_test.properties", gCfg.getConfigFile());

        assertEquals("//", gCfg.prefix.getValue());
        assertEquals("//", cfg.prefix.getValue());

        copyResource("/global_config_test.properties", gCfg.getConfigFile());

        assertEquals("zdsb.", gCfg.prefix.getValue());
        assertEquals("zdsb.", cfg.prefix.getValue());

        copyResource("/local_config_test.properties", gCfg.getConfigFile());
        cfg.prefix.setValue("r.");

        assertEquals("//", gCfg.prefix.getValue());
        assertEquals("r.", cfg.prefix.getValue());

        copyResource("/global_config_test.properties", gCfg.getConfigFile());

        assertEquals("zdsb.", gCfg.prefix.getValue());
        assertEquals("r.", cfg.prefix.getValue());
    }

    @Test
    void globalConfig() {
        assertEquals(defaultValue, manager.globalConfig().default_entry_field.getValue());
    }

    @Test
    void botConfig() throws IOException {
        final ZDSBBotConfig cfg = manager.botConfig();
        final String id = "331524458806247426";

        assertEquals(new File(cfgDir, "config.properties"), cfg.getConfigFile());
        assertTrue(cfg.operators.getValue().contains("1337"));
        assertTrue(cfg.isOperator(id));

        copyResource("/config_test.properties", cfg.getConfigFile());

        assertFalse(cfg.isOperator(id));
    }
}