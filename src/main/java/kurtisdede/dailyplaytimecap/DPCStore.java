package kurtisdede.dailyplaytimecap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public class DPCStore {

    public static class PlayerLimit {
        public int cap;
        public int left;

        public PlayerLimit(int cap, int left) {
            this.cap = cap;
            this.left = left;
        }
    }
    public static class SaveData {
        public String lastResetDate;
        public Map<String, PlayerLimit> limits = new HashMap<>();
    }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("dailyplaytime");
    private static final Path FILE = DIR.resolve("limits.json");

    private static SaveData savedata = new SaveData();
    public static PlayerLimit getLimit(UUID uuid) {
        return savedata.limits.get(uuid.toString());
    }

    public static void setLimit(UUID uuid, int seconds) {
        savedata.limits.put(uuid.toString(), new PlayerLimit(seconds, seconds));
        save();
    }

    public static boolean removeLimit(UUID uuid) {
        PlayerLimit removed = savedata.limits.remove(uuid.toString());
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    public static void resetIfNewDay() {
        String today = LocalDate.now().toString();
        if (savedata.lastResetDate == null) {
            savedata.lastResetDate = today;
            save();
            return;
        }

        if (!today.equals(savedata.lastResetDate)) {
            savedata.lastResetDate = today;

            for (PlayerLimit limit : savedata.limits.values()) {
                limit.left = limit.cap;
            }

            save();
        }
    }

    public static void decrementLeft(UUID uuid, boolean updateFile) {
        PlayerLimit limit = getLimit(uuid);
        if (limit == null) return;

        if (limit.left > 0) {
            limit.left = (limit.left - 1);
            if(updateFile)
                save();
        }
    }

    public static void load() {
        try {
            Files.createDirectories(DIR);

            if (!Files.exists(FILE)) {
                save();
                return;
            }

            try (Reader reader = Files.newBufferedReader(FILE)) {
                SaveData loaded = GSON.fromJson(reader, SaveData.class);
                if (loaded == null) {
                    savedata.lastResetDate = LocalDate.now().toString();
                } else {
                    savedata = loaded;
                    if (savedata.limits == null) {
                        savedata.limits = new HashMap<>();
                    }
                    if (savedata.lastResetDate == null) {
                        savedata.lastResetDate = LocalDate.now().toString();
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load daily playtime limits", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(DIR);
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(savedata, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save daily playtime limits", e);
        }
    }
}