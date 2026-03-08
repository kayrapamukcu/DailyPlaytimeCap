package kurtisdede.dailyplaytimecap;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

public class DailyPlaytimeTicker {
    private static int lastProcessedMinute = -1;
    private static int lastProcessedSecond = -1;
    private static String lastProcessedDate = "";

    static void onEndTick(MinecraftServer server) {
        LocalDateTime now = LocalDateTime.now();
        String currentDate = now.toLocalDate().toString();
        int currentMinute = now.getHour() * 60 + now.getMinute();
        int currentSecond = currentMinute * 60 + now.getSecond();

        if (!currentDate.equals(lastProcessedDate)) {
            lastProcessedDate = currentDate;
            lastProcessedMinute = currentMinute;
            lastProcessedSecond = currentSecond;

            DPCStore.resetIfNewDay();
            return;
        }

        if(lastProcessedMinute != currentMinute) {
            lastProcessedMinute = currentMinute;
            DPCStore.save();
        }

        if (currentSecond == lastProcessedSecond) {
            return;
        }

        lastProcessedSecond = currentSecond;

        for (ServerPlayer player : new ArrayList<>(server.getPlayerList().getPlayers())) {
            UUID uuid = player.getUUID();
            DPCStore.PlayerLimit limit = DPCStore.getLimit(uuid);

            if (limit == null) {
                continue;
            }

            if (limit.left > 0) {
                DPCStore.decrementLeft(uuid, false);
                limit = DPCStore.getLimit(uuid);
            }

            if (limit != null && limit.left <= 0) {
                DPCStore.save();
                player.connection.disconnect(Component.literal("You have reached your playtime limit for today."));
            }
        }
    }
}