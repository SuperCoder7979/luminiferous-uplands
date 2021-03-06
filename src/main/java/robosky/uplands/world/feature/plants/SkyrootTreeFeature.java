package robosky.uplands.world.feature.plants;

import com.mojang.datafixers.Dynamic;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockBox;
import net.minecraft.world.ModifiableTestableWorld;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import robosky.uplands.block.BlockRegistry;

import java.util.Random;
import java.util.Set;
import java.util.function.Function;

public class SkyrootTreeFeature extends AbstractUplandsTree<DefaultFeatureConfig> {
    private final int height;
    private final BlockState log;
    private final BlockState wood;
    private BlockState leaves;

    public SkyrootTreeFeature(Function<Dynamic<?>, ? extends DefaultFeatureConfig> deserialize, boolean sapling) {
        this(deserialize,
                sapling,
                BlockRegistry.SKYROOT_LOG().getDefaultState(),
                BlockRegistry.SKYROOT_WOOD().getDefaultState());
    }

    private SkyrootTreeFeature(Function<Dynamic<?>, ? extends DefaultFeatureConfig> dezerialize, boolean sapling,
                               BlockState log, BlockState wood) {
        super(dezerialize, sapling);
        this.height = 4;
        this.log = log;
        this.wood = wood;
    }

    public boolean generate(Set<BlockPos> set, ModifiableTestableWorld world, Random rand,
                            BlockPos startPos, BlockBox bbox) {
        // Leaves have to be obtained in generate because we need access to the RNG
        float randomLeaves = rand.nextFloat();

        // In order of rarity, leaf colors are: Orange, Red, Yellow
        if (randomLeaves < 0.8f) {
            if (randomLeaves > 0.45) {
                leaves = BlockRegistry.RED_SKYROOT_LEAVES().getDefaultState();
            } else {
                leaves = BlockRegistry.ORANGE_SKYROOT_LEAVES().getDefaultState();
            }
        } else {
            leaves = BlockRegistry.YELLOW_SKYROOT_LEAVES().getDefaultState();
        }

        // Create variables related to the tree's trunk

        // The number of segments this tree will generate with, from 1 to 5.
        int numberOfSegments = (rand.nextInt(4) + 1);

        // The number of mushrooms around the base, from 0 to 2
        int numberOfMushrooms;
        float randomMushrooms = rand.nextFloat();
        if (randomMushrooms < 0.7) {
            numberOfMushrooms = 0;
        } else {
            if (randomMushrooms < 0.78) {
                numberOfMushrooms = 2;
            } else {
                numberOfMushrooms = 1;
            }
        }

        // The height of the individual segments.
        int[] segmentHeights = new int[numberOfSegments];

        // The total height of all the segments, used to make sure the tree doesn't try to grow over the sky limit
        int totalHeightOfTrunk = 0;

        // The direction the trunk will bend when the next segment begins.
        Direction[] segmentBendDirections = new Direction[numberOfSegments];

        for (int i = 0; i <= numberOfSegments - 1; i++) {
            int treeHeight = height + rand.nextInt(3);

            totalHeightOfTrunk += treeHeight;
            segmentHeights[i] = treeHeight;
            segmentBendDirections[i] = Direction.fromHorizontal(rand.nextInt(4));

            // Last segment needs to be taller to be sure there's enough room for the leaves.
            if (i == numberOfSegments - 1) {
                totalHeightOfTrunk += 3;
                segmentHeights[i] += 3;
            }
        }


        // If the tree is too close to the sky limit, bail out.
        // +2 to account for the extra height of the leaves
        if (startPos.getY() + totalHeightOfTrunk + 2 >= 256) {
            return false;
        }

        // If the spot directly below is grass, turn it to dirt.
        // Otherwise stop, or else trees can generate in the void.
        if (isDirtOrGrass(world, startPos.down())) {
            setToDirt(world, startPos.down());
        } else {
            return false;
        }

        // currentPos will represent the current trunk log being placed.
        BlockPos currentPos = startPos;

        // mushrooms yo
        for (int i = 0; i < numberOfMushrooms; i++) {
            Direction mushroomPlacement = Direction.fromHorizontal(rand.nextInt());
            BlockPos mushroomLocation = currentPos.add(mushroomPlacement.getVector());

            if (canPlaceBlock(world, mushroomLocation)) {
                if (!canPlaceBlock(world, mushroomLocation.down())) {
                    setBlockState(set, world, mushroomLocation, BlockRegistry.AZOTE_MUSHROOM().getDefaultState(), bbox);
                }
            }
        }

        //Start on the tree

        // For every segment,
        for (int i = 0; i <= numberOfSegments - 1; i++) {

            // For every block of that segment,
            for (int j = 0; j < segmentHeights[i]; j++) {
                // Check if the block position is empty, quit if it's not.
                // This is in case someone tries to grow a tree beneath a roof or something.
                if (!canPlaceBlock(world, currentPos)) {
                    return true;
                }

                // Check if this trunk is either the top or the bottom of a segment.
                // If this is the bottom of the first segment, it's the bit that connects the tree to the ground;
                // use a log there.
                boolean isTopOrBottom = (j == 0) || (j == segmentHeights[i] - 1) && (startPos != currentPos);

                // Place either a bark or log block there depending on whether or not it's a top or a bottom.
                setBlockState(set, world, currentPos, isTopOrBottom ? wood : log, bbox);

                // Increment the position.
                currentPos = currentPos.add(0, 1, 0);
            }

            // If that wasn't the last segment, add the bend direction too.
            // If this were done on the last segment, it'd cause the leaves to be placed incorrectly.
            if (i != numberOfSegments - 1) {
                currentPos = currentPos.add(segmentBendDirections[i].getVector());
            }
        }

        int radius;

        if (numberOfSegments == 1) {
            radius = 3;
        } else {
            if (numberOfSegments == 5) {
                radius = 6;
            } else {
                radius = 4;
            }
        }

        // Move the current position down half the radius to start on the leaves.
        currentPos = currentPos.add(0, -(radius / 2), 0);

        int radiusSquared = radius * radius;

        for(int y = -radius; y <= radius; y++) {
            for(int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    int squareDistance = (x * x) + (y * y) + (z * z);

                    if (squareDistance <= radiusSquared) {
                        if (isAirOrLeaves(world, currentPos.add(x, y, z))) {
                            setBlockState(set, world, currentPos.add(x, y, z), leaves, bbox);
                        }
                    }
                }
            }
        }

        return true;
    }

    private boolean canPlaceBlock(TestableWorld world, BlockPos blockPosition) {
        if (!isAirOrLeaves(world, blockPosition)) {
            return false;
        }

        return canTreeReplace(world, blockPosition);
    }
}
