package fuzs.strawstatues.world.item;

import fuzs.puzzlesapi.api.statues.v1.helper.ArmorStandInteractHelper;
import fuzs.strawstatues.init.ModRegistry;
import fuzs.strawstatues.world.entity.decoration.ImportedStrawStatue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class ImportedStrawStatueItem extends Item {

    public ImportedStrawStatueItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Direction direction = context.getClickedFace();
        if (direction == Direction.DOWN) {
            return InteractionResult.FAIL;
        } else {
            Level level = context.getLevel();
            BlockPlaceContext blockPlaceContext = new BlockPlaceContext(context);
            BlockPos blockPos = blockPlaceContext.getClickedPos();
            ItemStack itemStack = context.getItemInHand();
            Vec3 vec3 = Vec3.atBottomCenterOf(blockPos);
            AABB aABB = ModRegistry.IMPORTED_STRAW_STATUE_ENTITY_TYPE.get().getDimensions().makeBoundingBox(vec3.x(), vec3.y(), vec3.z());
            if (level.noCollision(null, aABB) && level.getEntities(null, aABB).isEmpty()) {
                if (level instanceof ServerLevel serverLevel) {
                    Player player = context.getPlayer();
                    Consumer<ImportedStrawStatue> consumer = EntityType.createDefaultStackConfig(serverLevel, itemStack, context.getPlayer());
                    ImportedStrawStatue statue = ModRegistry.IMPORTED_STRAW_STATUE_ENTITY_TYPE.get().create(serverLevel, itemStack.getTag(), consumer, blockPos, MobSpawnType.SPAWN_EGG, true, true);
                    if (statue == null) {
                        return InteractionResult.FAIL;
                    }
                    float f = (float) Mth.floor((Mth.wrapDegrees(context.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
                    statue.moveTo(statue.getX(), statue.getY(), statue.getZ(), f, 0.0F);
                    serverLevel.addFreshEntityWithPassengers(statue);
                    level.playSound(null, statue.getX(), statue.getY(), statue.getZ(), SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
                    statue.gameEvent(GameEvent.ENTITY_PLACE, player);
                }
                itemStack.shrink(1);
                return InteractionResult.sidedSuccess(level.isClientSide);
            } else {
                return InteractionResult.FAIL;
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        tooltipComponents.add(ArmorStandInteractHelper.getArmorStandHoverText());
    }
}
