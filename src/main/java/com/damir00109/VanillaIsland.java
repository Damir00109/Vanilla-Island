package com.damir00109;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class VanillaIsland implements ModInitializer {
	public static final String MOD_ID = "v_island";

	// Конфигурация
	public static ModConfig config;

	// Экземпляры классов
	private Zona zona;
	private MentalBed mentalBed;

	@Override
	public void onInitialize() {
		// Загружаем конфигурацию
		config = ModConfig.load();

		// Инициализируем классы
		mentalBed = new MentalBed(); // Сначала создаём MentalBed
		zona = new Zona(config, mentalBed); // Затем передаём его в Zona

		// Регистрируем обработчик для тиков сервера
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				// Проверяем позицию игрока
				zona.checkPlayerPosition(player);

				// Проверяем состояние сна игрока
				mentalBed.checkSleepingState(player);
			}
		});

		// Регистрируем обработчик подключения игрока
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.player;

			// Добавляем данные об игроке
			mentalBed.addPlayerData(player);
		});
	}
}