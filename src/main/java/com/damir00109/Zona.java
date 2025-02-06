package com.damir00109;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Zona {
    public static final Logger LOGGER = LoggerFactory.getLogger("v_island");

    // Карта для хранения состояния каждого игрока
    private final Map<UUID, Integer> playerWarnings = new HashMap<>();
    // Карта для хранения времени отсрочки для каждого игрока (в тиках)
    private final Map<UUID, Integer> playerCooldowns = new HashMap<>();

    // Таймер для проверки позиции игроков без предупреждений
    private int checkTimer = 0;

    // Конфигурация
    private final ModConfig config;

    // Ссылка на MentalBed для проверки ментального здоровья
    private final MentalBed mentalBed;

    public Zona(ModConfig config, MentalBed mentalBed) {
        this.config = config;
        this.mentalBed = mentalBed;
    }

    // Проверяем, находится ли игрок за пределами зоны
    public boolean isPlayerOutsideZone(ServerPlayerEntity player) {
        int x = (int) player.getX(); // Используем целочисленные координаты
        int z = (int) player.getZ(); // Используем целочисленные координаты

        // Получаем данные об игроке
        PlayerMentalConfig.PlayerMentalData data = mentalBed.getPlayerMentalConfig().playerData.get(player.getUuid());

        // Определяем границы зоны в зависимости от ментального здоровья
        int xBoundary = config.x;
        int xMinusBoundary = config.x_minus;
        int zBoundary = config.z;
        int zMinusBoundary = config.z_minus;

        // Если ментальное здоровье меньше 50, уменьшаем границы на 200 блоков
        if (data != null && data.randomValue < 50) {
            xBoundary -= 200;
            xMinusBoundary += 200;
            zBoundary -= 200;
            zMinusBoundary += 200;
        }

        // Проверяем, находится ли игрок за пределами координат из конфигурации
        return x > xBoundary || x < xMinusBoundary || z > zBoundary || z < zMinusBoundary;
    }

    // Проверяем, находится ли игрок за пределами зоны на 200 блоков (вторая стена)
    public boolean isPlayerFarOutsideZone(ServerPlayerEntity player) {
        int x = (int) player.getX(); // Используем целочисленные координаты
        int z = (int) player.getZ(); // Используем целочисленные координаты

        // Получаем данные об игроке
        PlayerMentalConfig.PlayerMentalData data = mentalBed.getPlayerMentalConfig().playerData.get(player.getUuid());

        // Определяем границы зоны в зависимости от ментального здоровья
        int xBoundary = config.x + 200;
        int xMinusBoundary = config.x_minus - 200;
        int zBoundary = config.z + 200;
        int zMinusBoundary = config.z_minus - 200;

        // Если ментальное здоровье меньше 50, уменьшаем границы на 200 блоков
        if (data != null && data.randomValue < 50) {
            xBoundary -= 200;
            xMinusBoundary += 200;
            zBoundary -= 200;
            zMinusBoundary += 200;
        }

        // Проверяем, находится ли игрок за пределами зоны на 200 блоков
        return x > xBoundary || x < xMinusBoundary || z > zBoundary || z < zMinusBoundary;
    }

    public void checkPlayerPosition(ServerPlayerEntity player) {
        // Проверяем, находится ли игрок в Overworld
        if (player.getWorld().getRegistryKey() != World.OVERWORLD) {
            return; // Если не в Overworld, выходим из метода
        }

        UUID playerId = player.getUuid();

        // Проверяем, находится ли игрок за пределами второй стены (200 блоков)
        if (isPlayerFarOutsideZone(player)) {
            // Если игрок зашел за вторую стену, убиваем его моментально
            killPlayer(player);
            playerWarnings.remove(playerId); // Сбрасываем предупреждения
            playerCooldowns.remove(playerId); // Сбрасываем отсрочку
            return;
        }

        // Если у игрока уже есть предупреждения, проверяем его позицию каждый тик
        if (playerWarnings.containsKey(playerId)) {
            // Проверяем, вернулся ли игрок в зону
            if (!isPlayerOutsideZone(player)) {
                // Если игрок вернулся в зону, сбрасываем его состояние
                playerWarnings.remove(playerId);
                playerCooldowns.remove(playerId);
                return;
            }

            // Если игрок всё ещё за пределами зоны, обрабатываем предупреждения
            handlePlayerWarning(player, playerId);
        } else {
            // Если у игрока нет предупреждений, проверяем его позицию раз в 10 секунд (200 тиков)
            checkTimer++;
            if (checkTimer >= 40) { // 10 секунд (200 тиков)
                checkTimer = 0; // Сбрасываем таймер

                // Проверяем, находится ли игрок за пределами зоны
                if (isPlayerOutsideZone(player)) {
                    handlePlayerWarning(player, playerId);
                }
            }
        }
    }

    private void handlePlayerWarning(ServerPlayerEntity player, UUID playerId) {
        int warningLevel = playerWarnings.getOrDefault(playerId, 0);

        // Проверяем, прошло ли время отсрочки
        int cooldown = playerCooldowns.getOrDefault(playerId, 0);
        if (cooldown > 0) {
            playerCooldowns.put(playerId, cooldown - 1);
            return; // Если отсрочка еще не прошла, выходим из метода
        }

        // Логика предупреждений
        switch (warningLevel) {
            case 0 -> {
                // Первое предупреждение: эффект и частицы
                applyDarknessEffect(player, 5);
                spawnParticles(player);
                playerWarnings.put(playerId, 1);
                playerCooldowns.put(playerId, 100); // 5 секунд (100 тиков) до следующего предупреждения
            }
            case 1 -> {
                // Второе предупреждение: эффект, частицы и урон
                applyDarknessEffect(player, 5);
                spawnParticles(player);
                damagePlayer(player, 4); // 2 сердца (4 HP)
                playerWarnings.put(playerId, 2);
                playerCooldowns.put(playerId, 100); // 5 секунд (100 тиков) до следующего предупреждения
            }
            case 2 -> {
                // Третье предупреждение: эффект, частицы и смерть
                applyDarknessEffect(player, 5);
                spawnParticles(player);
                killPlayer(player);
                playerWarnings.remove(playerId); // Сбрасываем предупреждения
                playerCooldowns.remove(playerId); // Сбрасываем отсрочку
            }
        }
    }

    private void applyDarknessEffect(ServerPlayerEntity player, int durationInSeconds) {
        // Преобразуем секунды в тики (20 тиков = 1 секунда)
        int durationInTicks = durationInSeconds * 20;

        // Создаем эффект darkness с указанной длительностью
        StatusEffectInstance darknessEffect = new StatusEffectInstance(StatusEffects.DARKNESS, durationInTicks, 0);
        player.addStatusEffect(darknessEffect); // Применяем эффект к игроку
    }

    private void spawnParticles(ServerPlayerEntity player) {
        // Получаем мир игрока
        World world = player.getWorld();

        // Проверяем, что мир является ServerWorld
        if (world instanceof ServerWorld serverWorld) {
            // Создаем частицы дыма от костра
            serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY(), player.getZ(), 30, 1, 1, 1, 0.1);
        } else {
            LOGGER.error("Ошибка: Мир игрока не является ServerWorld!");
        }
    }

    private void damagePlayer(ServerPlayerEntity player, float damageAmount) {
        // Наносим урон игроку
        player.damage(player.getDamageSources().generic(), damageAmount);
    }

    private void killPlayer(ServerPlayerEntity player) {
        // Убиваем игрока
        player.kill();
    }
}