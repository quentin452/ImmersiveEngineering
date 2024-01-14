package blusunrize.immersiveengineering.common.crafting;

import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.IEApi;
import blusunrize.immersiveengineering.api.crafting.ArcFurnaceRecipe;
import blusunrize.immersiveengineering.common.util.IELogger;
import blusunrize.immersiveengineering.common.util.Utils;

import java.util.*;

import com.google.common.collect.Sets;

public class ArcRecyclingThreadHandler
{
    static boolean hasProfiled = false;

    public static void doRecipeProfiling() {
        removeOldRecipes();

        long timestamp = System.currentTimeMillis();
        int threadAmount = Runtime.getRuntime().availableProcessors();
        RegistryIterationThread[] threads = createThreads(threadAmount);

        ArrayList<RecyclingCalculation> validated = new ArrayList<>();
        Map<ItemStack, List<RecyclingCalculation>> nonValidated = calculateThreadOutputs(threads, validated);

        validateNonValidated(validated, nonValidated);

        HashSet<String> finishedRecycles = new HashSet<>();
        addValidRecipes(validated, finishedRecycles);
        addInvalidRecipes(nonValidated, finishedRecycles);

        logFinishedProfiling(timestamp);
        hasProfiled = true;
    }

    private static void removeOldRecipes() {
        long r = ArcFurnaceRecipe.recipeList.stream()
            .filter(recipe -> "Recycling".equals(recipe.specialRecipeType))
            .count();

        ArcFurnaceRecipe.recipeList.removeIf(recipe -> "Recycling".equals(recipe.specialRecipeType));
        IELogger.info("Removed " + r + " old recipes");
    }

    private static RegistryIterationThread[] createThreads(int threadAmount) {
        final List<IRecipe> recipeList = CraftingManager.getInstance().getRecipeList();
        boolean divisable = recipeList.size() % threadAmount == 0;
        int limit = divisable ? (recipeList.size() / threadAmount) : (recipeList.size() / (threadAmount - 1));
        int leftOver = divisable ? limit : (recipeList.size() - (threadAmount - 1) * limit);
        RegistryIterationThread[] threads = new RegistryIterationThread[threadAmount];
        for (int i = 0; i < threadAmount; i++)
            threads[i] = new RegistryIterationThread(recipeList, limit * i, i == (threadAmount - 1) ? leftOver : limit);
        IELogger.info("Starting recipe profiler for Arc Recycling, using " + threadAmount + " Threads");
        return threads;
    }

    private static Map<ItemStack, List<RecyclingCalculation>> calculateThreadOutputs(
        RegistryIterationThread[] threads, ArrayList<RecyclingCalculation> validated) {
        Map<ItemStack, List<RecyclingCalculation>> nonValidated = new HashMap<>();

        for (RegistryIterationThread thread : threads) {
            try {
                thread.join();
                for (RecyclingCalculation calc : thread.calculatedOutputs) {
                    if (calc.isValid()) {
                        validated.add(calc);
                    } else {
                        nonValidated.computeIfAbsent(calc.stack, k -> new ArrayList<>()).add(calc);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return nonValidated;
    }

    private static void validateNonValidated(ArrayList<RecyclingCalculation> validated,
                                             Map<ItemStack, List<RecyclingCalculation>> nonValidated) {
        int timeout = 0;
        Set<ItemStack> newlyValidatedSubcomponents = new HashSet<>();

        while (!nonValidated.isEmpty() && timeout++ < (nonValidated.size() * 10)) {
            Set<ItemStack> toRemove = new HashSet<>();

            for (RecyclingCalculation valid : validated) {
                for (ItemStack subcomponent : Sets.difference(nonValidated.keySet(), newlyValidatedSubcomponents)) {
                    List<RecyclingCalculation> nonValid = nonValidated.get(subcomponent);
                    if (!nonValid.isEmpty()) {
                        RecyclingCalculation firstNonValid = nonValid.get(0);
                        if (OreDictionary.itemMatches(subcomponent, valid.stack, false)
                            && firstNonValid.validateSubcomponent(valid)) {
                            newlyValidatedSubcomponents.add(subcomponent);
                            toRemove.add(subcomponent);
                        }
                    }
                }
            }

            nonValidated.keySet().removeAll(toRemove);
        }
    }

    private static void addValidRecipes(ArrayList<RecyclingCalculation> validated, HashSet<String> finishedRecycles) {
        for (RecyclingCalculation valid : validated) {
            if (finishedRecycles.add(valid.stack.toString())) {
                ArcFurnaceRecipe.recipeList.add(new ArcRecyclingRecipe(valid.outputs, valid.stack, 100, 512));
            }
        }
    }

    private static void addInvalidRecipes(Map<ItemStack, List<RecyclingCalculation>> nonValidated,
                                          HashSet<String> finishedRecycles) {
        for (List<RecyclingCalculation> invalidList : nonValidated.values()) {
            for (RecyclingCalculation invalid : invalidList) {
                if (finishedRecycles.add(invalid.stack.toString())) {
                    IELogger.info("Couldn't fully analyze " + invalid.stack + ", missing knowledge for "
                        + invalid.queriedSubcomponents);
                    ArcFurnaceRecipe.recipeList.add(new ArcRecyclingRecipe(invalid.outputs, invalid.stack, 100, 512));
                }
            }
        }
    }

    private static void logFinishedProfiling(long timestamp) {
        IELogger.info("Finished recipe profiler for Arc Recycling, took " + (System.currentTimeMillis() - timestamp) + " milliseconds");
    }

    public static class RegistryIterationThread extends Thread
    {
        final List<IRecipe> recipeList;
        final int baseOffset;
        final int passes;
        ArrayList<RecyclingCalculation> calculatedOutputs = new ArrayList<>();

        public RegistryIterationThread(List<IRecipe> recipeList, int baseOffset, int passes)
        {
            setName("Immersive Engineering Registry Iteration Thread");
            setDaemon(true);
            start();
            this.recipeList = recipeList;
            this.baseOffset = baseOffset;
            this.passes = passes;
        }

        @Override
        public void run()
        {
            for(int pass=0; pass<passes; pass++)
            {
                IRecipe recipe = recipeList.get(baseOffset+pass);
                if(recipe.getRecipeOutput()!=null && isValidForRecycling(recipe.getRecipeOutput()))
                {
                    RecyclingCalculation calc = getRecycleCalculation(recipe.getRecipeOutput(), recipe);
                    if(calc!=null)
                        calculatedOutputs.add(calc);
                }
            }
        }
    }

    public static boolean isValidForRecycling(ItemStack stack)
    {
        if(stack==null)
            return false;
        Item item = stack.getItem();
        if(item instanceof ItemTool || item instanceof ItemSword || item instanceof ItemHoe || item instanceof ItemArmor)
            return true;
        for(Object recycle : ArcFurnaceRecipe.recyclingAllowed)
            if(Utils.stackMatchesObject(stack, recycle))
                return true;
        return false;
    }

    public static RecyclingCalculation getRecycleCalculation(ItemStack stack, IRecipe recipe)
    {
        Object[] inputs = null;
        if(recipe instanceof ShapedOreRecipe)
            inputs = ((ShapedOreRecipe)recipe).getInput();
        else if(recipe instanceof ShapelessOreRecipe)
            inputs = ((ShapelessOreRecipe)recipe).getInput().toArray();
        else if(recipe instanceof ShapedRecipes)
            inputs = ((ShapedRecipes)recipe).recipeItems;
        else if(recipe instanceof ShapelessRecipes)
            inputs = ((ShapelessRecipes)recipe).recipeItems.toArray();

        if(inputs!=null)
        {
            int inputSize = stack.stackSize;
            ArrayList<ItemStack> missingSub = new ArrayList<>();
            HashMap<ItemStack, Double> outputs = new HashMap<>();
            for(Object in : inputs)
                if (in != null) {
                    ItemStack inputStack = getInputStack(in);

                    if (inputStack == null) {
                        continue;
                    }

                    Object[] brokenDown = ApiUtils.breakStackIntoPreciseIngots(inputStack);
                    if (brokenDown == null) {
                        if (isValidForRecycling(inputStack)) {
                            missingSub.add(inputStack);
                        }
                        continue;
                    }

                    if (brokenDown[0] instanceof ItemStack && brokenDown[1] instanceof Double && (Double) brokenDown[1] > 0) {
                        ItemStack brokenDownStack = (ItemStack) brokenDown[0];
                        boolean invalidOutput = false;
                        for (Object invalid : ArcFurnaceRecipe.invalidRecyclingOutput) {
                            if (invalid instanceof ItemStack && Utils.stackMatchesObject(brokenDownStack, invalid)) {
                                invalidOutput = true;
                                break;
                            }
                        }
                        if (!invalidOutput) {
                            boolean found = outputs.keySet().stream().anyMatch(storedOut -> OreDictionary.itemMatches(brokenDownStack, storedOut, false));
                            if (found) {
                                outputs.merge(outputs.keySet().stream().filter(storedOut -> OreDictionary.itemMatches(brokenDownStack, storedOut, false)).findFirst().get(), (Double) brokenDown[1] / inputSize, Double::sum);
                            } else {
                                outputs.put(Utils.copyStackWithAmount(brokenDownStack, 1), (Double) brokenDown[1] / inputSize);
                            }
                        }
                    }
                }
            if (!outputs.isEmpty() || !missingSub.isEmpty()) {
                RecyclingCalculation calc = new RecyclingCalculation(recipe, Utils.copyStackWithAmount(stack, 1), outputs);
                calc.queriedSubcomponents.addAll(missingSub);
                return calc;
            }
        }
        return null;
    }

    private static ItemStack getInputStack(Object input) {
        if (input instanceof ItemStack) {
            return (ItemStack) input;
        } else if (input instanceof ArrayList) {
            ArrayList<?> inList = (ArrayList<?>) input;
            if (!inList.isEmpty() && inList.get(0) instanceof ItemStack) {
                return (ItemStack) inList.get(0);
            }
        } else if (input instanceof String) {
            return IEApi.getPreferredOreStack((String) input);
        }
        return null;
    }

    public static class RecyclingCalculation {
        IRecipe recipe;
        ItemStack stack;
        HashMap<ItemStack, Double> outputs;
        List<ItemStack> queriedSubcomponents;

        public RecyclingCalculation(IRecipe recipe, ItemStack stack, Map<ItemStack, Double> outputs) {
            this.recipe = recipe;
            this.stack = stack;
            this.outputs = new HashMap<>(outputs);
            this.queriedSubcomponents = new ArrayList<>();
        }

        public boolean isValid() {
            return outputs.isEmpty() && queriedSubcomponents.isEmpty();
        }

        public boolean validateSubcomponent(RecyclingCalculation calc) {
            if (isValid()) {
                return true;
            }

            if (!calc.isValid()) {
                return false;
            }

            Set<ItemStack> toRemove = new HashSet<>();
            for (ItemStack next : queriedSubcomponents) {
                if (OreDictionary.itemMatches(next, calc.stack, false)) {
                    calc.outputs.forEach((k, v) -> outputs.merge(k, v, Double::sum));
                    toRemove.add(next);
                }
            }

            queriedSubcomponents.removeAll(toRemove);
            return isValid();
        }
    }
}
