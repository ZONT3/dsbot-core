package ru.zont.dsbot.core.config;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;
import ru.zont.dsbot.core.StandardTest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZDSBConfigTest extends StandardTest {
    private static class TestConfig extends ZDSBContextConfig {
        protected TestConfig(String configName, File dir, ZDSBConfig inherit) {
            super(configName, dir, inherit);
            prefix = new Entry(defaultValue);
        }

        public Entry default_entry_field = new Entry(defaultValue);
        public Entry default_entry_field_no_inherit = new Entry(defaultValue, true);
    }

    public static final String TAG_COPY_CONFIGS = "CopyConfigs";
    public static final String TAG_DONT_SETUP_CFGS = "DontSetupCfgs";

    private static final File cfgDir = new File("cfg-test");
    private static final String defaultValue = "default-value";

    private TestConfig globalConfig;
    private TestConfig localConfig;

    @BeforeEach
    void setUp(TestInfo info) throws IOException {
        if (cfgDir.exists())
            FileUtils.forceDelete(cfgDir);
        if (info.getTags().contains(TAG_COPY_CONFIGS))
            copyConfigs();
        if (info.getTags().contains(TAG_DONT_SETUP_CFGS)) {
            globalConfig = null;
            localConfig = null;
        } else {
            globalConfig = ZDSBConfig.newInstance(TestConfig.class, "global_config_test", cfgDir, null, null);
            localConfig = ZDSBConfig.newInstance(TestConfig.class, "local_config_test", cfgDir, globalConfig, null, true);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        if (cfgDir.exists())
            FileUtils.forceDelete(cfgDir);
    }

    static void copyConfigs() throws IOException {
        FileUtils.forceMkdir(cfgDir);
        for (String name : List.of("/config_test.properties", "/global_config_test.properties", "/local_config_test.properties")) {
            copyResource(name, new File(cfgDir, name));
        }
    }

    @Test
    void getEntries() {
        HashMap<String, ZDSBConfig.Entry> entries = globalConfig.getEntries();
        assertTrue(entries.containsKey("prefix"));
        assertTrue(entries.containsKey("default_entry_field"));
        assertTrue(entries.containsKey("default_entry_field_no_inherit"));
        assertEquals(defaultValue, entries.get("prefix").getValue());
        assertEquals(defaultValue, entries.get("default_entry_field").getValue());
        assertEquals(defaultValue, entries.get("default_entry_field_no_inherit").getValue());

        entries = localConfig.getEntries();
        assertTrue(entries.containsKey("prefix"));
        assertTrue(entries.containsKey("default_entry_field"));
        assertTrue(entries.containsKey("default_entry_field_no_inherit"));
        assertEquals(defaultValue, entries.get("prefix").getValue());
        assertEquals(defaultValue, entries.get("default_entry_field").getValue());
        assertNull(entries.get("default_entry_field_no_inherit").getValue());
    }

    @Test
    void defaultConfigs() {
        assertEquals(defaultValue, globalConfig.default_entry_field.getValue());
        assertEquals(defaultValue, globalConfig.default_entry_field_no_inherit.getValue());
        assertEquals(defaultValue, localConfig.default_entry_field.getValue());
        assertNull(localConfig.default_entry_field_no_inherit.getValue());
    }

    @SuppressWarnings("unused")
    @Test
    @Tag(TAG_DONT_SETUP_CFGS)
    void storeConfigs() throws IOException {
        assertFalse(cfgDir.exists());
        assertNull(globalConfig);
        assertNull(localConfig);

        globalConfig = ZDSBConfig.newInstance(TestConfig.class, "global_config_test", cfgDir, null, null);
        localConfig = ZDSBConfig.newInstance(TestConfig.class, "local_config_test", cfgDir, globalConfig, null, true);
        TestConfig createdConfig = ZDSBConfig.newInstance(TestConfig.class, "created_local_config_test", cfgDir, globalConfig, null, true);

        globalConfig.setShouldCloneInherited(true);
        localConfig.setShouldCloneInherited(true);
        createdConfig.setShouldCloneInherited(true);

        File globalFile = new File(cfgDir, "global_config_test.properties");
        File localFile = new File(cfgDir, "local_config_test.properties");
        File createdFile = new File(cfgDir, "created_local_config_test.properties");

        assertTrue(cfgDir.exists());
        assertTrue(cfgDir.isDirectory());
        assertTrue(globalFile.isFile());
        assertTrue(localFile.isFile());
        assertTrue(createdFile.isFile());

        String globalStr = IOUtils.toString(globalFile.toURI(), StandardCharsets.UTF_8);
        String localStr = IOUtils.toString(localFile.toURI(), StandardCharsets.UTF_8);
        String createdStr = IOUtils.toString(createdFile.toURI(), StandardCharsets.UTF_8);

        assertTrue(globalStr.contains("default_entry_field"), "Actual file contents:\n" + globalStr);
        assertTrue(globalStr.contains("default_entry_field_no_inherit"), "Actual file contents:\n" + globalStr);
        assertTrue(localStr.contains("default_entry_field"), "Actual file contents:\n" + localStr);
        assertFalse(localStr.contains("default_entry_field_no_inherit"), "Actual file contents:\n" + localStr);
        assertTrue(createdStr.contains("default_entry_field"), "Actual file contents:\n" + createdStr);
        assertFalse(createdStr.contains("default_entry_field_no_inherit"), "Actual file contents:\n" + createdStr);
    }

    @Test
    @Tag(TAG_COPY_CONFIGS)
    void inheritAndStoreConfigs() throws IOException {
        TestConfig createdConfig = ZDSBConfig.newInstance(TestConfig.class, "created_local_config_test", cfgDir, globalConfig, null, true);

        File globalFile = new File(cfgDir, "global_config_test.properties");
        File localFile = new File(cfgDir, "local_config_test.properties");
        File createdFile = new File(cfgDir, "created_local_config_test.properties");

        assertTrue(cfgDir.exists());
        assertTrue(cfgDir.isDirectory());
        assertTrue(globalFile.isFile());
        assertTrue(localFile.isFile());
        assertTrue(createdFile.isFile());

        String globalStr = IOUtils.toString(globalFile.toURI(), StandardCharsets.UTF_8);
        String localStr = IOUtils.toString(localFile.toURI(), StandardCharsets.UTF_8);
        String createdStr = IOUtils.toString(createdFile.toURI(), StandardCharsets.UTF_8);

        assertTrue(globalStr.contains("default_entry_field"), "Actual file contents:\n" + globalStr);
        assertTrue(globalStr.contains("default_entry_field_no_inherit"), "Actual file contents:\n" + globalStr);
        assertTrue(localStr.contains("default_entry_field"), "Actual file contents:\n" + localStr);
        assertFalse(localStr.contains("default_entry_field_no_inherit"), "Actual file contents:\n" + localStr);
        assertTrue(createdStr.contains("default_entry_field"), "Actual file contents:\n" + createdStr);
        assertFalse(createdStr.contains("default_entry_field_no_inherit"), "Actual file contents:\n" + createdStr);

        assertTrue(globalStr.contains("prefix"));
        assertTrue(globalStr.contains("global_field"));
        assertEquals("yes", globalConfig.getValue("global_field"));
        assertEquals("yes", localConfig.getValue("local_field"));
        assertEquals("yes", localConfig.getValue("global_field"));
        assertNull(globalConfig.getValue("local_field"));

        assertEquals("zdsb.", globalConfig.prefix.getValue());
        assertEquals("//", localConfig.prefix.getValue());
        assertEquals("zdsb.", createdConfig.prefix.getValue());

        createdConfig.prefix.setValue("r.");
        createdStr = IOUtils.toString(createdFile.toURI(), StandardCharsets.UTF_8);

        assertEquals("zdsb.", globalConfig.prefix.getValue());
        assertEquals("//", localConfig.prefix.getValue());
        assertEquals("r.", createdConfig.prefix.getValue());
        assertTrue(createdStr.contains("r."), "Actual file contents:\n" + createdStr);
    }

    @Test
    void updateConfig() throws IOException {
        assertEquals(defaultValue, localConfig.prefix.getValue());
        assertEquals(defaultValue, localConfig.default_entry_field.getValue());
        assertTrue(new File(cfgDir, "global_config_test.properties").isFile());

        copyConfigs();

        assertEquals("//", localConfig.prefix.getValue());
        assertEquals(defaultValue, localConfig.default_entry_field.getValue());
    }
}