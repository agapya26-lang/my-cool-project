package net.lanternlight.infuse;

import java.util.Locale;

/** The 12 effects: 8 primaries and 4 supports. */
public enum InfuseType {
    STRENGTH(Slot.PRIMARY),
    THUNDER(Slot.PRIMARY),
    HASTE(Slot.PRIMARY),
    FROST(Slot.PRIMARY),
    HEART(Slot.PRIMARY),
    REGEN(Slot.PRIMARY),
    FEATHER(Slot.PRIMARY),
    INVIS(Slot.PRIMARY),
    EMERALD(Slot.SUPPORT),
    SPEED(Slot.SUPPORT),
    FIRE(Slot.SUPPORT),
    OCEAN(Slot.SUPPORT);

    public enum Slot { PRIMARY, SUPPORT }

    public final Slot slot;

    InfuseType(Slot slot) {
        this.slot = slot;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static InfuseType byId(String raw) {
        if (raw == null) return null;
        String s = raw.toLowerCase(Locale.ROOT);
        if (s.equals("invisibility")) s = "invis";
        if (s.equals("regeneration")) s = "regen";
        for (InfuseType t : values()) {
            if (t.id().equals(s)) return t;
        }
        return null;
    }
}
