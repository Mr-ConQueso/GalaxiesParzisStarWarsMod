package net.mrconqueso.pswg.p3d;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.mrconqueso.pswg.Resources;
import net.mrconqueso.pswg.data.KeyedReloadableLoader;
import org.spongepowered.asm.util.perf.Profiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class P3dManager extends KeyedReloadableLoader<P3dModel>
{
	public static final ResourceLocation ID = Resources.id("pm3d_manager");
	public static final P3dManager INSTANCE = new P3dManager();

	private final Map<ResourceLocation, P3dModel> modelData;

	private P3dManager()
	{
		super("models", "p3d");
		modelData = new HashMap<>();
	}

	@Override
	public P3dModel readResource(ResourceManager resourceManager, Profiler profiler, InputStream stream) throws IOException
	{
		// All client-read models should have vertex data
		return P3dModel.read(stream, true);
	}

	public P3dModel get(ResourceLocation identifier)
	{
		return modelData.get(identifier);
	}

	@Override
	protected void apply(Map<ResourceLocation, P3dModel> prepared, ResourceManager manager, Profiler profiler)
	{
		modelData.clear();
		modelData.putAll(prepared);
	}

	@Override
	public ResourceLocation getFabricId()
	{
		return ID;
	}
}
