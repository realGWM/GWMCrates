package org.gwmdevelopments.sponge_plugin.crates.util;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.gwmdevelopments.sponge_plugin.crates.GWMCrates;
import org.gwmdevelopments.sponge_plugin.crates.drop.Drop;
import org.gwmdevelopments.sponge_plugin.crates.drop.drops.CommandsDrop;
import org.gwmdevelopments.sponge_plugin.crates.drop.drops.EmptyDrop;
import org.gwmdevelopments.sponge_plugin.crates.exception.SSOCreationException;
import org.gwmdevelopments.sponge_plugin.crates.manager.Manager;
import org.gwmdevelopments.sponge_plugin.library.utils.Pair;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.*;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.item.inventory.type.OrderedInventory;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.sql.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class GWMCratesUtils {

    private GWMCratesUtils() {
    }

    public static final ItemStack EMPTY_ITEM = ItemStack.of(ItemTypes.NONE, 0);
    public static final Drop EMPTY_DROP = new EmptyDrop();
    public static final Pattern ID_PATTERN = Pattern.compile("[a-z]([-_]?[a-z0-9])*");

    public static void loadManager(File file, boolean force) {
        try {
            ConfigurationLoader<CommentedConfigurationNode> managerConfigurationLoader =
                    HoconConfigurationLoader.builder().setFile(file).build();
            ConfigurationNode managerNode = managerConfigurationLoader.load();
            if (force || managerNode.getNode("LOAD").getBoolean(true)) {
                Manager manager = new Manager(managerNode);
                for (Manager createdManager : GWMCrates.getInstance().getCreatedManagers()) {
                    if (manager.getId().equals(createdManager.getId())) {
                        GWMCrates.getInstance().getLogger().warn("Manager from file \"" + GWMCratesUtils.getManagerRelativePath(file) + "\" is not loaded because its ID is not unique!");
                        return;
                    }
                }
                GWMCrates.getInstance().getCreatedManagers().add(manager);
                GWMCrates.getInstance().getLogger().info("Manager \"" + manager.getId() + "\" (\"" + manager.getName() + "\") from file \"" + GWMCratesUtils.getManagerRelativePath(file) + "\" successfully loaded!");
            } else {
                GWMCrates.getInstance().getLogger().info("Skipping manager from file \"" + GWMCratesUtils.getManagerRelativePath(file) + "\"!");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<Currency> getCurrencyById(EconomyService economyService, String id) {
        for (Currency currency : economyService.getCurrencies()) {
            if (currency.getId().equals(id)) {
                return Optional.of(currency);
            }
        }
        return Optional.empty();
    }

    public static void asyncImportToMySQL() {
        new Thread(() -> {
            ConsoleSource console = Sponge.getServer().getConsole();
            try {
                final long time = importToMySQL();
                Sponge.getScheduler().createTaskBuilder().execute(() ->
                        console.sendMessage(GWMCrates.getInstance().getLanguage().
                                getText("IMPORT_TO_MYSQL_SUCCESSFUL", console, null,
                                        new Pair<>("%TIME%", millisToString(time))))).
                        submit(GWMCrates.getInstance());
            } catch (SQLException e) {
                Sponge.getScheduler().createTaskBuilder().execute(() ->
                        console.sendMessage(GWMCrates.getInstance().getLanguage().
                                getText("IMPORT_TO_MYSQL_FAILED", console, null)))
                        .submit(GWMCrates.getInstance());
                GWMCrates.getInstance().getLogger().warn("Async import to MySQL failed!", e);
            }
        }).start();
    }

    public static long importToMySQL() throws SQLException {
        long start = System.currentTimeMillis();
        Connection connection = GWMCrates.getInstance().getDataSource().get().getConnection();
        connection.setAutoCommit(false);
        PreparedStatement virtualCasesSelectStatement =
                connection.prepareStatement("SELECT value FROM virtual_cases " +
                        "WHERE uuid = ? " +
                        "AND name = ?");
        PreparedStatement virtualCasesUpdateStatement =
                connection.prepareStatement("UPDATE virtual_cases " +
                        "SET value = ? " +
                        "WHERE uuid = ? " +
                        "AND name = ?");
        PreparedStatement virtualCasesInsertStatement =
                connection.prepareStatement("INSERT INTO virtual_cases (uuid, name, value) " +
                        "VALUES (?, ?, ?)");
        for (Map.Entry<Object, ? extends ConfigurationNode> virtualCasesEntry :
                GWMCrates.getInstance().getVirtualCasesConfig().getNode().getChildrenMap().entrySet()) {
            String uuid = virtualCasesEntry.getKey().toString();
            for (Map.Entry<Object, ? extends ConfigurationNode> valueEntry :
                    virtualCasesEntry.getValue().getChildrenMap().entrySet()) {
                String name = valueEntry.getKey().toString();
                int value = valueEntry.getValue().getInt();
                virtualCasesSelectStatement.setString(1, uuid);
                virtualCasesSelectStatement.setString(2, name);
                if (virtualCasesSelectStatement.executeQuery().next()) {
                    virtualCasesUpdateStatement.setInt(1, value);
                    virtualCasesUpdateStatement.setString(2, uuid);
                    virtualCasesUpdateStatement.setString(3, name);
                    virtualCasesUpdateStatement.executeUpdate();
                } else {
                    virtualCasesInsertStatement.setString(1, uuid);
                    virtualCasesInsertStatement.setString(2, name);
                    virtualCasesInsertStatement.setInt(3, value);
                    virtualCasesInsertStatement.executeUpdate();
                }
            }
        }
        PreparedStatement virtualKeysSelectStatement =
                connection.prepareStatement("SELECT value FROM virtual_keys " +
                        "WHERE uuid = ? " +
                        "AND name = ?");
        PreparedStatement virtualKeysUpdateStatement =
                connection.prepareStatement("UPDATE virtual_keys " +
                        "SET value = ? " +
                        "WHERE uuid = ? " +
                        "AND name = ?");
        PreparedStatement virtualKeysInsertStatement =
                connection.prepareStatement("INSERT INTO virtual_keys (uuid, name, value) " +
                        "VALUES (?, ?, ?)");
        for (Map.Entry<Object, ? extends ConfigurationNode> virtualKeysEntry :
                GWMCrates.getInstance().getVirtualKeysConfig().getNode().getChildrenMap().entrySet()) {
            String uuid = virtualKeysEntry.getKey().toString();
            for (Map.Entry<Object, ? extends ConfigurationNode> valueEntry :
                    virtualKeysEntry.getValue().getChildrenMap().entrySet()) {
                String name = valueEntry.getKey().toString();
                int value = valueEntry.getValue().getInt();
                virtualKeysSelectStatement.setString(1, uuid);
                virtualKeysSelectStatement.setString(2, name);
                if (virtualKeysSelectStatement.executeQuery().next()) {
                    virtualKeysUpdateStatement.setInt(1, value);
                    virtualKeysUpdateStatement.setString(2, uuid);
                    virtualKeysUpdateStatement.setString(3, name);
                    virtualKeysUpdateStatement.executeUpdate();
                } else {
                    virtualKeysInsertStatement.setString(1, uuid);
                    virtualKeysInsertStatement.setString(2, name);
                    virtualKeysInsertStatement.setInt(3, value);
                    virtualKeysInsertStatement.executeUpdate();
                }
            }
        }
        PreparedStatement timedCasesSelectStatement =
                connection.prepareStatement("SELECT delay FROM timed_cases " +
                        "WHERE uuid = ? " +
                        "AND name = ?");
        PreparedStatement timedCasesUpdateStatement =
                connection.prepareStatement("UPDATE timed_cases " +
                        "SET delay = ? " +
                        "WHERE uuid = ? " +
                        "AND name = ?");
        PreparedStatement timedCasesInsertStatement =
                connection.prepareStatement("INSERT INTO timed_cases (uuid, name, delay) " +
                        "VALUES (?, ?, ?)");
        for (Map.Entry<Object, ? extends ConfigurationNode> timedCasesEntry :
                GWMCrates.getInstance().getTimedCasesConfig().getNode().getChildrenMap().entrySet()) {
            String uuid = timedCasesEntry.getKey().toString();
            for (Map.Entry<Object, ? extends ConfigurationNode> valueEntry :
                    timedCasesEntry.getValue().getChildrenMap().entrySet()) {
                String name = valueEntry.getKey().toString();
                long delay = valueEntry.getValue().getLong();
                timedCasesSelectStatement.setString(1, uuid);
                timedCasesSelectStatement.setString(2, name);
                if (timedCasesSelectStatement.executeQuery().next()) {
                    timedCasesUpdateStatement.setLong(1, delay);
                    timedCasesUpdateStatement.setString(2, uuid);
                    timedCasesUpdateStatement.setString(3, name);
                    timedCasesUpdateStatement.executeUpdate();
                } else {
                    timedCasesInsertStatement.setString(1, uuid);
                    timedCasesInsertStatement.setString(2, name);
                    timedCasesInsertStatement.setLong(3, delay);
                    timedCasesInsertStatement.executeUpdate();
                }
            }
        }
        PreparedStatement timedKeysSelectStatement =
                connection.prepareStatement("SELECT delay FROM timed_keys " +
                        "WHERE uuid = ? " +
                        "AND name = ?");
        PreparedStatement timedKeysUpdateStatement =
                connection.prepareStatement("UPDATE timed_keys " +
                        "SET delay = ? " +
                        "WHERE uuid = ? " +
                        "AND name = ?");
        PreparedStatement timedKeysInsertStatement =
                connection.prepareStatement("INSERT INTO timed_keys (uuid, name, delay) " +
                        "VALUES (?, ?, ?)");
        for (Map.Entry<Object, ? extends ConfigurationNode> timedKeysEntry :
                GWMCrates.getInstance().getTimedKeysConfig().getNode().getChildrenMap().entrySet()) {
            String uuid = timedKeysEntry.getKey().toString();
            for (Map.Entry<Object, ? extends ConfigurationNode> valueEntry :
                    timedKeysEntry.getValue().getChildrenMap().entrySet()) {
                String name = valueEntry.getKey().toString();
                long delay = valueEntry.getValue().getLong();
                timedKeysSelectStatement.setString(1, uuid);
                timedKeysSelectStatement.setString(2, name);
                if (timedKeysSelectStatement.executeQuery().next()) {
                    timedKeysUpdateStatement.setLong(1, delay);
                    timedKeysUpdateStatement.setString(2, uuid);
                    timedKeysUpdateStatement.setString(3, name);
                    timedKeysUpdateStatement.executeUpdate();
                } else {
                    timedKeysInsertStatement.setString(1, uuid);
                    timedKeysInsertStatement.setString(2, name);
                    timedKeysInsertStatement.setLong(3, delay);
                    timedKeysInsertStatement.executeUpdate();
                }
            }
        }
        connection.commit();
        virtualCasesSelectStatement.close();
        virtualCasesUpdateStatement.close();
        virtualCasesInsertStatement.close();
        virtualKeysSelectStatement.close();
        virtualKeysUpdateStatement.close();
        virtualKeysInsertStatement.close();
        timedCasesSelectStatement.close();
        timedCasesUpdateStatement.close();
        timedCasesInsertStatement.close();
        timedKeysSelectStatement.close();
        timedKeysUpdateStatement.close();
        timedKeysInsertStatement.close();
        connection.close();
        return System.currentTimeMillis() - start;
    }

    public static void asyncImportFromMySQL() {
        new Thread(() -> {
            ConsoleSource console = Sponge.getServer().getConsole();
            try {
                final long time = importFromMySQL();
                Sponge.getScheduler().createTaskBuilder().execute(() ->
                        console.sendMessage(GWMCrates.getInstance().getLanguage().
                                getText("IMPORT_FROM_MYSQL_SUCCESSFUL", console, null,
                                        new Pair<>("%TIME%", millisToString(time))))).
                        submit(GWMCrates.getInstance());
            } catch (SQLException e) {
                Sponge.getScheduler().createTaskBuilder().execute(() ->
                        console.sendMessage(GWMCrates.getInstance().getLanguage().
                                getText("IMPORT_FROM_MYSQL_FAILED", console, null)))
                        .submit(GWMCrates.getInstance());
                GWMCrates.getInstance().getLogger().warn("Async import from MySQL failed!", e);
            }
        }).start();
    }

    public static long importFromMySQL() throws SQLException {
        long start = System.currentTimeMillis();
        Connection connection = GWMCrates.getInstance().getDataSource().get().getConnection();
        Statement statement = connection.createStatement();
        ResultSet virtualCases = statement.executeQuery("SELECT uuid, name, value FROM virtual_cases;");
        while (virtualCases.next()) {
            GWMCrates.getInstance().getVirtualCasesConfig().
                    getNode(virtualCases.getString(1), virtualCases.getString(2)).
                    setValue(virtualCases.getInt(3));
        }
        ResultSet virtualKeys = statement.executeQuery("SELECT uuid, name, value FROM virtual_keys;");
        while (virtualKeys.next()) {
            GWMCrates.getInstance().getVirtualKeysConfig().
                    getNode(virtualKeys.getString(1), virtualKeys.getString(2)).
                    setValue(virtualKeys.getInt(3));
        }
        ResultSet timedCases = statement.executeQuery("SELECT uuid, name, delay FROM timed_cases;");
        while (timedCases.next()) {
            GWMCrates.getInstance().getVirtualCasesConfig().
                    getNode(timedCases.getString(1), timedCases.getString(2)).
                    setValue(timedCases.getLong(3));
        }
        ResultSet timedKeys = statement.executeQuery("SELECT uuid, name, delay FROM timed_keys;");
        while (timedKeys.next()) {
            GWMCrates.getInstance().getTimedKeysConfig().
                    getNode(timedKeys.getString(1), timedKeys.getString(2)).
                    setValue(timedKeys.getLong(3));
        }
        statement.close();
        connection.close();
        GWMCrates.getInstance().getVirtualCasesConfig().save();
        GWMCrates.getInstance().getVirtualKeysConfig().save();
        GWMCrates.getInstance().getTimedCasesConfig().save();
        GWMCrates.getInstance().getTimedKeysConfig().save();
        return System.currentTimeMillis() - start;
    }

    public static String millisToString(long time) {
        return time / 1000 + "." + time % 1000;
    }
    
    public static long getCrateOpenDelay(UUID uuid) {
        if (GWMCrates.getInstance().getCrateOpenDelays().containsKey(uuid)) {
            long current = System.currentTimeMillis();
            long delay = GWMCrates.getInstance().getCrateOpenDelays().get(uuid);
            if (delay > current) {
                return delay - current;
            } else {
                GWMCrates.getInstance().getCrateOpenDelays().remove(uuid);
                return 0L;
            }
        } else {
            return 0L;
        }
    }

    public static void updateCrateOpenDelay(UUID uuid) {
        GWMCrates.getInstance().getCrateOpenDelays().put(uuid, System.currentTimeMillis() + GWMCrates.getInstance().getCrateOpenDelay());
    }

    public static CommandsDrop.ExecutableCommand parseCommand(ConfigurationNode node) {
        ConfigurationNode commandNode = node.getNode("COMMAND");
        { //Backward compatibility
            if (commandNode.isVirtual()) {
                GWMCrates.getInstance().getLogger().warn("[BACKWARD COMPATIBILITY] COMMAND node does not exist! Trying to use CMD node!");
                commandNode = node.getNode("CMD");
            }
        }
        ConfigurationNode consoleNode = node.getNode("CONSOLE");
        if (commandNode.isVirtual()) {
            throw new IllegalArgumentException("COMMAND node does not exist!");
        }
        String command = commandNode.getString();
        breakpoint:
        { //Backward compatibility
            //What the hell is this? How much did I drink?
            //This is always false, so I think it's safe to remove this code.
            //But maybe one day I will remember what it was here for, so I'll just comment it out for now.
            //Never, just NEVER write code while you're drunk... It's a really bad idea...
            //
            //if (!command.contains("")) {
            //    break breakpoint;
            //}
            String[] splited = command.split(" ");
            if (splited.length < 5) {
                break breakpoint;
            }
            String cmd = splited[0].toLowerCase();
            String give = splited[1].toLowerCase();
            String player = splited[2];
            String type = splited[3].toLowerCase();
            String id = splited[4];
            String amount = splited.length > 5 ? splited[5] : "1";
            if (give.equals("give") &&
                    (cmd.equals("gwmcrates") || cmd.equals("gwmcrate") || cmd.equals("crates") || cmd.equals("crate")) &&
                    (type.equals("case")) || type.equals("key") || type.equals("drop")) {
                String newCommand = "gwmcrates give " + type + " " + id + " " + player + " " + amount;
                GWMCrates.getInstance().getLogger().warn("[BACKWARD COMPATIBILITY] Replacing command \"" + command + "\" by \"" + newCommand + "\"!");
                command = newCommand;
            }
        }
        boolean console = consoleNode.getBoolean(true);
        return new CommandsDrop.ExecutableCommand(command, console);
    }

    public static boolean isFirstInventory(Container container, Slot slot) {
        int upperSize = container.iterator().next().capacity();
        Integer affectedSlot = slot.getProperty(SlotIndex.class, "slotindex").map(SlotIndex::getValue).orElse(-1);
        return affectedSlot != -1 && affectedSlot < upperSize;
    }

    public static int getInventoryHeight(int size) {
        int height = (int) Math.ceil((size)/9.);
        if (height < 1) {
            return 1;
        }
        if (height > 6) {
            return 6;
        }
        return height;
    }

    public static boolean isLocationChanged(Transform<World> from, Transform<World> to, boolean ySensitive) {
        Location<World> locationFrom = from.getLocation();
        Location<World> locationTo = to.getLocation();
        int xFrom = locationFrom.getBlockX();
        int yFrom = locationFrom.getBlockY();
        int zFrom = locationFrom.getBlockZ();
        int xTo = locationTo.getBlockX();
        int yTo = locationTo.getBlockY();
        int zTo = locationTo.getBlockZ();
        return xFrom != xTo || zFrom != zTo || (ySensitive && yFrom != yTo);
    }

    public static void addItemStack(Player player, ItemStack item, int amount) {
        int maxStackQuantity = item.getMaxStackQuantity();
        Iterator<Slot> slotIterator = ((PlayerInventory) player.getInventory()).getMain().<Slot>slots().iterator();
        while (slotIterator.hasNext() && amount > 0) {
            Slot slot = slotIterator.next();
            Optional<ItemStack> optionalInventoryItem = slot.peek();
            if (optionalInventoryItem.isPresent()) {
                ItemStack inventoryItem = optionalInventoryItem.get();
                if (ItemStackComparators.IGNORE_SIZE.compare(inventoryItem, item) == 0) {
                    int inventoryItemQuantity = inventoryItem.getQuantity();
                    if (inventoryItemQuantity < maxStackQuantity) {
                        int difference = maxStackQuantity - inventoryItemQuantity;
                        if (amount >= difference) {
                            inventoryItem.setQuantity(maxStackQuantity);
                            slot.set(inventoryItem);
                            amount -= difference;
                        } else {
                            inventoryItem.setQuantity(inventoryItemQuantity + amount);
                            slot.set(inventoryItem);
                            amount = 0;
                        }
                    }
                }
            } else {
                if (amount >= maxStackQuantity) {
                    ItemStack copy = item.copy();
                    copy.setQuantity(maxStackQuantity);
                    slot.set(copy);
                    amount -= maxStackQuantity;
                } else {
                    ItemStack copy = item.copy();
                    copy.setQuantity(amount);
                    slot.set(copy);
                    amount = 0;
                }
            }
        }
        if (amount > 0) {
            ItemStack copy = item.copy();
            copy.setQuantity(amount);
            Location<World> playerLocation = player.getLocation();
            World world = playerLocation.getExtent();
            Entity entity = world.createEntity(EntityTypes.ITEM, playerLocation.getPosition());
            world.spawnEntity(entity);
            entity.offer(Keys.REPRESENTED_ITEM, copy.createSnapshot());
        }
    }

    public static void removeItemStack(Player player, ItemStack item, int amount) {
        Inventory inventory = player.getInventory();
        Iterator<Slot> slotIterator = inventory.<Slot>slots().iterator();
        while (slotIterator.hasNext() && amount > 0) {
            Slot slot = slotIterator.next();
            Optional<ItemStack> optionalInventoryItem = slot.peek();
            if (optionalInventoryItem.isPresent()) {
                ItemStack inventoryItem = optionalInventoryItem.get();
                if (ItemStackComparators.IGNORE_SIZE.compare(inventoryItem, item) == 0) {
                    int itemQuantity = inventoryItem.getQuantity();
                    if (itemQuantity > amount) {
                        item.setQuantity(itemQuantity - amount);
                        slot.set(item);
                        amount = 0;
                    } else {
                        slot.set(ItemStack.of(ItemTypes.NONE, 1));
                        amount -= itemQuantity;
                    }
                }
            }
        }
    }

    public static int getItemStackAmount(Player player, ItemStack item) {
        int amount = 0;
        Inventory inventory = player.getInventory();
        for (Slot slot : inventory.<Slot>slots()) {
            Optional<ItemStack> optionalInventoryItem = slot.peek();
            if (optionalInventoryItem.isPresent()) {
                ItemStack inventoryItem = optionalInventoryItem.get();
                if (ItemStackComparators.IGNORE_SIZE.compare(inventoryItem, item) == 0) {
                    amount += inventoryItem.getQuantity();
                }
            }
        }
        return amount;
    }

    public static SuperObject createSuperObject(ConfigurationNode node, SuperObjectType superObjectType) {
        ConfigurationNode typeNode = node.getNode("TYPE");
        ConfigurationNode idNode = node.getNode("ID");
        if (typeNode.isVirtual()) {
            throw new IllegalArgumentException("TYPE node does not exist!");
        }
        String type = typeNode.getString();
        String id = idNode.isVirtual() ? "Unknown ID" : idNode.getString().toLowerCase().replace(' ', '_');
        if (type.equals("SAVED")) {
            ConfigurationNode savedIdNode = node.getNode("SAVED_ID");
            if (savedIdNode.isVirtual()) {
                throw new IllegalArgumentException("SAVED_ID node does not exist for Super Object \"" + superObjectType + "\" with type \"" + type + "\" and ID \"" + id + "\"!");
            }
            String savedId = savedIdNode.getString();
            Optional<SuperObject> savedSuperObject = getSavedSuperObject(superObjectType, savedId);
            if (!savedSuperObject.isPresent()) {
                throw new IllegalArgumentException("Saved Super Object \"" + superObjectType + "\" with ID \"" + savedId + "\" does not found!");
            }
            return savedSuperObject.get();
        }
        Optional<SuperObjectStorage> optionalSuperObjectStorage = getSuperObjectStorage(superObjectType, type);
        if (!optionalSuperObjectStorage.isPresent()) {
            throw new IllegalArgumentException("Type \"" + type + "\" for Super Object \"" + superObjectType + "\" does not found!");
        }
        SuperObjectStorage superObjectStorage = optionalSuperObjectStorage.get();
        try {
            Class<? extends SuperObject> superObjectClass = superObjectStorage.getSuperObjectClass();
            Constructor<? extends SuperObject> superObjectConstructor = superObjectClass.getConstructor(ConfigurationNode.class);
            return superObjectConstructor.newInstance(node);
        } catch (Exception e) {
            throw new SSOCreationException(superObjectType, type, e);
        }
    }

    public static Optional<Manager> getManager(String managerId) {
        for (Manager manager : GWMCrates.getInstance().getCreatedManagers()) {
            if (manager.getId().equalsIgnoreCase(managerId)) {
                return Optional.of(manager);
            }
        }
        return Optional.empty();
    }

    public static Optional<SuperObject> getSavedSuperObject(String savedSuperObjectId) {
        for (SuperObject superObject : GWMCrates.getInstance().getSavedSuperObjects().values()) {
            if (superObject.id().isPresent() && superObject.id().get().equals(savedSuperObjectId)) {
                return Optional.of(superObject);
            }
        }
        return Optional.empty();
    }

    public static Optional<SuperObjectStorage> getSuperObjectStorage(SuperObjectType superObjectType, String type) {
        for (SuperObjectStorage superObjectStorage : GWMCrates.getInstance().getSuperObjects()) {
            if (superObjectStorage.getSuperObjectType().equals(superObjectType) &&
                    superObjectStorage.getType().equals(type)) {
                return Optional.of(superObjectStorage);
            }
        }
        return Optional.empty();
    }

    public static Optional<SuperObject> getSavedSuperObject(SuperObjectType superObjectType, String superObjectName) {
        for (Map.Entry<Pair<SuperObjectType, String>, SuperObject> entry : GWMCrates.getInstance().getSavedSuperObjects().entrySet()) {
            Pair<SuperObjectType, String> pair = entry.getKey();
            if (pair.getKey().equals(superObjectType) && pair.getValue().equals(superObjectName)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    public static OrderedInventory castToOrdered(Inventory inventory) {
        Inventory result = inventory.query(QueryOperationTypes.INVENTORY_TYPE.of(OrderedInventory.class));
        if (result instanceof OrderedInventory) {
            return (OrderedInventory) result;
        }
        if (result instanceof EmptyInventory) {
            throw new IllegalArgumentException("Inventory can not be casted to Ordered Inventory!");
        }
        for (Inventory subInventory : inventory) {
            if (subInventory instanceof OrderedInventory) {
                return (OrderedInventory) subInventory;
            }
        }
        throw new IllegalArgumentException("Inventory can not be casted to Ordered Inventory!");
    }

    public static Path getManagerRelativePath(File managerFile) {
        return GWMCrates.getInstance().getManagersDirectory().toPath().relativize(managerFile.toPath());
    }

    public static void sendCaseMissingMessage(CommandSource source, Manager manager) {
        if (manager.isSendCaseMissingMessage()) {
            source.sendMessage(manager.getCustomCaseMissingMessage().
                    orElse(GWMCrates.getInstance().getLanguage().getText("HAVE_NOT_CASE", source, null)));
        }
    }

    public static void sendKeyMissingMessage(CommandSource source, Manager manager) {
        if (manager.isSendKeyMissingMessage()) {
            source.sendMessage(manager.getCustomKeyMissingMessage().
                    orElse(GWMCrates.getInstance().getLanguage().getText("HAVE_NOT_KEY", source, null)));
        }
    }
}
