package dev.z1kzak.potionhud.config;

public enum SortMode {
    DURATION_ASC("potionhudx.sort.duration_asc"),
    DURATION_DESC("potionhudx.sort.duration_desc"),
    NAME("potionhudx.sort.name"),
    AMPLIFIER("potionhudx.sort.amplifier"),
    NONE("potionhudx.sort.none");

    private final String key;

    SortMode(String key) {
        this.key = key;
    }

    public String translationKey() {
        return key;
    }

    public SortMode next() {
        SortMode[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public SortMode prev() {
        SortMode[] v = values();
        return v[(ordinal() + v.length - 1) % v.length];
    }
}
