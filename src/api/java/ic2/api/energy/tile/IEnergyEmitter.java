package ic2.api.energy.tile;

import net.minecraft.util.EnumFacing;

/**
 * For internal/multi-block usage only.
 *
 * @see IEnergySource
 * @see IEnergyConductor
 * 
 * See ic2/api/energy/usage.txt for an overall description of the energy net api.
 */
public interface IEnergyEmitter extends IEnergyTile {
	/**
	 * Determine if this emitter can emit energy to an adjacent receiver.
	 * 
	 * @param receiver receiver, may also be null or an IMetaDelegate
	 * @param direction direction the receiver is from the emitter
	 * @return Whether energy should be emitted
	 */
	boolean emitsEnergyTo(IEnergyAcceptor receiver, EnumFacing direction);
}

