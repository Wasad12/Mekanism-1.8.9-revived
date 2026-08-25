package mekanism.common.base;
import java.util.EnumSet;

import mekanism.api.energy.ICableOutputter;
import mekanism.api.energy.IStrictEnergyAcceptor;
import mekanism.api.energy.IStrictEnergyStorage;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.Optional.Interface;
import net.minecraftforge.fml.common.Optional.InterfaceList;
import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyReceiver;
import cofh.api.energy.IEnergyProvider;

public interface IEnergyWrapper extends IStrictEnergyStorage, IEnergyHandler, IEnergyReceiver, IEnergyProvider, IStrictEnergyAcceptor, ICableOutputter, IInventory
{
	public EnumSet<EnumFacing> getOutputtingSides();

	public EnumSet<EnumFacing> getConsumingSides();

	public double getMaxOutput();
}
