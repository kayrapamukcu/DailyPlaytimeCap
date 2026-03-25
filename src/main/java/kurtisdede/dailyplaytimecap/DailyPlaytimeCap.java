package kurtisdede.dailyplaytimecap;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;

public class DailyPlaytimeCap implements ModInitializer {
	public static final String MOD_ID = "dailyplaytimecap";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("DailyPlaytimeCap mod initializing...");
		DPCStore.load();

		// register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
					dispatcher.register(
							Commands.literal("dailyplaytime")
									.then(Commands.literal("set")
											.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
											.then(Commands.argument("player", GameProfileArgument.gameProfile())
													.suggests(
															(commandContext, suggestionsBuilder) -> {
																PlayerList playerList = commandContext.getSource().getServer().getPlayerList();
																return SharedSuggestionProvider.suggest(
																		playerList.getPlayers()
																				.stream()
																				.map(Player::nameAndId)
																				.map(NameAndId::name),
																		suggestionsBuilder
																);
															}
													)
													.then(Commands.argument("time_minutes", IntegerArgumentType.integer(1, 1440))
															.executes(DPCCommands::executeDailyPlaytimeSet)
													)
											)
									)
									.then(Commands.literal("check")
											.executes(DPCCommands::executeDailyPlaytimeCheck)
											.then(Commands.argument("player", GameProfileArgument.gameProfile())
													.suggests(
															(commandContext, suggestionsBuilder) -> {
																PlayerList playerList = commandContext.getSource().getServer().getPlayerList();
																return SharedSuggestionProvider.suggest(
																		playerList.getPlayers()
																				.stream()
																				.map(Player::nameAndId)
																				.map(NameAndId::name),
																		suggestionsBuilder
																);
															}
													)
													.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
													.executes(DPCCommands::executeDailyPlaytimeCheckOther)
											)
									)
									.then(Commands.literal("remove")
											.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
											.then(Commands.argument("player", GameProfileArgument.gameProfile())
													.suggests(
															(commandContext, suggestionsBuilder) -> {
																PlayerList playerList = commandContext.getSource().getServer().getPlayerList();
																return SharedSuggestionProvider.suggest(
																		playerList.getPlayers()
																				.stream()
																				.map(Player::nameAndId)
																				.map(NameAndId::name),
																		suggestionsBuilder
																);
															}
													)
													.executes(DPCCommands::executeDailyPlaytimeRemove)
											)
									)
									.then(Commands.literal("add_extra_time")
											.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
											.then(Commands.argument("player", GameProfileArgument.gameProfile())
													.suggests(
															(commandContext, suggestionsBuilder) -> {
																PlayerList playerList = commandContext.getSource().getServer().getPlayerList();
																return SharedSuggestionProvider.suggest(
																		playerList.getPlayers()
																				.stream()
																				.map(Player::nameAndId)
																				.map(NameAndId::name),
																		suggestionsBuilder
																);
															}
													)
													.then(Commands.argument("time_minutes", IntegerArgumentType.integer(1, 1440))
															.executes(DPCCommands::executeDailyPlaytimeAddExtraTime)
													)
											)
									)
					);
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			UUID uuid = handler.player.getUUID();
			DPCStore.resetIfNewDay();

			DPCStore.PlayerLimit limit = DPCStore.getLimit(uuid);
			if (limit != null && limit.left <= 0) {
				LocalDateTime now = LocalDateTime.now();
				int minutesLeft = 1440 - (now.getHour() * 60 + now.getMinute());
				handler.disconnect(Component.literal("You have reached your daily playtime limit. You can log in again in " + minutesLeft + " minute(s)."));
			}
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			DPCStore.save();
		});

		ServerTickEvents.END_SERVER_TICK.register(DailyPlaytimeTicker::onEndTick);
	}
}

