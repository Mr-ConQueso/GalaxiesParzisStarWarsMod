package net.mrconqueso.pswg.data.blaster;

import net.minecraft.resources.ResourceLocation;

public class BlasterAttachmentDescriptor
{
	public int bit;
	public int mutex;
	public BlasterAttachmentCategory category;
	public String id;
	public BlasterAttachmentFunction function;
	public String visualComponent;
	public ResourceLocation texture;

	public BlasterAttachmentDescriptor(int bit, int mutex, BlasterAttachmentCategory category, String id, BlasterAttachmentFunction function, String visualComponent, ResourceLocation texture)
	{
		this.bit = bit;
		this.mutex = mutex;
		this.category = category;
		this.id = id;
		this.function = function;
		this.visualComponent = visualComponent;
		this.texture = texture;
	}
}
