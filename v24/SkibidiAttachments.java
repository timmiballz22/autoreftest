package dev.ashenthefox.skibidi.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public final class SkibidiAttachments {
    private static final float G2_LASER_X = 0.52F;
    private static final float G2_LASER_Y = 0.90F;
    private static final float G2_LASER_Z = -0.44F;

    private static final float[][] G3_LASER_ROWS = {
        {0.60F, 1.05F, -0.44F},
        {0.46F, 0.70F, -0.40F},
        {0.46F, 0.50F, -0.42F}
    };

    private SkibidiAttachments() {
    }

    public static void renderMetal(
        SkibidiVariant variant,
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float headYaw,
        float headPitch
    ) {
        if (variant == SkibidiVariant.G_TOILET_2) {
            renderLaserBodies(2, pose, buffer, light, overlay, scale, headYaw, headPitch);
        } else if (variant == SkibidiVariant.G_TOILET_3) {
            renderLaserBodies(6, pose, buffer, light, overlay, scale, headYaw, headPitch);
            renderHeadphones(pose, buffer, light, overlay, scale, headYaw, headPitch);
            renderJetpack(pose, buffer, light, overlay, scale, headYaw, headPitch);
            renderEarphone(pose, buffer, light, overlay, scale, headYaw, headPitch);
        }
    }

    public static void renderDark(
        SkibidiVariant variant,
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float headYaw,
        float headPitch
    ) {
        if (variant != SkibidiVariant.G_TOILET_3) {
            return;
        }

        box(pose, buffer, light, overlay, scale, headYaw, headPitch, true,
            0.0F, 1.92F, 0.02F, 0.78F, 0.10F, 0.16F);
        box(pose, buffer, light, overlay, scale, headYaw, headPitch, true,
            0.39F, 1.58F, 0.01F, 0.12F, 0.42F, 0.22F);
        box(pose, buffer, light, overlay, scale, headYaw, headPitch, true,
            -0.39F, 1.58F, 0.01F, 0.12F, 0.42F, 0.22F);
        box(pose, buffer, light, overlay, scale, headYaw, headPitch, false,
            0.0F, 0.92F, 0.48F, 0.54F, 0.82F, 0.18F);
    }

    public static void renderYellow(
        SkibidiVariant variant,
        boolean laserFiring,
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float headYaw,
        float headPitch
    ) {
        if (variant.hasGlowingEyes()) {
            box(pose, buffer, light, overlay, scale, headYaw, headPitch, true,
                -0.16F, 1.69F, -0.345F, 0.13F, 0.10F, 0.055F);
            box(pose, buffer, light, overlay, scale, headYaw, headPitch, true,
                0.16F, 1.69F, -0.345F, 0.13F, 0.10F, 0.055F);
        }

        if (variant == SkibidiVariant.G_TOILET) {
            if (laserFiring) {
                eyeBeam(-0.16F, 1.69F, pose, buffer, light, overlay, scale, headYaw, headPitch);
                eyeBeam(0.16F, 1.69F, pose, buffer, light, overlay, scale, headYaw, headPitch);
            }
            return;
        }

        if (variant == SkibidiVariant.G_TOILET_2) {
            renderLaserEmitters(2, pose, buffer, light, overlay, scale, headYaw, headPitch);
            box(pose, buffer, light, overlay, scale, headYaw, headPitch, true,
                0.0F, 1.78F, -0.345F, 0.11F, 0.11F, 0.06F);

            if (laserFiring) {
                renderLaserBeams(2, pose, buffer, light, overlay, scale, headYaw, headPitch);
                eyeBeam(0.0F, 1.78F, pose, buffer, light, overlay, scale, headYaw, headPitch);
            }
            return;
        }

        if (variant == SkibidiVariant.G_TOILET_3) {
            renderLaserEmitters(6, pose, buffer, light, overlay, scale, headYaw, headPitch);

            box(pose, buffer, light, overlay, scale, headYaw, headPitch, false,
                0.28F, 0.40F, 0.61F, 0.18F, 0.12F, 0.08F);
            box(pose, buffer, light, overlay, scale, headYaw, headPitch, false,
                -0.28F, 0.40F, 0.61F, 0.18F, 0.12F, 0.08F);

            if (laserFiring) {
                renderLaserBeams(6, pose, buffer, light, overlay, scale, headYaw, headPitch);
            }
        }
    }

    private static void renderLaserBodies(
        int count,
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float yaw,
        float pitch
    ) {
        if (count == 2) {
            laserBody(-G2_LASER_X, G2_LASER_Y, G2_LASER_Z,
                pose, buffer, light, overlay, scale, yaw, pitch);
            laserBody(G2_LASER_X, G2_LASER_Y, G2_LASER_Z,
                pose, buffer, light, overlay, scale, yaw, pitch);
            return;
        }

        for (float[] row : G3_LASER_ROWS) {
            laserBody(-row[0], row[1], row[2],
                pose, buffer, light, overlay, scale, yaw, pitch);
            laserBody(row[0], row[1], row[2],
                pose, buffer, light, overlay, scale, yaw, pitch);
        }
    }

    private static void renderLaserEmitters(
        int count,
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float yaw,
        float pitch
    ) {
        if (count == 2) {
            emitter(-G2_LASER_X, G2_LASER_Y, G2_LASER_Z,
                pose, buffer, light, overlay, scale, yaw, pitch);
            emitter(G2_LASER_X, G2_LASER_Y, G2_LASER_Z,
                pose, buffer, light, overlay, scale, yaw, pitch);
            return;
        }

        for (float[] row : G3_LASER_ROWS) {
            emitter(-row[0], row[1], row[2],
                pose, buffer, light, overlay, scale, yaw, pitch);
            emitter(row[0], row[1], row[2],
                pose, buffer, light, overlay, scale, yaw, pitch);
        }
    }

    private static void renderLaserBeams(
        int count,
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float yaw,
        float pitch
    ) {
        if (count == 2) {
            beam(-G2_LASER_X, G2_LASER_Y, G2_LASER_Z,
                pose, buffer, light, overlay, scale, yaw, pitch);
            beam(G2_LASER_X, G2_LASER_Y, G2_LASER_Z,
                pose, buffer, light, overlay, scale, yaw, pitch);
            return;
        }

        for (float[] row : G3_LASER_ROWS) {
            beam(-row[0], row[1], row[2],
                pose, buffer, light, overlay, scale, yaw, pitch);
            beam(row[0], row[1], row[2],
                pose, buffer, light, overlay, scale, yaw, pitch);
        }
    }

    private static void beam(
        float x,
        float y,
        float z,
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float yaw,
        float pitch
    ) {
        box(pose, buffer, light, overlay, scale, yaw, pitch, false,
            x, y, z - 8.52F, 0.065F, 0.065F, 15.30F);
    }

    private static void eyeBeam(
        float x,
        float y,
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float yaw,
        float pitch
    ) {
        box(pose, buffer, light, overlay, scale, yaw, pitch, true,
            x, y, -8.20F, 0.055F, 0.055F, 15.60F);
    }

    private static void laserBody(
        float x,
        float y,
        float z,
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float yaw,
        float pitch
    ) {
        box(pose, buffer, light, overlay, scale, yaw, pitch, false,
            x, y, z, 0.20F, 0.22F, 0.22F);
        box(pose, buffer, light, overlay, scale, yaw, pitch, false,
            x, y, z - 0.44F, 0.13F, 0.13F, 0.72F);
    }

    private static void emitter(
        float x,
        float y,
        float z,
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float yaw,
        float pitch
    ) {
        box(pose, buffer, light, overlay, scale, yaw, pitch, false,
            x, y, z - 0.87F, 0.10F, 0.10F, 0.14F);
    }

    private static void renderHeadphones(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float yaw,
        float pitch
    ) {
        box(pose, buffer, light, overlay, scale, yaw, pitch, true,
            0.41F, 1.58F, 0.01F, 0.16F, 0.34F, 0.26F);
        box(pose, buffer, light, overlay, scale, yaw, pitch, true,
            -0.41F, 1.58F, 0.01F, 0.16F, 0.34F, 0.26F);
    }

    private static void renderJetpack(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float yaw,
        float pitch
    ) {
        box(pose, buffer, light, overlay, scale, yaw, pitch, false,
            0.28F, 0.88F, 0.58F, 0.22F, 0.92F, 0.25F);
        box(pose, buffer, light, overlay, scale, yaw, pitch, false,
            -0.28F, 0.88F, 0.58F, 0.22F, 0.92F, 0.25F);
    }

    private static void renderEarphone(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float yaw,
        float pitch
    ) {
        box(pose, buffer, light, overlay, scale, yaw, pitch, true,
            -0.43F, 1.53F, -0.03F, 0.08F, 0.17F, 0.14F);
        box(pose, buffer, light, overlay, scale, yaw, pitch, true,
            -0.39F, 1.43F, -0.22F, 0.055F, 0.055F, 0.34F);
    }

    private static void box(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float yaw,
        float pitch,
        boolean head,
        float cx,
        float cy,
        float cz,
        float sx,
        float sy,
        float sz
    ) {
        float x0 = cx - sx * 0.5F;
        float x1 = cx + sx * 0.5F;
        float y0 = cy - sy * 0.5F;
        float y1 = cy + sy * 0.5F;
        float z0 = cz - sz * 0.5F;
        float z1 = cz + sz * 0.5F;

        face(pose, buffer, light, overlay, scale, yaw, pitch, head,
            x0,y0,z0, x1,y0,z0, x1,y1,z0, x0,y1,z0, 0,0,-1);
        face(pose, buffer, light, overlay, scale, yaw, pitch, head,
            x1,y0,z1, x0,y0,z1, x0,y1,z1, x1,y1,z1, 0,0,1);
        face(pose, buffer, light, overlay, scale, yaw, pitch, head,
            x0,y0,z1, x0,y0,z0, x0,y1,z0, x0,y1,z1, -1,0,0);
        face(pose, buffer, light, overlay, scale, yaw, pitch, head,
            x1,y0,z0, x1,y0,z1, x1,y1,z1, x1,y1,z0, 1,0,0);
        face(pose, buffer, light, overlay, scale, yaw, pitch, head,
            x0,y1,z0, x1,y1,z0, x1,y1,z1, x0,y1,z1, 0,1,0);
        face(pose, buffer, light, overlay, scale, yaw, pitch, head,
            x0,y0,z1, x1,y0,z1, x1,y0,z0, x0,y0,z0, 0,-1,0);
    }

    /*
     * Entity cutout rendering is QUADS in Minecraft 1.21.11.
     * Emit one real four-vertex quad per box face. The previous six-vertex
     * triangle stream crossed face boundaries and visually mangled lasers,
     * headphones and jetpack geometry for the same reason as the main glTF.
     */
    private static void face(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float yaw,
        float pitch,
        boolean head,
        float ax,float ay,float az,
        float bx,float by,float bz,
        float cx,float cy,float cz,
        float dx,float dy,float dz,
        float nx,float ny,float nz
    ) {
        vertex(pose,buffer,light,overlay,scale,yaw,pitch,head,ax,ay,az,0,0,nx,ny,nz);
        vertex(pose,buffer,light,overlay,scale,yaw,pitch,head,bx,by,bz,1,0,nx,ny,nz);
        vertex(pose,buffer,light,overlay,scale,yaw,pitch,head,cx,cy,cz,1,1,nx,ny,nz);
        vertex(pose,buffer,light,overlay,scale,yaw,pitch,head,dx,dy,dz,0,1,nx,ny,nz);
    }

    private static void vertex(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float yaw,
        float pitch,
        boolean head,
        float x,float y,float z,
        float u,float v,
        float nx,float ny,float nz
    ) {
        SkibidiMesh.emitVertex(
            pose, buffer, light, overlay, scale, yaw, pitch, head,
            x, y, z, u, v, nx, ny, nz
        );
    }
}
