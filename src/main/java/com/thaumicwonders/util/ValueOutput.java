package com.thaumicwonders.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;

/** Small writer over a CompoundTag, keeping save code the same on both game versions. */
public final class ValueOutput {
    private final CompoundTag tag;
    private final HolderLookup.Provider registries;
    private final RegistryOps<Tag> ops;

    public ValueOutput(CompoundTag tag, HolderLookup.Provider registries) {
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

    public <T> void store(String key, Codec<T> codec, T value) {
        codec.encodeStart(ops, value).result().ifPresent(t -> tag.put(key, t));
    }

    public <T> void storeNullable(String key, Codec<T> codec, @Nullable T value) {
        if (value != null) {
            store(key, codec, value);
        }
    }

    public ValueOutput child(String key) {
        CompoundTag c = new CompoundTag();
        tag.put(key, c);
        return new ValueOutput(c, registries);
    }

    public void putInt(String key, int v) {
        tag.putInt(key, v);
    }

    public void putShort(String key, short v) {
        tag.putShort(key, v);
    }

    public void putLong(String key, long v) {
        tag.putLong(key, v);
    }

    public void putString(String key, String v) {
        tag.putString(key, v);
    }

    public void putBoolean(String key, boolean v) {
        tag.putBoolean(key, v);
    }

    public void putFloat(String key, float v) {
        tag.putFloat(key, v);
    }

    public void putDouble(String key, double v) {
        tag.putDouble(key, v);
    }

    public void putIntArray(String key, int[] v) {
        tag.putIntArray(key, v);
    }
}
