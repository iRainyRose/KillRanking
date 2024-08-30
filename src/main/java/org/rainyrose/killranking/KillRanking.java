package org.rainyrose.killranking;

import cn.lanink.gamecore.ranking.Ranking;
import cn.lanink.gamecore.ranking.RankingAPI;
import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.level.Position;
import cn.nukkit.plugin.PluginBase;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.util.HashMap;

/**
 * @author Rainy_Rose
 */
public class KillRanking extends PluginBase implements Listener {

    private Ranking ranking;

    private final HashMap<String, Integer> killCount = new HashMap<>();

    @Override
    public void onLoad() {
        this.saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);

        if (new File(this.getDataFolder() + "/killCount.json").exists()) {
            try {
                this.killCount.putAll(new Gson().fromJson(new JsonReader(new FileReader(this.getDataFolder() + "/killCount.json")), HashMap.class));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        HashMap<Object, Object> data = this.getConfig().get("ranking", new HashMap<>());
        String levelName = (String) data.get("level");
        if (!this.getServer().loadLevel(levelName)) {
            this.getLogger().error("世界 " + levelName + " 不存在，无法加载排行榜数据！");
            return;
        }
         Position position = new Position(
                (double) data.get("x"),
                (double) data.get("y"),
                (double) data.get("z"),
                this.getServer().getLevelByName(levelName)
        );
        this.ranking = RankingAPI.createRanking(this, "击杀排行榜", position);
        this.ranking.setRankingList(() -> this.killCount);

        this.getLogger().info("加载完成！");
    }

    @Override
    public void onDisable() {
        if (this.ranking != null) {
            this.ranking.close();
        }
        String json = new Gson().toJson(this.killCount);
        try (FileOutputStream stream = new FileOutputStream(this.getDataFolder() + "/killCount.json")) {
            stream.write(json.getBytes());
            stream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent ev) {
        EntityDamageEvent lastDamageCause = ev.getEntity().getLastDamageCause();
        if (lastDamageCause instanceof EntityDamageByEntityEvent event) {
            if (event.getDamager() instanceof Player player) {
                String name = player.getName();
                this.killCount.put(name, this.killCount.getOrDefault(name, 0) + 1);
            }
        }
    }
}
