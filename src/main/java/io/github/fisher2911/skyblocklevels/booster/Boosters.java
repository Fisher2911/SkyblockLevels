package io.github.fisher2911.skyblocklevels.booster;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.database.CreateTableStatement;
import io.github.fisher2911.skyblocklevels.database.DataManager;
import io.github.fisher2911.skyblocklevels.database.DeleteStatement;
import io.github.fisher2911.skyblocklevels.database.InsertStatement;
import io.github.fisher2911.skyblocklevels.database.KeyType;
import io.github.fisher2911.skyblocklevels.database.SelectStatement;
import io.github.fisher2911.skyblocklevels.database.VarChar;
import io.github.fisher2911.skyblocklevels.math.Operation;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

public class Boosters {

    public static final String TABLE = Boosters.class.getSimpleName().toLowerCase();
    public static final String START_TIME_EPOCH_FIELD = "start_time";
    public static final String DURATION_FIELD = "duration";
    public static final String AMOUNT_FIELD = "amount";
    public static final String TYPE_FIELD = "type";
    public static final String OPERATION_FIELD = "operation";
    public static final String OWNER_FIELD = "owner";

    private static final TimeZone ZONE = TimeZone.getTimeZone(ZoneId.of("UTC"));

    static {
        final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
        final DataManager dataManager = plugin.getDataManager();
        dataManager.addTable(
                CreateTableStatement.builder(TABLE).
                        addField(Long.class, START_TIME_EPOCH_FIELD).
                        addField(Integer.class, DURATION_FIELD).
                        addField(Integer.class, AMOUNT_FIELD).
                        addField(VarChar.of(20), TYPE_FIELD).
                        addField(VarChar.of(1), OPERATION_FIELD).
                        addField(VarChar.UUID, OWNER_FIELD).
                        groupKeys(KeyType.UNIQUE, OWNER_FIELD, START_TIME_EPOCH_FIELD, TYPE_FIELD, OPERATION_FIELD).
                        build()
        );
    }

    private final ListMultimap<BoosterType, Booster> boosters = Multimaps.newListMultimap(new EnumMap<>(BoosterType.class), ArrayList::new);

    public void addBooster(Booster booster) {
        final Booster active = this.getActiveBooster(booster.getBoosterType());
        if (active == null && !booster.isStarted()) booster.start();
        this.boosters.put(booster.getBoosterType(), booster);
    }

    public void removeBooster(Booster booster) {
        this.boosters.remove(booster.getBoosterType(), booster);
    }

    public boolean hasBooster(BoosterType boosterType) {
        return this.boosters.containsKey(boosterType);
    }

    public Collection<Booster> getBoosters(BoosterType boosterType) {
        final Collection<Booster> boosters = this.boosters.get(boosterType);
        boosters.removeIf(booster -> {
            if (booster.isExpired()) {
                this.delete(booster);
                return true;
            }
            return false;
        });
        return this.boosters.get(boosterType);
    }

    @Nullable
    public Booster getActiveBooster(BoosterType boosterType) {
        for (Booster booster : this.boosters.get(boosterType)) {
            if (!booster.isExpired()) {
                if (!booster.isStarted()) booster.start();
                return booster;
            }
            Bukkit.getScheduler().runTaskAsynchronously(SkyblockLevels.getPlugin(SkyblockLevels.class), () -> this.delete(booster));
        }
        return null;
    }

    public double getBoosterValue(BoosterType boosterType) {
        final Booster activeBooster = this.getActiveBooster(boosterType);
        if (activeBooster == null) return 0;
        return activeBooster.getValue();
    }

    public int getBoosterTimeLeft(BoosterType boosterType) {
        final Booster activeBooster = this.getActiveBooster(boosterType);
        if (activeBooster == null) return 0;
        return activeBooster.getTimeLeft();
    }

    public void save(Connection connection) {
        final InsertStatement.Builder builder = InsertStatement.builder(TABLE);
        final var boosters = this.boosters.values();
        if (boosters.isEmpty()) return;
        boolean hasValues = false;
        for (Booster booster : boosters) {
            if (booster.isExpired()) {
                this.delete(booster);
                continue;
            }
            hasValues = true;
            final Instant startTime = booster.getStartTime();
            builder.
                    newEntry().
                    addEntry(OWNER_FIELD, booster.getOwner()).
                    addEntry(START_TIME_EPOCH_FIELD, startTime == null ? -1 : startTime.toEpochMilli()).
                    addEntry(DURATION_FIELD, booster.getSeconds()).
                    addEntry(AMOUNT_FIELD, booster.getValue()).
                    addEntry(TYPE_FIELD, booster.getBoosterType().toString()).
                    addEntry(OPERATION_FIELD, booster.getOperation().getSign()).
                    batchSize(boosters.size());
        }
        if (!hasValues) return;
        builder.build().execute(connection);
    }

    private void delete(Booster booster) {
        if (!booster.isStarted()) return;
        final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                DeleteStatement.builder(TABLE).
                        condition(OWNER_FIELD, booster.getOwner()).
                        condition(START_TIME_EPOCH_FIELD, booster.getStartTime().toEpochMilli()).
                        condition(TYPE_FIELD, booster.getBoosterType().toString()).
                        condition(OPERATION_FIELD, booster.getOperation().getSign()).
                        build().
                        execute(plugin.getDataManager().getConnection()));
    }

    public static Boosters load(Connection connection, UUID owner) {
        final Boosters boosters = new Boosters();
        final List<Booster> boosterList = SelectStatement.builder(TABLE).
                condition(OWNER_FIELD, owner).
                selectAll().
                build().
                execute(connection, resultSet -> {
                    final long epocMillis = resultSet.getLong(START_TIME_EPOCH_FIELD);
                    final Instant startTime = epocMillis == -1 ? null : Instant.ofEpochMilli(epocMillis);
                    final int duration = resultSet.getInt(DURATION_FIELD);
                    final double amount = resultSet.getDouble(AMOUNT_FIELD);
                    final String type = resultSet.getString(TYPE_FIELD);
                    final String operation = resultSet.getString(OPERATION_FIELD);
                    final BoosterType boosterType = BoosterType.valueOf(type);
                    final Operation operationEnum = Operation.bySign(operation.charAt(0));
                    return new Booster(owner, boosterType, operationEnum, amount, duration, startTime);
                });
        boosterList.sort((o1, o2) -> {
            if (o1.isStarted() && !o2.isStarted()) return -1;
            if (!o1.isStarted() && o2.isStarted()) return 1;
            if (!o1.isStarted() && !o2.isStarted()) return 0;
            return o1.getStartTime().compareTo(o2.getStartTime());
        });
        for (Booster booster : boosterList) {
            boosters.addBooster(booster);
        }
        return boosters;
    }

}
