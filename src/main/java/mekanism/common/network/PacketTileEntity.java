package mekanism.common.network;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;

import mekanism.api.Coord4D;
import mekanism.api.MekanismConfig.general;
import mekanism.common.PacketHandler;
import mekanism.common.base.ITileNetwork;
import mekanism.common.block.states.BlockStateMachine.MachineType;
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
		
		PacketHandler.handlePacket(new Runnable() {
			@Override
			public void run()
			{
				World world = player.worldObj;
				BlockPos pos = message.coord4D.getPos();
				
				TileEntity tileEntity = message.coord4D.getTileEntity(world);
				TileEntity expected = null;
				
				if(message.expectedMachineType != null)
				{
					Block block = world.getBlockState(pos).getBlock();
					if(block instanceof mekanism.common.block.BlockMachine)
					{
						mekanism.common.block.BlockMachine machineBlock = (mekanism.common.block.BlockMachine)block;
						IBlockState expectedState = machineBlock.getDefaultState().withProperty(machineBlock.getTypeProperty(), message.expectedMachineType);
						expected = machineBlock.createTileEntity(world, expectedState);
					}
					else
					{
						IBlockState state = world.getBlockState(pos);
						expected = state.getBlock().createTileEntity(world, state);
					}
				}
				else
				{
					IBlockState state = world.getBlockState(pos);
					expected = state.getBlock().createTileEntity(world, state);
				}
				
				if(expected != null && (tileEntity == null || tileEntity.getClass() != expected.getClass()))
				{
					//The client has a stale or missing tile entity. This happens after a machine to factory
					//conversion, where the block metadata changes but the block ID does not, so the client
					//side tile entity is never replaced by the block update. Replace it with the correct one.
					if(tileEntity != null)
					{
						tileEntity.invalidate();
					}
					
					world.setTileEntity(pos, expected);
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
			}
		}, player);
		
		return null;
	}
	
	public static class TileEntityMessage implements IMessage
	{
		public Coord4D coord4D;
	
		public ArrayList<Object> parameters;
		
		public ByteBuf storedBuffer = null;
		
		public MachineType expectedMachineType = null;
		
		public TileEntityMessage() {}
	
		public TileEntityMessage(Coord4D coord, ArrayList<Object> params)
		{
			coord4D = coord;
			parameters = params;
		}
		
		public TileEntityMessage(Coord4D coord, ArrayList<Object> params, MachineType type)
		{
			coord4D = coord;
			parameters = params;
			expectedMachineType = type;
		}
	
		@Override
		public void toBytes(ByteBuf dataStream)
		{
			coord4D.write(dataStream);
			
			if(general.logPackets)
			{
				MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
				
				if(server != null)
				{
					World world = server.worldServerForDimension(coord4D.dimensionId);
					PacketHandler.log("Sending TileEntity packet from coordinate " + coord4D + " (" + coord4D.getTileEntity(world) + ")");
				}
			}
			
			PacketHandler.encode(new Object[] {parameters}, dataStream);
			
			if(expectedMachineType != null)
			{
				dataStream.writeInt(expectedMachineType.ordinal());
			}
			else
			{
				dataStream.writeInt(-1);
			}
		}
	
		@Override
		public void fromBytes(ByteBuf dataStream)
		{
			coord4D = Coord4D.read(dataStream);
			
			int totalRemaining = dataStream.readableBytes();
			if(totalRemaining >= 4)
			{
				storedBuffer = dataStream.copy(dataStream.readerIndex(), totalRemaining - 4);
				dataStream.readerIndex(dataStream.readerIndex() + totalRemaining - 4);
				int typeOrdinal = dataStream.readInt();
				if(typeOrdinal >= 0)
				{
					expectedMachineType = MachineType.values()[typeOrdinal];
				}
			}
			else
			{
				storedBuffer = dataStream.copy();
			}
		}
	}
}