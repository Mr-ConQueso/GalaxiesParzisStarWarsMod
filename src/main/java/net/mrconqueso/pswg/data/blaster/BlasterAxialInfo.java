package net.mrconqueso.pswg.data.blaster;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class BlasterAxialInfo
{
	public float horizontal;
	public float vertical;

	public static final StreamCodec<FriendlyByteBuf, BlasterAxialInfo> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, info -> info.horizontal,
			ByteBufCodecs.FLOAT, info -> info.vertical,
			BlasterAxialInfo::new
	);

	public BlasterAxialInfo(float horizontal, float vertical)
	{
		this.horizontal = horizontal;
		this.vertical = vertical;
	}

	public static BlasterAxialInfo read(FriendlyByteBuf buf) {
		float h = buf.readFloat();
		float v = buf.readFloat();
		return new BlasterAxialInfo(h, v);
	}

	public void write(FriendlyByteBuf buf)
	{
		buf.writeFloat(horizontal);
		buf.writeFloat(vertical);
	}
}
