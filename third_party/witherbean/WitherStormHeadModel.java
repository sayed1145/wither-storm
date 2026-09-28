package net.witherbean.wwsm.client.model.wither_storm.head;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.witherbean.wwsm.animation.definitions.WitherStormHeadAnimationDefinitions;
import net.witherbean.wwsm.entity.wither_storm.head.WitherStormHead;

public class WitherStormHeadModel<T extends Entity> extends HierarchicalModel<T> {
        // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
        public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "wither_storm_head"), "main");
        private final ModelPart head;
        private final ModelPart root;

        public WitherStormHeadModel(ModelPart root) {
            this.head = root.getChild("head");
            this.root = root.getChild("head");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition meshdefinition = new MeshDefinition();
            PartDefinition partdefinition = meshdefinition.getRoot();

            PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 7.0F));

            PartDefinition top = head.addOrReplaceChild("top", CubeListBuilder.create().texOffs(101, 101).addBox(-4.9423F, -4.6346F, -11.8462F, 10.0F, 5.0F, 12.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(4.0577F, 0.3654F, -1.8462F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(4.0577F, 0.3654F, -3.8462F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(4.0577F, 0.3654F, -5.8462F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(4.0577F, 0.3654F, -7.8462F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(4.0577F, 0.3654F, -9.8462F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(4.0577F, 0.3654F, -11.8462F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(2.0577F, 0.3654F, -12.8462F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(1.0577F, 0.3654F, -13.8462F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-0.9423F, 0.3654F, -13.8462F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-2.9423F, 0.3654F, -11.8462F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-4.9423F, 0.3654F, -11.8462F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-4.9423F, 0.3654F, -9.8462F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-4.9423F, 0.3654F, -7.8462F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-4.9423F, 0.3654F, -5.8462F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-4.9423F, 0.3654F, -3.8462F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-4.9423F, 0.3654F, -1.8462F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-2.9423F, 0.3654F, -13.8462F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(45, 109).addBox(-2.9423F, -4.6346F, -13.8462F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                    .texOffs(45, 109).addBox(-2.9423F, -0.6346F, -13.8462F, 6.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                    .texOffs(137, 94).addBox(1.0577F, -2.6346F, -13.8462F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                    .texOffs(152, 156).addBox(-0.9423F, -2.6346F, -13.8462F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                    .texOffs(19, 146).addBox(-2.9423F, -2.6346F, -13.8462F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                    .texOffs(13, 123).addBox(-2.9423F, -6.6346F, -10.8462F, 6.0F, 2.0F, 11.0F, new CubeDeformation(0.0F))
                    .texOffs(45, 92).addBox(3.0577F, -6.6346F, -5.8462F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
                    .texOffs(36, 123).addBox(-4.9423F, -6.6346F, -5.8462F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.0577F, 1.6346F, -0.1538F));

            PartDefinition tractor_beam = top.addOrReplaceChild("tractor_beam", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

            PartDefinition beam = tractor_beam.addOrReplaceChild("beam", CubeListBuilder.create().texOffs(0, 4).addBox(0.0F, -19.0F, -36.0F, 0.0F, 35.0F, 36.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0577F, -1.6346F, -13.8462F));

            PartDefinition beam2 = tractor_beam.addOrReplaceChild("beam2", CubeListBuilder.create().texOffs(0, 4).addBox(0.0F, -19.0F, -36.0F, 0.0F, 35.0F, 36.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0577F, -1.6346F, -13.8462F));

            PartDefinition beam3 = tractor_beam.addOrReplaceChild("beam3", CubeListBuilder.create().texOffs(0, 4).addBox(0.0F, -19.0F, -36.0F, 0.0F, 35.0F, 36.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0577F, -1.6346F, -13.8462F));

            PartDefinition beam4 = tractor_beam.addOrReplaceChild("beam4", CubeListBuilder.create().texOffs(0, 4).addBox(0.0F, -19.0F, -36.0F, 0.0F, 35.0F, 36.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0577F, -1.6346F, -13.8462F));

            PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(13, 109).addBox(-4.9F, -0.15F, -11.95F, 10.0F, 2.0F, 12.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(4.1F, -1.15F, -0.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(4.1F, -1.15F, -2.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(4.1F, -1.15F, -4.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(4.1F, -1.15F, -6.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(4.1F, -1.15F, -8.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(4.1F, -1.15F, -10.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(2.1F, -1.15F, -11.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(2.1F, -1.15F, -13.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(0.1F, -1.15F, -13.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-1.9F, -1.15F, -13.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-2.9F, -1.15F, -12.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-3.9F, -1.15F, -11.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-4.9F, -1.15F, -10.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-4.9F, -1.15F, -8.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-4.9F, -1.15F, -6.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-4.9F, -1.15F, -4.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-4.9F, -1.15F, -2.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(0, 158).addBox(-4.9F, -1.15F, -0.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                    .texOffs(45, 100).addBox(-2.9F, -0.15F, -13.95F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.1F, 3.15F, -0.05F));

            return LayerDefinition.create(meshdefinition, 160, 160);
        }

        @Override
        public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            this.root().getAllParts().forEach(ModelPart::resetPose);
            this.applyHeadRotation(netHeadYaw, headPitch, ageInTicks);

            this.animate(((WitherStormHead) entity).idleAnimationState, WitherStormHeadAnimationDefinitions.HEAD_IDLE, ageInTicks, 1f);
            this.animate(((WitherStormHead) entity).attackAnimationState, WitherStormHeadAnimationDefinitions.HEAD_ATTACK, ageInTicks, 1f);
            this.animate(((WitherStormHead) entity).shootAnimationState, WitherStormHeadAnimationDefinitions.HEAD_SHOOT, ageInTicks, 1f);
//            this.animate(((WitherStormHead) entity).deathAnimationState, WitherStormHeadAnimationDefinitions.HEAD_DEATH, ageInTicks, 1f);
        }

        private void applyHeadRotation(float pNetHeadYaw, float pHeadPitch, float pAgeInTicks) {
            pNetHeadYaw = Mth.clamp(pNetHeadYaw, -30.0F, 30.0F);
            pHeadPitch = Mth.clamp(pHeadPitch, -25.0F, 45.0F);

            this.head.yRot = pNetHeadYaw * ((float)Math.PI / 180F);
            this.head.xRot = pHeadPitch * ((float)Math.PI / 180F);
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
            head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        }

        @Override
        public ModelPart root() {
            return head;
        }
    }
