package me.JackHaynie.Karismic.listeners;

import me.JackHaynie.Karismic.EggCatcher;
import me.JackHaynie.Karismic.EggCatcherLogger;
import me.JackHaynie.Karismic.events.EggCaptureEvent;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;

public class EggCatcherEntityListener implements Listener {
    private HashMap<EntityType, Integer> eggIds;
    private final boolean usePermissions;
    private final boolean useCatchChance;
    private final boolean useHealthPercentage;
    private final boolean looseEggOnFail;
    private final boolean useVaultCost;
    private final boolean useItemCost;
    private final boolean explosionEffect;
    private final boolean smokeEffect;
    private final boolean nonPlayerCatching;
    private final boolean preventCatchingBabyAnimals;
    private final boolean preventCatchingTamedAnimals;
    private final boolean preventCatchingShearedSheeps;
    private final String catchChanceSuccessMessage;
    private final String catchChanceFailMessage;
    private final String healthPercentageFailMessage;
    private final String vaultTargetBankAccount;
    private final boolean spawnChickenOnFail;
    private final boolean spawnChickenOnSuccess;
    private final boolean deleteVillagerInventoryOnCatch;
    private final boolean logCaptures;
    private final File captureLogFile;
    private final EggCatcherLogger captureLogger;
    FileConfiguration config;
    JavaPlugin plugin;

    public EggCatcherEntityListener(JavaPlugin plugin) {
        this.config = plugin.getConfig();
        this.plugin = plugin;
        this.usePermissions = this.config.getBoolean("UsePermissions", true);
        this.useCatchChance = this.config.getBoolean("UseCatchChance", true);
        this.useHealthPercentage = this.config.getBoolean("UseHealthPercentage", false);
        this.looseEggOnFail = this.config.getBoolean("LooseEggOnFail", true);
        this.useVaultCost = this.config.getBoolean("UseVaultCost", false);
        this.useItemCost = this.config.getBoolean("UseItemCost", false);
        this.explosionEffect = this.config.getBoolean("ExplosionEffect", true);
        this.smokeEffect = this.config.getBoolean("SmokeEffect", false);
        this.nonPlayerCatching = this.config.getBoolean("NonPlayerCatching", true);
        this.catchChanceSuccessMessage = this.config.getString("Messages.CatchChanceSuccess");
        this.catchChanceFailMessage = this.config.getString("Messages.CatchChanceFail");
        this.healthPercentageFailMessage = this.config.getString("Messages.HealthPercentageFail");
        this.preventCatchingBabyAnimals = this.config.getBoolean("PreventCatchingBabyAnimals", true);
        this.preventCatchingTamedAnimals = this.config.getBoolean("PreventCatchingTamedAnimals", true);
        this.preventCatchingShearedSheeps = this.config.getBoolean("PreventCatchingShearedSheeps", true);
        this.spawnChickenOnFail = this.config.getBoolean("SpawnChickenOnFail", true);
        this.spawnChickenOnSuccess = this.config.getBoolean("SpawnChickenOnSuccess", false);
        this.vaultTargetBankAccount = this.config.getString("VaultTargetBankAccount", "");
        this.deleteVillagerInventoryOnCatch = this.config.getBoolean("DeleteVillagerInventoryOnCatch", false);
        this.logCaptures = this.config.getBoolean("LogEggCaptures", false);
        this.captureLogFile = new File(plugin.getDataFolder(), "captures.txt");
        this.captureLogger = new EggCatcherLogger(this.captureLogFile);
        this.eggIds = new HashMap<>();

        //Loop through the config and create the map of all the mob egg Ids
        for (String entity : config.getConfigurationSection("mobs").getKeys(false)) {
            eggIds.put(EntityType.valueOf(entity.toUpperCase()), config.getInt("mobs." + entity + ".EggId"));
        }
    }

    @SuppressWarnings({"deprecation", "Duplicates"})
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityHitByEgg(EntityDamageEvent event) {
        EntityDamageByEntityEvent damageEvent;
        Egg egg;
        double vaultCost;
        Entity entity = event.getEntity();
        String entityTypeString = event.getEntity().getType().toString();
        String entityFriendlyName = entity.toString().replace("Craft", "");

        //Bukkit.broadcastMessage("friendly name: " + entityFriendlyName);
        //Bukkit.broadcastMessage("custom name: " + event.getEntity().getCustomName());
        Bukkit.broadcastMessage("entity.getEntity().getType().toString(): " + entityTypeString);
        Bukkit.broadcastMessage("" + eggIds.get(entityTypeString));

        //Returns if the event is not a EntityDamageByEntityEvent
        if (!(event instanceof EntityDamageByEntityEvent)) {
            return;
        }

        damageEvent = (EntityDamageByEntityEvent) event;
        //Returns if the damage is not caused by an Egg
        if (!(damageEvent.getDamager() instanceof Egg)) {
            return;
        }

        egg = (Egg) damageEvent.getDamager();
        //Spawns a chicken on failure if enabled
        if (this.spawnChickenOnFail) {
            Chicken babyChicken = entity.getWorld().spawn(entity.getLocation(), Chicken.class);
            babyChicken.setBaby();
        }

        //Prevents catching baby animals if enabled
        if ((this.preventCatchingBabyAnimals) && ((entity instanceof Ageable)) && (!((Ageable) entity).isAdult())) {
            return;
        }

        //Prevents catching tamed animals if enabled
        if ((this.preventCatchingTamedAnimals) && ((entity instanceof Tameable)) && (((Tameable) entity).isTamed())) {
            return;
        }

        //Prevents catching sheared sheep is enabled
        if ((this.preventCatchingShearedSheeps) && ((entity instanceof Sheep)) && (((Sheep) entity).isSheared())) {
            return;
        }

        /*
        NEED TO COMMENT THIS
         */
        EggCaptureEvent eggCaptureEvent = new EggCaptureEvent(entity, egg);
        this.plugin.getServer().getPluginManager().callEvent(eggCaptureEvent);
        if (eggCaptureEvent.isCancelled()) {
            return;
        }

        /*
        NEED TO COMMENT THIS METHOD
         */
        int itemAmount;
        ItemStack itemStack;
        if ((egg.getShooter() instanceof Player)) {
            Player player = (Player) egg.getShooter();
            /*
            Check if usePermissions is enabled in the config, and if the player has permission to catch the
            specified mob.
             */
            if ((this.usePermissions) && (!player.hasPermission("eggcatcher.catch." + entityFriendlyName.toLowerCase()))) {
                player.sendMessage(this.config.getString("Messages.PermissionFail"));
                //If looseEggOnFail is
                if (!this.looseEggOnFail) {
                    player.getInventory().addItem(new ItemStack(Material.EGG, 1));
                }
                return;
            }

            if (this.useHealthPercentage) {

                double healthPercentage = this.config.getDouble("mobs." + entityFriendlyName.toUpperCase() + ".HealthPercentage");

                @SuppressWarnings("deprecation")
                double currentHealth = ((LivingEntity) entity).getHealth() * 100.0D / ((LivingEntity) entity).getMaxHealth();
                if (healthPercentage < currentHealth) {
                    if (this.healthPercentageFailMessage.length() > 0) {
                        player.sendMessage(String.format(this.healthPercentageFailMessage, healthPercentage));
                    }
                    if (!this.looseEggOnFail) {
                        player.getInventory().addItem(new ItemStack(Material.EGG, 1));
                    }
                    return;
                }
            }
            if (this.useCatchChance) {

                double catchChance = this.config.getDouble("mobs." + entityFriendlyName.toUpperCase() + ".CatchChance");
                if (Math.random() * 100.0D <= catchChance) {
                    if (this.catchChanceSuccessMessage.length() > 0) {
                        player.sendMessage(this.catchChanceSuccessMessage);
                    }
                } else {
                    if (this.catchChanceFailMessage.length() > 0) {
                        player.sendMessage(this.catchChanceFailMessage);
                    }
                    if (!this.looseEggOnFail) {
                        player.getInventory().addItem(new ItemStack(Material.EGG, 1));
                    }
                    return;
                }
            }

            if ((this.useVaultCost) && (!player.hasPermission("eggcatcher.free"))) {
                vaultCost = this.config.getDouble("mobs." + entityFriendlyName.toUpperCase() + ".VaultCost");
                if (!EggCatcher.economy.has(player.getName(), vaultCost)) {
                    player.sendMessage(String.format(this.config.getString("Messages.VaultFail"), vaultCost));
                    if (!this.looseEggOnFail) {
                        player.getInventory().addItem(new ItemStack(Material.EGG, 1));
                    }
                    return;
                }
                EggCatcher.economy.withdrawPlayer(player.getName(), vaultCost);
                if (!this.vaultTargetBankAccount.isEmpty()) {
                    EggCatcher.economy.bankDeposit(this.vaultTargetBankAccount, vaultCost);
                }
                player.sendMessage(String.format(this.config.getString("Messages.VaultSuccess"), vaultCost));
            }


            if ((this.useItemCost) && (!player.hasPermission("eggcatcher.free"))) {
                String itemName = this.config.getString("mobs." + entityFriendlyName.toUpperCase() + ".ItemCost.ItemName", "GOLD_NUGGET");
                itemAmount = this.config.getInt("mobs." + entityFriendlyName.toUpperCase() + ".ItemCost.Amount", 0);

                itemStack = new ItemStack(Material.valueOf(itemName), itemAmount);
                if (player.getInventory().containsAtLeast(itemStack, itemStack.getAmount())) {
                    player.sendMessage(String.format(this.config.getString("Messages.ItemCostSuccess"), itemAmount));
                    player.getInventory().removeItem(itemStack);
                } else {
                    player.sendMessage(String.format(this.config.getString("Messages.ItemCostFail"), itemAmount));
                    if (!this.looseEggOnFail) {
                        player.getInventory().addItem(new ItemStack(Material.EGG, 1));
                    }
                    return;
                }
            }

        } else {
            if (!this.nonPlayerCatching) {
                return;
            }
            if (this.useCatchChance) {
                double catchChance = this.config.getDouble("mobs." + entityFriendlyName.toUpperCase() + ".CatchChance");
                if (Math.random() * 100.0D > catchChance) {
                    return;
                }
            }
        }

        //Remove the entity and play the smoke/explosion effect if enabled
        entity.remove();
        if (this.explosionEffect) {
            entity.getWorld().createExplosion(entity.getLocation(), 0.0F);
        }
        if (this.smokeEffect) {
            entity.getWorld().playEffect(entity.getLocation(), Effect.SMOKE, 0);
        }

        //CREATE THE EGG
        ItemStack eggStack = new ItemStack(Material.MONSTER_EGG, 1, Short.parseShort(String.valueOf(EntityType.fromName(event.getEntity().getType().toString()))));
        //EntityType.fromName(entity.getType())
        //eggIds.get(entity.getType())))

        //If the entity has a custom name,
        if (entity.getCustomName() != null) {
            ItemMeta meta = eggStack.getItemMeta();
            meta.setDisplayName(entity.getCustomName());
            eggStack.setItemMeta(meta);
        }

        //If the entity is a pig and has a saddle, drop the saddle
        if (((entity instanceof Pig)) && (((Pig) entity).hasSaddle())) {
            entity.getWorld().dropItem(entity.getLocation(), new ItemStack(Material.SADDLE, 1));
        }

        //If the entity is a horse and carrying a chest, it drops the chest
        if (((entity instanceof Horse)) && (((Horse) entity).isCarryingChest())) {
            entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.CHEST));
        }

        //If the entity is a villager ... DOES SOMETHING NEED TO FILL OUT
        if ((((entity instanceof Villager)) && (!this.deleteVillagerInventoryOnCatch)) || ((!(entity instanceof Villager))
                && ((entity instanceof InventoryHolder)))) {
            //Get the items from the villagers inventory and drop the items
            ItemStack[] items = ((InventoryHolder) entity).getInventory().getContents();
            for (ItemStack item : items) {
                if (item != null) {
                    entity.getWorld().dropItemNaturally(entity.getLocation(), item);
                }
            }
        }

        //Drop the egg at the entities location
        entity.getWorld().dropItem(entity.getLocation(), eggStack);

        //Spawns a chicken on successful egg capture if enabled
        if (this.spawnChickenOnSuccess) {
            Chicken babyChicken = entity.getWorld().spawn(entity.getLocation(), Chicken.class);
            babyChicken.setBaby();
        }

        /*
        If logging is enabled, every time a player catches a mob information will be logged to a file in this format.
        "Player %player% caught %entity% at X %x% , Y %y%, Z %z% in world %world%"
         */
        if (this.logCaptures) {
            this.captureLogger.logToFile("Player " + ((Player) egg.getShooter()).getName() + " caught " + entity.getType() + " at X" + Math.round(entity.getLocation().getX()) + ",Y" + Math.round(entity.getLocation().getY()) + ",Z" + Math.round(entity.getLocation().getZ()) + " in world " + entity.getWorld().getName());
        }

    }
}
