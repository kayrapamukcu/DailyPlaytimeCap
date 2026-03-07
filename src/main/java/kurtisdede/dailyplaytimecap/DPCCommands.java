package kurtisdede.dailyplaytimecap;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.Collection;
import java.util.UUID;

public class DPCCommands {

	public static int executeDailyPlaytimeSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException{
		Collection<NameAndId> targets = GameProfileArgument.getGameProfiles(context, "player");

		int minutes_r = context.getArgument("time_minutes", Integer.class);
		final int minutes = Math.max(1, Math.min(minutes_r, 1440));

		for(NameAndId target : targets) {
			UUID uuid = target.id();
			String name = target.name();

			DPCStore.setLimit(uuid, minutes * 60);

			context.getSource().sendSuccess(() -> Component.literal("Set daily playtime for " + name + " to " + minutes + " minute(s)."), false);
			ServerPlayer onlinePlayer = context.getSource().getServer().getPlayerList().getPlayer(uuid);
			if(onlinePlayer != null)
				onlinePlayer.sendSystemMessage(Component.literal("Your daily playtime limit has been set to " + minutes + " minute(s)."), false);
		}

        return 1;
    }

	public static int executeDailyPlaytimeRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException{
		Collection<NameAndId> targets = GameProfileArgument.getGameProfiles(context, "player");

		for(NameAndId target : targets) {
			UUID uuid = target.id();
			String name = target.name();

			boolean removed = DPCStore.removeLimit(uuid);

			if (removed) {
				context.getSource().sendSuccess(() -> Component.literal("Removed daily playtime limit for " + name + "."), false);
				ServerPlayer onlinePlayer = context.getSource().getServer().getPlayerList().getPlayer(uuid);
				if(onlinePlayer != null)
					onlinePlayer.sendSystemMessage(Component.literal("Your daily playtime limit has been removed."), false);
			} else {
				context.getSource().sendFailure(Component.literal(name + " does not have a daily playtime limit set."));
			}
		}
		return 1;
	}

	public static int executeDailyPlaytimeCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		DPCStore.PlayerLimit limit = DPCStore.getLimit(player.getUUID());

		if (limit == null) {
			player.sendSystemMessage(Component.literal("You do not currently have a daily playtime limit."), false);
			return 1;
		}

		player.sendSystemMessage(Component.literal("You have " + limit.left / 60 + " minute(s), " + limit.left % 60 + " second(s) left."), false);
		return 1;
	}

	public static int executeDailyPlaytimeCheckOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException{
		Collection<NameAndId> targets = GameProfileArgument.getGameProfiles(context, "player");

		for(NameAndId target : targets) {
			UUID uuid = target.id();
			String name = target.name();

			DPCStore.PlayerLimit limit = DPCStore.getLimit(target.id());

			if (limit == null) {
				context.getSource().sendSuccess(() -> Component.literal(name + " does not currently have a daily playtime limit."), false);
				continue;
			}
			context.getSource().sendSuccess(() -> Component.literal(name + " has " + limit.left / 60 + " minute(s), " + limit.left % 60 + " second(s) left."), false);
		}

		return 1;
	}
}