package net.mrconqueso.pswg.data;

import net.minecraft.resources.ResourceLocation;

public class TintedIdentifier implements ResourceLocation {
	public enum Mode {
		Multiply,
		Add,
		Overlay
	}

	private final ResourceLocation delegate;
	private final int tint;
	private final Mode tintMode;

	public TintedIdentifier(String namespace, String path, int tint) {
		this(namespace, path, tint, Mode.Multiply);
	}

	public TintedIdentifier(String namespace, String path, int tint, Mode mode) {
		this.delegate = ResourceLocation.fromNamespaceAndPath(namespace, path);
		this.tint = tint;
		this.tintMode = mode;
	}

	public TintedIdentifier(ResourceLocation other, int tint, Mode mode) {
		this.delegate = other;
		this.tint = tint;
		this.tintMode = mode;
	}

	public int getTint() {
		return tint;
	}

	public Mode getTintMode() {
		return tintMode;
	}

	@Override
	public String getNamespace() {
		return delegate.getNamespace();
	}

	@Override
	public String getPath() {
		return delegate.getPath();
	}

	@Override
	public String toString() {
		return delegate.toString();
	}
}