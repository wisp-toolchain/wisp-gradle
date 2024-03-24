package me.alphamode.wisp.test;

import org.gradle.api.Transformer;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public class FakeProperty<T> implements Property<T> {

    public FakeProperty(T defaultVal) {
        this.val = defaultVal;
    }

    public FakeProperty() {}

    private T val;

    @Override
    public void set(@Nullable T value) {
        this.val = value;
    }

    @Override
    public void set(Provider<? extends T> provider) {
        this.val = provider.get();
    }

    @Override
    public Property<T> value(@Nullable T value) {
        this.val = value;
        return this;
    }

    @Override
    public Property<T> value(Provider<? extends T> provider) {
        this.val = provider.get();
        return this;
    }

    @Override
    public Property<T> convention(@Nullable T value) {
        return null;
    }

    @Override
    public Property<T> convention(Provider<? extends T> provider) {
        return null;
    }

    @Override
    public void finalizeValue() {

    }

    @Override
    public void finalizeValueOnRead() {

    }

    @Override
    public void disallowChanges() {

    }

    @Override
    public void disallowUnsafeRead() {

    }

    @Override
    public T get() {
        return this.val;
    }

    @Nullable
    @Override
    public T getOrNull() {
        return this.val;
    }

    @Override
    public T getOrElse(T defaultValue) {
        return this.val;
    }

    @Override
    public <S> Provider<S> map(Transformer<? extends S, ? super T> transformer) {
        return null;
    }

    @Override
    public <S> Provider<S> flatMap(Transformer<? extends @Nullable Provider<? extends S>, ? super T> transformer) {
        return null;
    }

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public Provider<T> orElse(T value) {
        return null;
    }

    @Override
    public Provider<T> orElse(Provider<? extends T> provider) {
        return null;
    }

    @Override
    public Provider<T> forUseAtConfigurationTime() {
        return null;
    }

    @Override
    public <U, R> Provider<R> zip(Provider<U> right, BiFunction<? super T, ? super U, ? extends R> combiner) {
        return null;
    }
}
