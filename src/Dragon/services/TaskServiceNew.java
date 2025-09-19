package Dragon.services;

import Dragon.jdbc.daos.TaskCache;
import Dragon.models.boss.Boss;
import Dragon.models.item.Item;
import Dragon.models.map.ItemMap;
import Dragon.models.map.Zone;
import Dragon.models.mob.Mob;
import Dragon.models.npc.Npc;
import Dragon.models.player.Player;
import Dragon.utils.Logger;

import java.util.List;

/**
 * New SQL-based Task Service
 */
public class TaskServiceNew {

    private static TaskServiceNew instance;
    private TaskCache cache;

    public static TaskServiceNew getInstance() {
        if (instance == null) {
            instance = new TaskServiceNew();
        }
        return instance;
    }

    private TaskServiceNew() {
        this.cache = TaskCache.getInstance();
    }

    /**
     * Check task completion when killing mob
     */
    public void checkDoneTaskKillMob(Player player, Mob mob) {
        if (!player.isBoss && !player.isPet && !player.isClone) {
            Logger.log("TaskServiceNew: Player " + player.name + " killed mob " + mob.tempId +
                    " at map " + mob.zone.map.mapId);

            int currentTaskId = getCurrentTaskId(player);
            if (currentTaskId == -1)
                return;

            int taskMainId = getTaskMainId(currentTaskId);
            int taskSubId = getTaskSubId(currentTaskId);

            List<TaskCache.TaskRequirement> requirements = cache.getTaskRequirements(taskMainId, taskSubId, "KILL_MOB");

            for (TaskCache.TaskRequirement req : requirements) {
                if (req.targetId == mob.tempId && checkMapRestriction(req.mapRestriction, mob.zone.map.mapId)) {
                    Logger.log("TaskServiceNew: Task requirement matched - " + req.toString());
                    incrementTaskProgress(player, req, 1);
                }
            }
        }
    }

    /**
     * Check task completion when killing boss
     */
    public void checkDoneTaskKillBoss(Player player, Boss boss) {
        if (player != null && !player.isBoss && !player.isPet && !player.isClone) {
            Logger.log("TaskServiceNew: Player " + player.name + " killed boss " + boss.id);

            int currentTaskId = getCurrentTaskId(player);
            if (currentTaskId == -1)
                return;

            int taskMainId = getTaskMainId(currentTaskId);
            int taskSubId = getTaskSubId(currentTaskId);

            List<TaskCache.TaskRequirement> requirements = cache.getTaskRequirements(taskMainId, taskSubId,
                    "KILL_BOSS");

            for (TaskCache.TaskRequirement req : requirements) {
                if (req.targetId == (int) boss.id) {
                    Logger.log("TaskServiceNew: Boss task requirement matched - " + req.toString());
                    incrementTaskProgress(player, req, 1);
                }
            }
        }
    }

    /**
     * Check task completion when talking to NPC
     */
    public boolean checkDoneTaskTalkNpc(Player player, Npc npc) {
        Logger.log("TaskServiceNew: Player " + player.name + " talked to NPC " + npc.tempId +
                " at map " + npc.mapId);

        int currentTaskId = getCurrentTaskId(player);
        if (currentTaskId == -1)
            return false;

        int taskMainId = getTaskMainId(currentTaskId);
        int taskSubId = getTaskSubId(currentTaskId);

        List<TaskCache.TaskRequirement> requirements = cache.getTaskRequirements(taskMainId, taskSubId, "TALK_NPC");

        for (TaskCache.TaskRequirement req : requirements) {
            if (req.targetId == npc.tempId && checkMapRestriction(req.mapRestriction, npc.mapId)) {
                Logger.log("TaskServiceNew: NPC task requirement matched - " + req.toString());
                return incrementTaskProgress(player, req, 1);
            }
        }

        return false;
    }

    /**
     * Check task completion when picking item
     */
    public void checkDoneTaskPickItem(Player player, ItemMap item) {
        if (!player.isBoss && !player.isPet && !player.isClone && item != null) {
            Logger.log("TaskServiceNew: Player " + player.name + " picked item " + item.itemTemplate.id);

            int currentTaskId = getCurrentTaskId(player);
            if (currentTaskId == -1)
                return;

            int taskMainId = getTaskMainId(currentTaskId);
            int taskSubId = getTaskSubId(currentTaskId);

            List<TaskCache.TaskRequirement> requirements = cache.getTaskRequirements(taskMainId, taskSubId,
                    "PICK_ITEM");

            for (TaskCache.TaskRequirement req : requirements) {
                if (req.targetId == item.itemTemplate.id) {
                    Logger.log("TaskServiceNew: Item task requirement matched - " + req.toString());
                    incrementTaskProgress(player, req, 1);
                }
            }
        }
    }

    /**
     * Check task completion when going to map
     */
    public void checkDoneTaskGoToMap(Player player, Zone zoneJoin) {
        if (player.isPl() && !player.isBot) {
            Logger.log("TaskServiceNew: Player " + player.name + " entered map " + zoneJoin.map.mapId);

            int currentTaskId = getCurrentTaskId(player);
            if (currentTaskId == -1)
                return;

            int taskMainId = getTaskMainId(currentTaskId);
            int taskSubId = getTaskSubId(currentTaskId);

            List<TaskCache.TaskRequirement> requirements = cache.getTaskRequirements(taskMainId, taskSubId,
                    "GO_TO_MAP");

            for (TaskCache.TaskRequirement req : requirements) {
                if (req.targetId == zoneJoin.map.mapId) {
                    Logger.log("TaskServiceNew: Map task requirement matched - " + req.toString());
                    incrementTaskProgress(player, req, 1);
                }
            }
        }
    }

    /**
     * Check map restriction
     */
    private boolean checkMapRestriction(String mapRestriction, int mapId) {
        if (mapRestriction == null || mapRestriction.isEmpty()) {
            return true; // No restriction
        }

        try {
            String restriction = mapRestriction.trim();

            // Handle exclusion (!)
            if (restriction.startsWith("!")) {
                return !checkMapInRange(restriction.substring(1), mapId);
            }

            // Handle inclusion
            return checkMapInRange(restriction, mapId);

        } catch (Exception e) {
            Logger.logException(TaskServiceNew.class, e);
            return false; // Safe fallback
        }
    }

    private boolean checkMapInRange(String rangeStr, int mapId) {
        String[] ranges = rangeStr.split(",");

        for (String range : ranges) {
            range = range.trim();

            if (range.contains("-")) {
                String[] parts = range.split("-");
                if (parts.length == 2) {
                    int start = Integer.parseInt(parts[0].trim());
                    int end = Integer.parseInt(parts[1].trim());
                    if (mapId >= start && mapId <= end) {
                        return true;
                    }
                }
            } else {
                int singleMap = Integer.parseInt(range);
                if (mapId == singleMap) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Increment task progress
     */
    private boolean incrementTaskProgress(Player player, TaskCache.TaskRequirement req, int amount) {
        // Get current progress from player data
        int currentProgress = getCurrentTaskProgress(player, req);
        int newProgress = currentProgress + amount;

        Logger.log("TaskServiceNew: Task progress " + req.taskMainId + "_" + req.taskSubId +
                ": " + currentProgress + " + " + amount + " = " + newProgress + "/" + req.targetCount);

        // Update progress (cần implement với player task system hiện tại)
        setCurrentTaskProgress(player, req, newProgress);

        // Check if completed
        if (newProgress >= req.targetCount) {
            Logger.log("TaskServiceNew: Task completed! " + req.toString());
            completeTask(player, req.taskMainId, req.taskSubId);
            return true;
        }

        return false;
    }

    /**
     * Complete task and give rewards
     */
    private void completeTask(Player player, int taskMainId, int taskSubId) {
        Logger.log("TaskServiceNew: Completing task " + taskMainId + "_" + taskSubId + " for player " + player.name);

        // Give rewards
        List<TaskCache.TaskReward> rewards = cache.getTaskRewards(taskMainId, taskSubId);
        for (TaskCache.TaskReward reward : rewards) {
            giveRewardToPlayer(player, reward);
        }

        // Move to next task (integrate với TaskService hiện tại)
        player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).count += 1;
        TaskService.gI().sendInfoCurrentTask(player);
    }

    /**
     * Give reward to player
     */
    private void giveRewardToPlayer(Player player, TaskCache.TaskReward reward) {
        Logger.log("TaskServiceNew: Giving reward " + reward.toString() + " to player " + player.name);

        switch (reward.rewardType) {
            case "ITEM":
                Item item = ItemService.gI().createNewItem((short) reward.rewardId, (int) reward.rewardQuantity);
                InventoryServiceNew.gI().addItemBag(player, item);
                InventoryServiceNew.gI().sendItemBags(player);
                break;

            case "GOLD":
                player.inventory.gold = Math.min(player.inventory.gold + reward.rewardQuantity,
                        Dragon.models.player.Inventory.LIMIT_GOLD);
                Service.gI().sendMoney(player);
                break;

            case "EXP":
                Service.gI().addSMTN(player, (byte) 1, reward.rewardQuantity, true);
                break;

            case "RUBY":
                player.inventory.ruby = Math.min(player.inventory.ruby + (int) reward.rewardQuantity, 2000000000);
                Service.gI().sendMoney(player);
                break;
        }

        if (reward.rewardDescription != null && !reward.rewardDescription.isEmpty()) {
            Service.gI().sendThongBao(player, reward.rewardDescription);
        }
    }

    // Helper methods (cần integrate với TaskService hiện tại)
    private int getCurrentTaskId(Player player) {
        return TaskService.gI().getIdTask(player);
    }

    private int getTaskMainId(int taskId) {
        return taskId >> 10;
    }

    private int getTaskSubId(int taskId) {
        return (taskId >> 1) & 0x1FF;
    }

    private int getCurrentTaskProgress(Player player, TaskCache.TaskRequirement req) {
        // TODO: Implement với player task progress system
        return 0;
    }

    private void setCurrentTaskProgress(Player player, TaskCache.TaskRequirement req, int progress) {
        // TODO: Implement với player task progress system
    }

    /**
     * Refresh cache
     */
    public void refreshCache() {
        cache.refreshCache();
    }

    /**
     * Get cache stats
     */
    public String getCacheStats() {
        return cache.getCacheStats();
    }
}
