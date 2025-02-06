package com.damir00109;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

public class MentalBed {
    private static final Logger LOGGER = LoggerFactory.getLogger("v_island");

    // Карта для хранения состояния сна игрока
    private final Map<UUID, Boolean> playerSleepingState = new HashMap<>();

    // Конфигурация для хранения данных об игроках
    private final PlayerMentalConfig playerMentalConfig;

    // Генератор случайных чисел
    private final Random random = new Random();

    // Таймер для снижения здоровья раз в секунду
    private int tickCounter = 0;

    // Ссылка на объект Zona
    private Zona zona;

    // Конструктор без аргументов
    public MentalBed() {
        this.playerMentalConfig = PlayerMentalConfig.load();
        registerCommands();
        registerTickHandler();
    }

    // Метод для установки ссылки на Zona
    public void setZona(Zona zona) {
        this.zona = zona;
    }

    // Метод для получения конфигурации ментального здоровья
    public PlayerMentalConfig getPlayerMentalConfig() {
        return playerMentalConfig;
    }

    // Логика проверки состояния сна игрока
    public void checkSleepingState(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        boolean isSleeping = player.isSleeping();

        // Получаем данные об игроке
        PlayerMentalConfig.PlayerMentalData data = playerMentalConfig.playerData.get(playerId);

        // Если данные об игроке есть и его ментальное здоровье <= 50
        if (data != null && data.randomValue <= 50) {
            // Если игрок пытается заснуть
            if (isSleeping && (!playerSleepingState.containsKey(playerId) || !playerSleepingState.get(playerId))) {
                // Отменяем сон
                player.wakeUp(false, true); // false — не сбрасывать таймер сна, true — обновить состояние сна

                // Отправляем сообщение в зависимости от языка клиента
                Text message = Text.translatable("block.minecraft.bed.no_sleep");
                player.sendMessage(message, true); // Сообщение в актион баре
            }
        }

        // Обновляем состояние сна игрока
        playerSleepingState.put(playerId, isSleeping);
    }

    // Логика снижения "ментального здоровья" игрока
    public void decreaseMentalHealth(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();

        // Проверяем, находится ли игрок за пределами зоны
        if (zona != null && zona.isPlayerOutsideZone(player)) {
            // Проверяем, есть ли данные об игроке
            if (playerMentalConfig.playerData.containsKey(playerId)) {
                PlayerMentalConfig.PlayerMentalData data = playerMentalConfig.playerData.get(playerId);

                // Определяем, на сколько уменьшить значение (50% на 2, 50% на 1)
                int decreaseAmount = (random.nextBoolean()) ? 2 : 1;

                // Уменьшаем значение, но не ниже 0
                data.randomValue = Math.max(0, data.randomValue - decreaseAmount);

                // Логируем изменение
                LOGGER.info("Ментальное здоровье игрока " + data.playerName + " уменьшено на " + decreaseAmount + ". Текущее значение: " + data.randomValue);

                // Сохраняем изменения
                playerMentalConfig.save();
            }
        }
    }

    // Регистрация команды /checkmental
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("checkmental")
                            .executes(context -> {
                                // Выводим данные всех игроков
                                for (Map.Entry<UUID, PlayerMentalConfig.PlayerMentalData> entry : playerMentalConfig.playerData.entrySet()) {
                                    PlayerMentalConfig.PlayerMentalData data = entry.getValue();

                                    // Формируем сообщение для каждого игрока
                                    String message = data.playerName + " = " + data.randomValue;
                                    context.getSource().sendMessage(Text.of(message));
                                }
                                return 1; // Успешное выполнение команды
                            })
            );
        });
    }

    // Метод для добавления данных об игроке
    public void addPlayerData(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        String playerName = player.getName().getString();

        // Генерируем случайное число от 98 до 100
        int randomValue = 98 + (int) (Math.random() * 3); // 98, 99 или 100

        // Сохраняем данные об игроке в конфиг
        playerMentalConfig.playerData.put(playerId, new PlayerMentalConfig.PlayerMentalData(playerName, randomValue, ""));
        playerMentalConfig.save(); // Сохраняем конфиг в файл
    }

    // Регистрация обработчика тиков
    private void registerTickHandler() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            // Проверяем, прошла ли секунда (20 тиков = 1 секунда)
            if (tickCounter >= 20) {
                tickCounter = 0; // Сбрасываем счетчик

                // Проходим по всем игрокам на сервере
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    // Уменьшаем ментальное здоровье, если игрок в зоне
                    decreaseMentalHealth(player);
                }
            }
        });
    }
}