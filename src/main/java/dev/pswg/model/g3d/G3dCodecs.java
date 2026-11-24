package dev.pswg.model.g3d;

import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.util.ExtraCodecs;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

final class G3dCodecs
{
	static final Codec<Vector2fc> VECTOR2 = Codec.FLOAT.listOf().comapFlatMap(
			values -> Util.fixedSize(values, 2).map(pair -> new Vector2f(pair.get(0), pair.get(1))),
			vector -> List.of(vector.x(), vector.y()));
	static final Codec<Vector3fc> VECTOR3 = ExtraCodecs.VECTOR3F.xmap(vector -> vector, Vector3f::new);
	static final Codec<Quaternionfc> QUATERNION = ExtraCodecs.QUATERNIONF.xmap(rotation -> rotation, Quaternionf::new);

	private G3dCodecs()
	{
	}
}
