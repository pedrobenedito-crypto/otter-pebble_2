package com.huuda.otterpebble.entity;

/** Visible continuous actions, synced to clients to drive the animation controller. */
public enum OtterAction {
    NONE,
    SLIDE,
    GROOM,
    DIVE,
    WATCH,
    AWKWARD_DIVE,
    PEBBLE_PLAY,
    PAIR_LEFT_PAW,
    PAIR_RIGHT_PAW;

    public static OtterAction byId(int id) {
        OtterAction[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }

    /** Poses that lie on the back (no head look, no swim pitch). */
    public boolean isOnBack() {
        return this == PEBBLE_PLAY || this == PAIR_LEFT_PAW || this == PAIR_RIGHT_PAW;
    }

    public boolean isPaired() {
        return this == PAIR_LEFT_PAW || this == PAIR_RIGHT_PAW;
    }
}
