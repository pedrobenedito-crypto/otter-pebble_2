package com.huuda.otterpebble.entity;

/** The three palette options from the "One silhouette, two ages" slide. Cosmetic only. */
public enum OtterVariant {
    WARM_CHESTNUT("warm_chestnut"),
    DEEP_BROWN("deep_brown"),
    GRAY_BROWN("gray_brown");

    private final String textureName;

    OtterVariant(String textureName) {
        this.textureName = textureName;
    }

    public String textureName() {
        return textureName;
    }

    public static OtterVariant byId(int id) {
        OtterVariant[] values = values();
        return id >= 0 && id < values.length ? values[id] : WARM_CHESTNUT;
    }
}
