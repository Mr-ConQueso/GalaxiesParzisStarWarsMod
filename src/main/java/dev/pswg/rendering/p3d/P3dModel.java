package dev.pswg.rendering.p3d;

import com.google.common.io.LittleEndianDataInputStream;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record P3dModel(List<Part> roots, Map<String, Socket> sockets, AABB bounds)
{
	public static P3dModel read(InputStream stream, boolean geometry) throws IOException
	{
		var input = new LittleEndianDataInputStream(stream);
		String magic = geometry ? "P3D" : "P3DR";
		var header = new byte[magic.length()];
		input.readFully(header);
		if (!magic.equals(new String(header, StandardCharsets.US_ASCII)) || input.readInt() != 2)
			throw new IOException("Unsupported " + magic + " header");

		var sockets = new HashMap<String, Socket>();
		int socketCount = count(input, 8192);
		for (int i = 0; i < socketCount; i++)
		{
			String name = name(input);
			String parent = input.readBoolean() ? name(input) : null;
			put(sockets, name, new Socket(parent, matrix(input)));
		}
		int rootCount = count(input, 8192);
		var roots = new ArrayList<Part>(rootCount);
		var bounds = new Bounds();
		for (int i = 0; i < rootCount; i++)
			roots.add(part(input, sockets, null, geometry, 0, bounds));
		var model = new P3dModel(List.copyOf(roots), Map.copyOf(sockets), bounds.box());
		for (String name : model.sockets.keySet())
			if (model.transform(name) == null)
				throw new IOException("Missing or circular ancestry for " + name);
		return model;
	}

	private static Part part(LittleEndianDataInputStream stream, Map<String, Socket> sockets, String parent, boolean geometry,
	                         int depth, Bounds bounds) throws IOException
	{
		if (depth >= 64)
			throw new IOException("P3D model hierarchy exceeds 64 levels");
		String name = name(stream);
		var transform = matrix(stream);
		put(sockets, name, new Socket(parent, transform));
		int material = geometry ? stream.readUnsignedByte() : 0;
		int faceCount = geometry ? count(stream, 250_000) : 0;
		var faces = new ArrayList<Face>(faceCount);
		for (int i = 0; i < faceCount; i++)
		{
			var normal = new Vector3f(stream.readFloat(), stream.readFloat(), stream.readFloat());
			var vertices = new Vertex[4];
			for (int j = 0; j < 4; j++)
			{
				var vertex = new Vertex(stream.readFloat(), stream.readFloat(), stream.readFloat(), stream.readFloat(), 1F - stream.readFloat());
				vertices[j] = vertex;
				bounds.include(vertex);
			}
			faces.add(new Face(normal, vertices));
		}
		int childCount = count(stream, 8192);
		var children = new ArrayList<Part>(childCount);
		for (int i = 0; i < childCount; i++)
			children.add(part(stream, sockets, name, geometry, depth + 1, bounds));
		return new Part(name, transform, material, List.copyOf(faces), List.copyOf(children));
	}

	private static void put(Map<String, Socket> sockets, String name, Socket socket) throws IOException
	{
		if (sockets.putIfAbsent(name, socket) != null)
			throw new IOException("Duplicate P3D part or socket " + name);
	}

	private static Matrix4f matrix(LittleEndianDataInputStream stream) throws IOException
	{
		var values = new float[16];
		for (int i = 0; i < 16; i++)
		{
			values[i] = stream.readFloat();
			if (!Float.isFinite(values[i]))
				throw new IOException("Non-finite P3D transformation");
		}
		return new Matrix4f().setTransposed(values);
	}

	private static int count(LittleEndianDataInputStream stream, int limit) throws IOException
	{
		int count = stream.readInt();
		if (count < 0 || count > limit)
			throw new IOException("Invalid P3D element count " + count);
		return count;
	}

	private static String name(LittleEndianDataInputStream stream) throws IOException
	{
		var name = new StringBuilder();
		while (name.length() < 1024)
		{
			int character = stream.readUnsignedByte();
			if (character == 0)
				return name.toString();
			name.append((char) character);
		}
		throw new IOException("P3D name is too long");
	}

	public Matrix4f transform(String name)
	{
		return transform(name, part -> null);
	}

	public Matrix4f transform(String name, java.util.function.Function<String, Matrix4f> animation)
	{
		var socket = sockets.get(name);
		if (socket == null)
			return null;
		var value = new Matrix4f(socket.transform);
		var movement = animation.apply(name);
		if (movement != null)
			value.mul(movement);
		Set<String> chain = new HashSet<>();
		chain.add(name);
		while (socket.parent != null)
		{
			if (!chain.add(socket.parent))
				return null;
			String parent = socket.parent;
			socket = sockets.get(parent);
			if (socket == null)
				return null;
			movement = animation.apply(parent);
			if (movement != null)
				value.mulLocal(movement);
			value.mulLocal(socket.transform);
		}
		return value;
	}

	// The Fabric item renderer framed P3D icons using all raw vertices, including optional parts.
	private static final class Bounds
	{
		private float minX = Float.POSITIVE_INFINITY;
		private float minY = Float.POSITIVE_INFINITY;
		private float minZ = Float.POSITIVE_INFINITY;
		private float maxX = Float.NEGATIVE_INFINITY;
		private float maxY = Float.NEGATIVE_INFINITY;
		private float maxZ = Float.NEGATIVE_INFINITY;

		private void include(Vertex vertex)
		{
			minX = Math.min(minX, vertex.x());
			minY = Math.min(minY, vertex.y());
			minZ = Math.min(minZ, vertex.z());
			maxX = Math.max(maxX, vertex.x());
			maxY = Math.max(maxY, vertex.y());
			maxZ = Math.max(maxZ, vertex.z());
		}

		private AABB box()
		{
			return minX != Float.POSITIVE_INFINITY ? new AABB(minX, minY, minZ, maxX, maxY, maxZ) : new AABB(0, 0, 0, 0, 0, 0);
		}
	}

	public record Socket(String parent, Matrix4f transform) {}
	public record Part(String name, Matrix4f transform, int material, List<Face> faces, List<Part> children) {}
	public record Face(Vector3f normal, Vertex[] vertices) {}
	public record Vertex(float x, float y, float z, float u, float v) {}
}
