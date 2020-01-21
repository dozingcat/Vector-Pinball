package com.dozingcatsoftware.bouncy;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class WorldLayers {
    private Vector2 gravity = Vector2.Zero;
    private ContactListener contactListener;
    private HashMap<Integer, World> worldsByLayer = new HashMap<Integer, World>();
    private List<Integer> layerValues = Collections.emptyList();
    private List<World> layerWorlds = Collections.emptyList();

    public WorldLayers(ContactListener listener) {
        this.contactListener = listener;
    }

    // Redundant storage so that we can iterate over the layers without memory allocations.
    // Not threadsafe.
    private void _rebuildArrays() {
        ArrayList<Integer> values = new ArrayList<Integer>();
        ArrayList<World> worlds = new ArrayList<World>();
        for (int lv : this.worldsByLayer.keySet()) {
            values.add(lv);
        }
        Collections.sort(values);
        for (int lv : values) {
            worlds.add(this.worldsByLayer.get(lv));
        }
        this.layerValues = Collections.unmodifiableList(values);
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

    public List<World> getOrderedWorlds() {
        return this.layerWorlds;
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
