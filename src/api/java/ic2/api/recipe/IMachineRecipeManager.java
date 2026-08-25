package ic2.api.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Recipe manager interface for basic machines.
 *
 * @author RichardG, Player
 */
public interface IMachineRecipeManager {
	/**
	 * Inner class for iterating recipes.
	 */
	public static class RecipeIoContainer {
		public final IRecipeInput input;
		public final RecipeOutput output;

		public RecipeIoContainer(IRecipeInput input, RecipeOutput output) {
			this.input = input;
			this.output = output;
		}
	}

	/**
	 * Adds a recipe to the machine.
	 *
	 * @param input Recipe input
	 * @param metadata meta data for additional recipe properties, may be null
	 * @param overwrite Replace an existing recipe, not recommended, may be ignored.
	 * @param outputs Recipe outputs, zero or more depending on the machine
	 */
	public boolean addRecipe(IRecipeInput input, NBTTagCompound metadata, boolean overwrite, ItemStack... outputs);

	/**
	 * Gets the recipe output for the given input.
	 *
	 * @param input Recipe input
	 * @param adjustInput modify the input according to the recipe's requirements
	 * @return Recipe output, or null if none
	 */
	public RecipeOutput getOutputFor(ItemStack input, boolean adjustInput);

	/**
	 * Gets a list of recipes.
	 *
	 * You're a mad evil scientist if you ever modify this.
	 *
	 * @return Iterable of recipes
	 */
	public Iterable<RecipeIoContainer> getRecipes();

	/**
	 * Whether this recipe manager is iterable.
	 *
	 * @return true if getRecipes() can be used
	 */
	public boolean isIterable();
}
