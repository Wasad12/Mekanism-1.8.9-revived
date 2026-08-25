package mekanism.common.util;

import java.util.ArrayList;

import mekanism.api.Coord4D;
import mekanism.api.Range4D;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.MekanismBlocks;
import mekanism.common.SideData;
import mekanism.common.Tier.FactoryTier;
import mekanism.common.Upgrade;
import mekanism.common.base.IFactory.RecipeType;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.tile.TileEntityBasicBlock;
import mekanism.common.tile.TileEntityFactory;
import mekanism.common.tile.component.TileComponentConfig;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/**
 * Centralized helper for Factory upgrades — mirrors 1.10.2 refactoring.
 * Handles block-state swap, common field copy, inventory mapping and
 * client sync (packet + block update) in one place to avoid duplication
 * across TileEntityElectricMachine / Advanced / Infuser / Factory.
 */
public final class FactoryUpgradeUtils
{
	private FactoryUpgradeUtils() {}

	/**
	 * Create a fresh Factory tile at the given pos by swapping block state
	 * to the correct Factory meta. Handles BASIC/ADVANCED/ELITE mapping.
	 */
	public static TileEntityFactory createFactory(World world, BlockPos pos, FactoryTier targetTier)
	{
		int meta;
		if(targetTier == FactoryTier.BASIC) meta = 5;
		else if(targetTier == FactoryTier.ADVANCED) meta = 6;
		else if(targetTier == FactoryTier.ELITE) meta = 7;
		else meta = 5;

		world.setBlockToAir(pos);
		world.setBlockState(pos, MekanismBlocks.MachineBlock.getStateFromMeta(meta), 3);

		TileEntityFactory factory = (TileEntityFactory)world.getTileEntity(pos);
		if(factory == null)
		{
			return null;
		}
		factory.validate();
		return factory;
	}

	/**
	 * Overload for machine→BASIC factory (used by Electric/Advanced/Infuser).
	 */
	public static TileEntityFactory createBasicFactory(World world, BlockPos pos)
	{
		return createFactory(world, pos, FactoryTier.BASIC);
	}

	/**
	 * Copy common fields from any TileEntityBasicBlock-based machine to a
	 * freshly created factory. Mirrors the field-copy block that was
	 * duplicated across three upgrade() methods.
	 */
	public static void copyCommon(TileEntityBasicBlock from, TileEntityFactory to, RecipeType type)
	{
		// BasicBlock
		to.facing = from.facing;
		to.clientFacing = from.clientFacing;
		to.ticker = from.ticker;
		to.redstone = from.redstone;
		to.redstoneLastTick = from.redstoneLastTick;
		to.doAutoSync = from.doAutoSync;

		// Electric
		if(from instanceof mekanism.common.tile.TileEntityElectricBlock)
		{
			to.electricityStored = ((mekanism.common.tile.TileEntityElectricBlock)from).electricityStored;
		}

		// Noisy
		if(from instanceof mekanism.common.tile.TileEntityNoisyElectricBlock)
		{
			to.soundURL = ((mekanism.common.tile.TileEntityNoisyElectricBlock)from).soundURL;
		}

		// Machine (progress[0], active, control, etc.)
		if(from instanceof mekanism.common.tile.TileEntityBasicMachine)
		{
			mekanism.common.tile.TileEntityBasicMachine<?,?,?> basicMachine = (mekanism.common.tile.TileEntityBasicMachine<?,?,?>)from;
			to.progress[0] = basicMachine.operatingTicks;
			to.updateDelay = basicMachine.updateDelay;
			to.isActive = basicMachine.isActive;
			to.clientActive = basicMachine.clientActive;
			to.controlType = basicMachine.controlType;
			to.prevEnergy = basicMachine.prevEnergy;
			to.upgradeComponent.readFrom(basicMachine.upgradeComponent);
			to.upgradeComponent.setUpgradeSlot(0);
			to.ejectorComponent.readFrom(basicMachine.ejectorComponent);
			to.ejectorComponent.setOutputData(TransmissionType.ITEM, to.configComponent.getOutputs(TransmissionType.ITEM).get(2));
			to.securityComponent.readFrom(basicMachine.securityComponent);
			// config + ejector
			for(TransmissionType transmission : basicMachine.configComponent.transmissions)
			{
				to.configComponent.setConfig(transmission, basicMachine.configComponent.getConfig(transmission));
				to.configComponent.setEjecting(transmission, basicMachine.configComponent.isEjecting(transmission));
			}
			// Advanced machine gas
			if(from instanceof mekanism.common.tile.TileEntityAdvancedElectricMachine)
			{
				to.gasTank.setGas(((mekanism.common.tile.TileEntityAdvancedElectricMachine)from).gasTank.getGas());
			}
		}
		else
		{
			// TileEntityMetallurgicInfuser does not extend TileEntityBasicMachine
			// Handle its machine-like fields via reflection-style direct access
			if(from instanceof mekanism.common.tile.TileEntityMetallurgicInfuser)
			{
				mekanism.common.tile.TileEntityMetallurgicInfuser infuser = (mekanism.common.tile.TileEntityMetallurgicInfuser)from;
				to.progress[0] = infuser.operatingTicks;
				to.clientActive = infuser.clientActive;
				to.isActive = infuser.isActive;
				to.updateDelay = infuser.updateDelay;
				to.controlType = infuser.controlType;
				to.prevEnergy = infuser.prevEnergy;
				to.upgradeComponent.readFrom(infuser.upgradeComponent);
				to.upgradeComponent.setUpgradeSlot(0);
				to.ejectorComponent.readFrom(infuser.ejectorComponent);
				to.ejectorComponent.setOutputData(TransmissionType.ITEM, to.configComponent.getOutputs(TransmissionType.ITEM).get(2));
				to.securityComponent.readFrom(infuser.securityComponent);
				for(TransmissionType transmission : infuser.configComponent.transmissions)
				{
					to.configComponent.setConfig(transmission, infuser.configComponent.getConfig(transmission));
					to.configComponent.setEjecting(transmission, infuser.configComponent.isEjecting(transmission));
				}
				// infuser specific
				to.infuseStored.amount = infuser.infuseStored.amount;
				to.infuseStored.type = infuser.infuseStored.type;
			}
		}

		to.recipeType = type;
		to.upgradeComponent.setSupported(Upgrade.GAS, type.fuelEnergyUpgrades());

		for(Upgrade upgrade : to.upgradeComponent.getSupportedTypes())
		{
			to.recalculateUpgradables(upgrade);
		}
	}

	/**
	 * Copy Factory-specific fields for tier upgrades (BASIC→ADVANCED→ELITE).
	 * Handles progress array, recipeTicks, gas, sorting, etc. and inventory
	 * remapping based on tier process counts.
	 */
	public static void copyFactoryData(TileEntityFactory from, TileEntityFactory to)
	{
		to.facing = from.facing;
		to.clientFacing = from.clientFacing;
		to.ticker = from.ticker;
		to.redstone = from.redstone;
		to.redstoneLastTick = from.redstoneLastTick;
		to.doAutoSync = from.doAutoSync;
		to.electricityStored = from.electricityStored;
		to.soundURL = from.soundURL;

		for(int i = 0; i < from.tier.processes; i++)
		{
			to.progress[i] = from.progress[i];
		}
		to.recipeTicks = from.recipeTicks;
		to.clientActive = from.clientActive;
		to.isActive = from.isActive;
		to.updateDelay = from.updateDelay;
		to.prevEnergy = from.prevEnergy;
		to.gasTank.setGas(from.gasTank.getGas());
		to.infuseStored.amount = from.infuseStored.amount;
		to.infuseStored.type = from.infuseStored.type;
		to.sorting = from.sorting;
		to.controlType = from.controlType;
		to.upgradeComponent.readFrom(from.upgradeComponent);
		to.ejectorComponent.readFrom(from.ejectorComponent);
		to.configComponent.readFrom(from.configComponent);
		to.ejectorComponent.setOutputData(TransmissionType.ITEM, to.configComponent.getOutputs(TransmissionType.ITEM).get(2));
		to.recipeType = from.recipeType;
		to.upgradeComponent.setSupported(Upgrade.GAS, from.recipeType.fuelEnergyUpgrades());
		to.securityComponent.readFrom(from.securityComponent);

		// inventory base copy (first 5+processes slots)
		for(int i = 0; i < from.tier.processes+5; i++)
		{
			to.inventory[i] = from.inventory[i];
		}
		// output remapping
		for(int i = 0; i < from.tier.processes; i++)
		{
			int output = from.getOutputSlot(i);
			if(from.inventory[output] != null)
			{
				int newOutput = 5+to.tier.processes+i;
				to.inventory[newOutput] = from.inventory[output];
			}
		}
		for(Upgrade upgrade : to.upgradeComponent.getSupportedTypes())
		{
			to.recalculateUpgradables(upgrade);
		}
	}

	public static void finishUpgrade(World world, TileEntityFactory factory)
	{
		MachineType type;
		if(factory.tier == FactoryTier.BASIC) type = MachineType.BASIC_FACTORY;
		else if(factory.tier == FactoryTier.ADVANCED) type = MachineType.ADVANCED_FACTORY;
		else type = MachineType.ELITE_FACTORY;
		finishUpgrade(world, factory, type);
	}

	public static void finishUpgrade(World world, TileEntityFactory factory, MachineType expectedType)
	{
		factory.upgraded = true;
		factory.markDirty();
		Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(Coord4D.get(factory), factory.getNetworkedData(new ArrayList()), expectedType), new Range4D(Coord4D.get(factory)));
		world.notifyNeighborsOfStateChange(factory.getPos(), factory.getBlockType());
		MekanismUtils.updateBlock(world, factory.getPos());
	}
}