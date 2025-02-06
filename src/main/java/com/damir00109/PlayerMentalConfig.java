package com.damir00109;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMentalConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("v_island");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "playermental.json");

    // Данные об игроках
    public Map<UUID, PlayerMentalData> playerData = new HashMap<>();

    // Загрузка конфигурации из файла
    public static PlayerMentalConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                LOGGER.info("Конфигурация загружена из файла.");
                return GSON.fromJson(reader, PlayerMentalConfig.class);
            } catch (IOException e) {
                LOGGER.error("Ошибка при загрузке конфигурации:", e);
            }
        }
        // Если файла нет, создаем новый конфиг с настройками по умолчанию
        LOGGER.info("Создан новый конфиг с настройками по умолчанию.");
        PlayerMentalConfig config = new PlayerMentalConfig();
        config.save();
        return config;
    }

    // Сохранение конфигурации в файл
    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
            LOGGER.info("Конфигурация сохранена в файл.");
        } catch (IOException e) {
            LOGGER.error("Ошибка при сохранении конфигурации:", e);
        }
    }

    // Класс для хранения данных об игроке
    public static class PlayerMentalData {
        public String playerName;
        public int randomValue;
        public String comment;

        public PlayerMentalData(String playerName, int randomValue, String comment) {
            this.playerName = playerName;
            this.randomValue = randomValue;
            this.comment = comment;
        }
    }
}