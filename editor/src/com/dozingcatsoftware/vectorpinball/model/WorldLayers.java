package com.dozingcatsoftware.vectorpinball.model;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Collection of Box2d worlds, one for each "layer" of a table.
 */
public class WorldLayers {
    private Vector2 gravity = Vector2.Zero;
    private ContactListener contactListener;
    private HashMap<Integer, World> worldsByLayer = new HashMap<>();
    private List<World> layerWorlds = Collections.emptyList();

    public WorldLayers(ContactListener listener) {
        this.contactListener = listener;
    }

    // Redundant storage so that we can iterate over the layers without memory allocations.
    // Not threadsafe.
    private void _rebuildArrays() {
        ArrayList<Integer> levelValues = new ArrayList<>(this.worldsByLayer.keySet());
        Collections.sort(levelValues);
        ArrayList<World> worlds = new ArrayList<>();
        for (int lv : levelValues) {
            worlds.add(this.worldsByLayer.get(lv));
        }
        this.layerWorlds = Collections.unmodifiableList(worlds);
    }

    public World existingWorldForLayer(int layer) {
        return this.worldsByLayer.get(layer);
    }

    public World existingOrNewWorldForLayer(int layer) {
        World w = this.existingWorldForLayer(layer);
        if (w == null) {
            w = new World(this.gravity, false);
            w.setContactListener(this.contactListener);
            this.worldsByLayer.put(layer, w);
            this._rebuildArrays();
        }
        return w;
    }

    void setGravity(Vector2 gravity) {
        this.gravity = gravity.cpy();
        for (int i = 0; i < this.layerWorlds.size(); i++) {
            this.layerWorlds.get(i).setGravity(this.gravity);
        }
    }

    void step(float dt, int velIters, int posIters) {
        int n = this.layerWorlds.size();
        for (int i = 0; i < n; i++) {
            this.layerWorlds.get(i).step(dt, velIters, posIters);
        }
    }
}
