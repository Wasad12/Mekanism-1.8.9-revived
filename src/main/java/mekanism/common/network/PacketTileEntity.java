package mekanism.common.network;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;

import mekanism.api.Coord4D;
import mekanism.common.PacketHandler;
import mekanism.common.base.ITileNetwork;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.util.MekanismUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketTileEntity implements IMessageHandler<TileEntityMessage, IMessage>
{
	@Override
	public IMessage onMessage(TileEntityMessage message, MessageContext context) 
	{
		EntityPlayer player = PacketHandler.getPlayer(context);
		
		if(player == null)
		{
			return null;
		}
		
		TileEntity tileEntity = message.coord4D.getTileEntity(player.worldObj);
		IBlockState state = player.worldObj.getBlockState(message.coord4D.getPos());
		TileEntity expected = state.getBlock().createTileEntity(player.worldObj, state);
		
		if(expected != null && (tileEntity == null || tileEntity.getClass() != expected.getClass()))
		{
			//The client has a stale or missing tile entity. This happens after a machine to factory
			//conversion, where the block metadata changes but the block ID does not, so the client
			//side tile entity is never replaced by the block update. Replace it with the correct one.
			if(tileEntity != null)
			{
				tileEntity.invalidate();
			}
			
			player.worldObj.setTileEntity(message.coord4D.getPos(), expected);
			tileEntity = expected;
		}
		
		if(tileEntity != null && MekanismUtils.hasCapability(tileEntity, Capabilities.TILE_NETWORK_CAPABILITY, null))
		{
			ITileNetwork network = MekanismUtils.getCapability(tileEntity, Capabilities.TILE_NETWORK_CAPABILITY, null);
			
			try {
				network.handlePacketData(message.storedBuffer);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		message.storedBuffer.release();
		
		return null;
	}
	
	public static class TileEntityMessage implements IMessage
	{
		public Coord4D coord4D;
	
		public ArrayList<Object> parameters;
		
		public ByteBuf storedBuffer = null;
		
		public TileEntityMessage() {}
	
		public TileEntityMessage(Coord4D coord, ArrayList<Object> params)
		{
			coord4D = coord;
			parameters = params;
		}
	
		@Override
		public void toBytes(ByteBuf dataStream)
		{
			coord4D.write(dataStream);
			
			MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
			
			if(server != null)
			{
				World world = server.worldServerForDimension(coord4D.dimensionId);
				PacketHandler.log("Sending TileEntity packet from coordinate " + coord4D + " (" + coord4D.getTileEntity(world) + ")");
			}
			
			PacketHandler.encode(new Object[] {parameters}, dataStream);
		}
	
		@Override
		public void fromBytes(ByteBuf dataStream)
		{
			coord4D = Coord4D.read(dataStream);
			
			storedBuffer = dataStream.copy();
		}
	}
}