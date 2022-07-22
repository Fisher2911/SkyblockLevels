package io.github.fisher2911.skyblocklevels.logger;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockLogger {

    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:s:A");
    private static final String LOG_FOLDER_NAME = "logs";

    private final SkyblockLevels plugin;
    private String currentDayFileName;
    private final Map<LogAction, BufferedWriter> writers = new HashMap<>();
    final ExecutorService SERVICE = Executors.newSingleThreadExecutor();

    public BlockLogger(SkyblockLevels plugin) {
        this.plugin = plugin;
        this.currentDayFileName = this.createCurrentDayFileName();
    }

    private File getFile(LogCategory category, LogAction action) {
        final File dataFolder = this.plugin.getDataFolder();
        final String currentDayFileName = this.createCurrentDayFileName();
        if (!currentDayFileName.equalsIgnoreCase(this.currentDayFileName)) {
            this.currentDayFileName = currentDayFileName;
            final BufferedWriter writer = this.writers.remove(action);
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!dataFolder.exists()) dataFolder.mkdirs();
        final File file = dataFolder.toPath().resolve(LOG_FOLDER_NAME).resolve(category.name()).resolve(action.name() + "-" + this.currentDayFileName + ".txt").toFile();
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    private String createCurrentDayFileName() {
        return FILE_NAME_FORMATTER.format(LocalDateTime.now());
    }

    public void logBlockPlace(String player, SkyBlock block, WorldPosition at) {
        final File file = this.getFile(LogCategory.SKYBLOCK, LogAction.SKYBLOCK_BLOCK_PLACE);
        try {
            BufferedWriter writer = this.writers.get(LogAction.SKYBLOCK_BLOCK_PLACE);
            if (writer == null) {
                writer = new BufferedWriter(new java.io.FileWriter(file, true));
                this.writers.put(LogAction.SKYBLOCK_BLOCK_PLACE, writer);
            }
            final String log = LOG_FORMATTER.format(LocalDateTime.now()) + " - " + player + " placed a " + block.getItemId() + " : " + block.getClass().getSimpleName() + " block at " + at;
            final BufferedWriter finalWriter = writer;
            SERVICE.submit(() -> {
                try {
                    finalWriter.write(log);
                    finalWriter.newLine();
                    finalWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logBlockPlaceSave(String player, SkyBlock block, WorldPosition at) {
        final File file = this.getFile(LogCategory.SKYBLOCK, LogAction.SKYBLOCK_BLOCK_PLACE);
        try {
            BufferedWriter writer = this.writers.get(LogAction.SKYBLOCK_BLOCK_PLACE);
            if (writer == null) {
                writer = new BufferedWriter(new java.io.FileWriter(file, true));
                this.writers.put(LogAction.SKYBLOCK_BLOCK_PLACE, writer);
            }
            final String log = LOG_FORMATTER.format(LocalDateTime.now()) + " - " + player + " saved a placed " + block.getItemId() + " : " + block.getClass().getSimpleName() + " block at " + at;
            final BufferedWriter finalWriter = writer;
            SERVICE.submit(() -> {
                try {
                    finalWriter.write(log);
                    finalWriter.newLine();
                    finalWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logBlockBreak(String player, SkyBlock block, WorldPosition at) {
        final File file = this.getFile(LogCategory.SKYBLOCK, LogAction.SKYBLOCK_BLOCK_BREAK);
        try {
            BufferedWriter writer = this.writers.get(LogAction.SKYBLOCK_BLOCK_BREAK);
            if (writer == null) {
                writer = new BufferedWriter(new java.io.FileWriter(file, true));
                this.writers.put(LogAction.SKYBLOCK_BLOCK_BREAK, writer);
            }
            final String log = LOG_FORMATTER.format(LocalDateTime.now()) + " - " + player + " broke a " + block.getItemId() + " : " + block.getClass().getSimpleName() + " block at " + at;
            final BufferedWriter finalWriter = writer;
            SERVICE.submit(() -> {
                try {
                    finalWriter.write(log);
                    finalWriter.newLine();
                    finalWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logBlockBreakSave(String player, SkyBlock block, WorldPosition at) {
        final File file = this.getFile(LogCategory.SKYBLOCK, LogAction.SKYBLOCK_BLOCK_BREAK);
        try {
            BufferedWriter writer = this.writers.get(LogAction.SKYBLOCK_BLOCK_BREAK);
            if (writer == null) {
                writer = new BufferedWriter(new java.io.FileWriter(file, true));
                this.writers.put(LogAction.SKYBLOCK_BLOCK_BREAK, writer);
            }
            final String log = LOG_FORMATTER.format(LocalDateTime.now()) + " - " + player + " saved a broken a " + block.getItemId() + " : " + block.getClass().getSimpleName() + " block at " + at;
            final BufferedWriter finalWriter = writer;
            SERVICE.submit(() -> {
                try {
                    finalWriter.write(log);
                    finalWriter.newLine();
                    finalWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        for (BufferedWriter writer : this.writers.values()) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
