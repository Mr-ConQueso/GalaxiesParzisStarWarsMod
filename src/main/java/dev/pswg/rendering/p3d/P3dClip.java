package dev.pswg.rendering.p3d;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class P3dClip
{
	private P3dClip() {}

	static List<P3dModel.Vertex> clip(List<P3dModel.Vertex> vertices, Direction.Axis axis, float plane, boolean keepAbove)
	{
		if (vertices.isEmpty())
			return vertices;
		var clipped = new ArrayList<P3dModel.Vertex>(vertices.size() + 1);
		var previous = vertices.getLast();
		float previousDistance = distance(previous, axis, plane, keepAbove);
		for (var vertex : vertices)
		{
			float distance = distance(vertex, axis, plane, keepAbove);
			if (distance * previousDistance < 0)
			{
				float progress = previousDistance / (previousDistance - distance);
				clipped.add(new P3dModel.Vertex(Mth.lerp(progress, previous.x(), vertex.x()),
						Mth.lerp(progress, previous.y(), vertex.y()), Mth.lerp(progress, previous.z(), vertex.z()),
						Mth.lerp(progress, previous.u(), vertex.u()), Mth.lerp(progress, previous.v(), vertex.v())));
			}
			if (distance >= 0)
				clipped.add(vertex);
			previous = vertex;
			previousDistance = distance;
		}
		return clipped;
	}

	static void emitQuads(List<P3dModel.Vertex> vertices, Consumer<List<P3dModel.Vertex>> emit)
	{
		if (vertices.size() == 4)
			emit.accept(vertices);
		else
			for (int i = 1; i + 1 < vertices.size(); i++)
				emit.accept(List.of(vertices.getFirst(), vertices.get(i), vertices.get(i + 1), vertices.get(i + 1)));
	}

	static float coordinate(P3dModel.Vertex vertex, Direction.Axis axis)
	{
		return (float) axis.choose(vertex.x(), vertex.y(), vertex.z());
	}

	private static float distance(P3dModel.Vertex vertex, Direction.Axis axis, float plane, boolean keepAbove)
	{
		float distance = coordinate(vertex, axis) - plane;
		return keepAbove ? distance : -distance;
	}
}
