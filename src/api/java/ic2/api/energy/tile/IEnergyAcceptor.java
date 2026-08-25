package ic2.api.energy.tile;

import net.minecraft.util.EnumFacing;

/**
 * For internal/multi-block usage only.
 *
 * @see IEnergySink
 * @see IEnergyConductor
 * 
 * See ic2/api/energy/usage.txt for an overall description of the energy net api.
 */
public interface IEnergyAcceptor extends IEnergyTile {
	/**
	 * Determine if this acceptor can accept current from an adjacent emitter in a direction.
	 * 
	 * @param emitter energy emitter, may also be null or an IMetaDelegate
	 * @param direction direction the energy is being received from
	 */
	boolean acceptsEnergyFrom(IEnergyEmitter emitter, EnumFacing direction);
}

