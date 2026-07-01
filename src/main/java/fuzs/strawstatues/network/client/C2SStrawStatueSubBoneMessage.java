package fuzs.strawstatues.network.client;

import fuzs.puzzlesapi.api.statues.v1.world.inventory.ArmorStandMenu;
import fuzs.puzzleslib.api.network.v2.MessageV2;
import fuzs.strawstatues.world.entity.decoration.StrawStatue;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

public class C2SStrawStatueSubBoneMessage implements MessageV2<C2SStrawStatueSubBoneMessage> {
    private SubBoneType type;
    private float value;

    public C2SStrawStatueSubBoneMessage() {

    }

    public C2SStrawStatueSubBoneMessage(SubBoneType type, float value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.type);
        buf.writeFloat(this.value);
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        this.type = buf.readEnum(SubBoneType.class);
        this.value = buf.readFloat();
    }

    @Override
    public MessageHandler<C2SStrawStatueSubBoneMessage> makeHandler() {
        return new MessageHandler<>() {

            @Override
            public void handle(C2SStrawStatueSubBoneMessage message, Player player, Object gameInstance) {
                if (player.containerMenu instanceof ArmorStandMenu menu && menu.stillValid(player)) {
                    message.type.consumer.accept((StrawStatue) menu.getArmorStand(), message.value);
                }
            }
        };
    }

    public enum SubBoneType {
        RIGHT_ELBOW(StrawStatue::setRightElbow),
        LEFT_ELBOW(StrawStatue::setLeftElbow),
        RIGHT_KNEE(StrawStatue::setRightKnee),
        LEFT_KNEE(StrawStatue::setLeftKnee);

        public final BiConsumer<StrawStatue, Float> consumer;

        SubBoneType(BiConsumer<StrawStatue, Float> consumer) {
            this.consumer = consumer;
        }
    }
}
