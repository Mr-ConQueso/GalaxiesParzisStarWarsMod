package net.mrconqueso.pswg.data.blaster;

import java.util.HashMap;

public record BlasterAttachmentMap(int attachmentMinimum, int attachmentDefault, HashMap<Integer, BlasterAttachmentDescriptor> attachmentMap)
{
}
