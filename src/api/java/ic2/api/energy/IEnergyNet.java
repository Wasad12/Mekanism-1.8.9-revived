package ic2.api.energy;

import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import ic2.api.energy.tile.IEnergyTile;

/**
 * Interface representing the methods provided by the global EnergyNet class.
 * 
 * See ic2/api/energy/usage.txt for an overall description of the energy net api.
 */
public interface IEnergyNet {
	/**
	 * Get the EnergyNet-registered tile entity at the specified position.
	 * 
	 * @param world World containing the tile entity
	 * @param pos block position
	 * @return tile entity registered to the energy net or null if none is registered
	 */
	IEnergyTile getTile(World world, BlockPos pos);

	/**
	 * Get the EnergyNet-registered sub tile entity at the specified position.
	 * 
	 * @param world World containing the tile entity
	 * @param pos block position
	 * @return sub tile entity registered to the energy net or null if none is registered
	 */
	IEnergyTile getSubTile(World world, BlockPos pos);

	/**
	 * Get the world of the specified tile entity.
	 * 
	 * @param te tile entity
	 * @return the world
	 */
	World getWorld(IEnergyTile te);

	/**
	 * Get the position of the specified tile entity.
	 * 
	 * @param te tile entity
	 * @return the position
	 */
	BlockPos getPos(IEnergyTile te);

	/**
	 * Retrieve statistics for the tile entity specified.
	 * 
	 * The statistics apply to the last simulated tick.
	 * 
	 * @param te Tile entity to check.
	 * @return Statistics for the tile entity.
	 */
	NodeStats getNodeStats(IEnergyTile te);

	/**
	 * Determine the typical power used by the specific tier, e.g. 128 eu/t for tier 2.
	 * 
	 * @param tier tier
	 * @return power in eu/t
	 */
	double getPowerFromTier(int tier);

	/**
	 * Determine minimum tier required to handle the specified power, e.g. tier 2 for 128 eu/t.
	 * 
	 * @param power in eu/t
	 * @return tier
	 */
	int getTierFromPower(double power);
}
