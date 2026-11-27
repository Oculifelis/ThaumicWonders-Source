package com.thaumicwonders.util;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;

/** Small reader over a CompoundTag, keeping load code the same on both game versions. */
public final class ValueInput {
    private final CompoundTag tag;
    private final HolderLookup.Provider registries;
    private final RegistryOps<Tag> ops;

    public ValueInput(CompoundTag tag, HolderLookup.Provider registries) {
        this.tag = tag;
        this.registries = registries;
        this.ops = registries.createSerializationContext(NbtOps.INSTANCE);
    }

    public CompoundTag tag() {
        return tag;
    }

    public HolderLookup.Provider registries() {
        return registries;
    }

    public <T> Optional<T> read(String key, Codec<T> codec) {
        Tag t = tag.get(key);
        return t == null ? Optional.empty() : codec.parse(ops, t).result();
    }

    public Optional<ValueInput> child(String key) {
        return tag.contains(key, Tag.TAG_COMPOUND) ? Optional.of(new ValueInput(tag.getCompound(key), registries)) : Optional.empty();
    }

    public int getIntOr(String key, int def) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : def;
    }

    public short getShortOr(String key, short def) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getShort(key) : def;
    }

    public long getLongOr(String key, long def) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getLong(key) : def;
    }

    public float getFloatOr(String key, float def) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getFloat(key) : def;
    }

    public double getDoubleOr(String key, double def) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getDouble(key) : def;
    }

    public boolean getBooleanOr(String key, boolean def) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getBoolean(key) : def;
    }

    public String getStringOr(String key, String def) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : def;
    }

    public Optional<int[]> getIntArray(String key) {
        return tag.contains(key, Tag.TAG_INT_ARRAY) ? Optional.of(tag.getIntArray(key)) : Optional.empty();
    }
}
