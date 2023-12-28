package me.rosillogames.eggwars.arena.game;

import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import me.rosillogames.eggwars.EggWars;
import me.rosillogames.eggwars.arena.Arena;
import me.rosillogames.eggwars.arena.Generator;
import me.rosillogames.eggwars.arena.Team;
import me.rosillogames.eggwars.enums.ArenaStatus;
import me.rosillogames.eggwars.language.TranslationUtils;
import me.rosillogames.eggwars.loaders.ArenaLoader;
import me.rosillogames.eggwars.loaders.KitLoader;
import me.rosillogames.eggwars.player.EwPlayer;
import me.rosillogames.eggwars.utils.TeamUtils;
import me.rosillogames.eggwars.utils.VoteUtils;

public class Lobby
{
    public static void doStartingPhase(final Arena arenaIn)
    {
        arenaIn.setStatus(ArenaStatus.STARTING);
        arenaIn.setCurrentCountdown(arenaIn.getStartCountdown() + 1);
        (new BukkitRunnable()
        {
            private boolean full = false;

            @Override
            public void run()
            {
                if (!arenaIn.getStatus().equals(ArenaStatus.STARTING))
                {
                    this.cancel();
                    return;
                }

                if (!arenaIn.hasEnoughPlayers())
                {
                    for (EwPlayer ewplayer : arenaIn.getPlayers())
                    {
                        ewplayer.getPlayer().setLevel(0);
                        ewplayer.getPlayer().setExp(0.0F);
                    }

                    arenaIn.sendBroadcast("gameplay.lobby.not_enough_players");
                    arenaIn.setStatus(ArenaStatus.WAITING);
                    this.cancel();
                    return;
                }

                int countDown = arenaIn.getCurrentCountdown() - 1;
                boolean changedToFull = false;

                if (arenaIn.isFull() && !this.full && arenaIn.getFullCountdown() >= 0)
                {
                    this.full = true;
                    int fullCD = arenaIn.getFullCountdown();

                    if (countDown > fullCD)
                    {
                        countDown = fullCD;
                    }

                    changedToFull = true;
                }

                if (countDown != 0 && (countDown % 30 == 0 || (countDown < 30 && countDown % 10 == 0) || countDown <= 5) || countDown == arenaIn.getStartCountdown())
                {
                    playCountDown(arenaIn, "starting", countDown);
                }

                for (EwPlayer ewplayer1 : arenaIn.getPlayers())
                {
                    ewplayer1.getPlayer().setLevel(countDown);
                    ewplayer1.getPlayer().setExp(0.0F);

                    if (changedToFull)
                    {
                        TranslationUtils.sendMessage("gameplay.lobby.full_countdown", ewplayer1.getPlayer(), TranslationUtils.translateTime(ewplayer1.getPlayer(), countDown, true));
                    }
                }

                if (countDown <= 0)
                {
                    this.cancel();
                    endStartingPhase(arenaIn);
                    return;
                }

                arenaIn.setCurrentCountdown(countDown);
            }

            @Override
            public void cancel()
            {
                super.cancel();
                arenaIn.setCurrentCountdown(0);
            }
        }).runTaskTimer(EggWars.instance, 0L, 20L);
    }

    public static void endStartingPhase(Arena arenaIn)
    {
        if (arenaIn.skipSoloLobby())
        {
            //"solo" start has to skip ArenaStatus.STARTING_GAME and should use defCountdown
            arenaIn.setupVotedResults();
            arenaIn.loadShop();

            for (Team team : arenaIn.getTeams().values())
            {
                team.prepareForGame();
            }

            for (Generator gen : arenaIn.getGenerators().values())
            {
                gen.prepareForGame();
            }

            Starting.releasePlayersAndStartGame(arenaIn);
            arenaIn.getScores().updateScores(true);
        }
        else
        {
            Starting.doReleasingPhase(arenaIn);
        }
    }

    public static void onEnter(Arena arenaIn, EwPlayer ewplayer)
    {//TODO: Add a way to auto-update these item's when config or lang is changed
        if (arenaIn.skipSoloLobby())
        {
            for (Team team : arenaIn.getTeams().values())
            {
                if (team.getPlayers().size() <= 0)
                {
                    team.addPlayer(ewplayer);
                    team.placeCages();
                    team.tpPlayersToCages();
                    break;
                }
            }

            ewplayer.getPlayer().getInventory().setItem(EggWars.instance.getConfig().getInt("inventory.kit_selection.slot_in_cage"), KitLoader.getInvItem(ewplayer.getPlayer()));
            ewplayer.getPlayer().getInventory().setItem(EggWars.instance.getConfig().getInt("inventory.voting.slot_in_cage"), VoteUtils.getInvItem(ewplayer.getPlayer()));
        }
        else
        {
            ewplayer.getPlayer().getInventory().setItem(EggWars.instance.getConfig().getInt("inventory.kit_selection.slot"), KitLoader.getInvItem(ewplayer.getPlayer()));
            ewplayer.getPlayer().getInventory().setItem(EggWars.instance.getConfig().getInt("inventory.voting.slot"), VoteUtils.getInvItem(ewplayer.getPlayer()));
            ewplayer.getPlayer().getInventory().setItem(EggWars.instance.getConfig().getInt("inventory.team_selection.slot"), TeamUtils.getInvItem(ewplayer.getPlayer()));
        }

        (new BukkitRunnable()
        {
            public void run()
            {
                if (ewplayer.isInArena() && !arenaIn.getStatus().isGame())
                {
                    ewplayer.getPlayer().getInventory().setItem(EggWars.instance.getConfig().getInt("inventory.leave.slot"), ArenaLoader.getLeaveItem(ewplayer.getPlayer()));
                }
            }
        }).runTaskLater(EggWars.instance, 30L);
    }

    public static void playCountDownSound(Arena arenaIn)
    {
        arenaIn.broadcastSound(Sound.UI_BUTTON_CLICK, 1.0F, 2.0F);
    }

    public static void playCountDown(Arena arenaIn, String type, int countdown)
    {
        for (EwPlayer ewplayer : arenaIn.getPlayers())
        {
            TranslationUtils.sendMessage("gameplay.lobby." + type + "_countdown", ewplayer.getPlayer(), TranslationUtils.translateTime(ewplayer.getPlayer(), countdown, true));
        }

        playCountDownSound(arenaIn);
    }
}
