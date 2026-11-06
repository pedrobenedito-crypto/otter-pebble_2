package com.huuda.otterpebble.entity;

/** Relationship row of the "Behavior states" slide: Unbonded -> Curious -> Inspecting -> Bonded. */
public enum BondState {
    UNBONDED,
    CURIOUS,
    INSPECTING,
    BONDED;

    public static BondState byId(int id) {
        BondState[] values = values();
        return id >= 0 && id < values.length ? values[id] : UNBONDED;
    }
}
