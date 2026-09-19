package dev.ashenthefox.skibidi.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public final class SkibidiAttachments {
    private SkibidiAttachments() {}

    public static void renderMetal(SkibidiVariant variant, PoseStack.Pose pose, VertexConsumer buffer,
        int light, int overlay, float scale, float headYaw, float headPitch) {
        if (variant == SkibidiVariant.G_TOILET_2) {
            renderLaserBodies(2, pose, buffer, light, overlay, scale, headYaw, headPitch);
        } else if (variant == SkibidiVariant.G_TOILET_3) {
            renderLaserBodies(6, pose, buffer, light, overlay, scale, headYaw, headPitch);
            renderHeadphones(pose, buffer, light, overlay, scale, headYaw, headPitch);
            renderJetpack(pose, buffer, light, overlay, scale, headYaw, headPitch);
            renderEarphone(pose, buffer, light, overlay, scale, headYaw, headPitch);
        }
    }

    public static void renderDark(SkibidiVariant variant, PoseStack.Pose pose, VertexConsumer buffer,
        int light, int overlay, float scale, float headYaw, float headPitch) {
        if (variant != SkibidiVariant.G_TOILET_3) return;
        box(pose, buffer, light, overlay, scale, headYaw, headPitch, true, 0F,1.92F,.02F,.78F,.10F,.16F);
        box(pose, buffer, light, overlay, scale, headYaw, headPitch, true, .39F,1.58F,.01F,.12F,.42F,.22F);
        box(pose, buffer, light, overlay, scale, headYaw, headPitch, true, -.39F,1.58F,.01F,.12F,.42F,.22F);
        box(pose, buffer, light, overlay, scale, headYaw, headPitch, false, 0F,.92F,.48F,.54F,.82F,.18F);
    }

    public static void renderYellow(SkibidiVariant variant, PoseStack.Pose pose, VertexConsumer buffer,
        int light, int overlay, float scale, float headYaw, float headPitch) {
        if (variant.hasGlowingEyes()) {
            box(pose, buffer, light, overlay, scale, headYaw, headPitch, true, -.16F,1.69F,-.345F,.13F,.10F,.055F);
            box(pose, buffer, light, overlay, scale, headYaw, headPitch, true, .16F,1.69F,-.345F,.13F,.10F,.055F);
        }
        if (variant == SkibidiVariant.G_TOILET_2) {
            renderLaserEmitters(2, pose, buffer, light, overlay, scale, headYaw, headPitch);
            renderLaserBeams(2, pose, buffer, light, overlay, scale, headYaw, headPitch);
            box(pose, buffer, light, overlay, scale, headYaw, headPitch, true, 0F,1.78F,-.345F,.11F,.11F,.06F);
        } else if (variant == SkibidiVariant.G_TOILET_3) {
            renderLaserEmitters(6, pose, buffer, light, overlay, scale, headYaw, headPitch);
            renderLaserBeams(6, pose, buffer, light, overlay, scale, headYaw, headPitch);
            box(pose, buffer, light, overlay, scale, headYaw, headPitch, false, .28F,.40F,.61F,.18F,.12F,.08F);
            box(pose, buffer, light, overlay, scale, headYaw, headPitch, false, -.28F,.40F,.61F,.18F,.12F,.08F);
        }
    }

    private static void renderLaserBodies(int count, PoseStack.Pose pose, VertexConsumer buffer,
        int light, int overlay, float scale, float yaw, float pitch) {
        if (count == 2) {
            laserBody(-.48F,1.43F,pose,buffer,light,overlay,scale,yaw,pitch);
            laserBody(.48F,1.43F,pose,buffer,light,overlay,scale,yaw,pitch);
            return;
        }
        float[] ys={1.82F,1.45F,1.08F};
        for(float y:ys){ laserBody(-.55F,y,pose,buffer,light,overlay,scale,yaw,pitch); laserBody(.55F,y,pose,buffer,light,overlay,scale,yaw,pitch); }
    }

    private static void renderLaserEmitters(int count, PoseStack.Pose pose, VertexConsumer buffer,
        int light, int overlay, float scale, float yaw, float pitch) {
        if(count==2){ emitter(-.48F,1.43F,pose,buffer,light,overlay,scale,yaw,pitch); emitter(.48F,1.43F,pose,buffer,light,overlay,scale,yaw,pitch); return; }
        float[] ys={1.82F,1.45F,1.08F};
        for(float y:ys){ emitter(-.55F,y,pose,buffer,light,overlay,scale,yaw,pitch); emitter(.55F,y,pose,buffer,light,overlay,scale,yaw,pitch); }
    }

    private static void renderLaserBeams(int count, PoseStack.Pose pose, VertexConsumer buffer,
        int light, int overlay, float scale, float yaw, float pitch) {
        if(count==2){ beam(-.48F,1.43F,pose,buffer,light,overlay,scale,yaw,pitch); beam(.48F,1.43F,pose,buffer,light,overlay,scale,yaw,pitch); return; }
        float[] ys={1.82F,1.45F,1.08F};
        for(float y:ys){ beam(-.55F,y,pose,buffer,light,overlay,scale,yaw,pitch); beam(.55F,y,pose,buffer,light,overlay,scale,yaw,pitch); }
    }

    private static void laserBody(float x,float y,PoseStack.Pose pose,VertexConsumer buffer,int light,int overlay,float scale,float yaw,float pitch){
        box(pose,buffer,light,overlay,scale,yaw,pitch,false,x,y,-.03F,.20F,.22F,.22F);
        box(pose,buffer,light,overlay,scale,yaw,pitch,false,x,y,-.38F,.13F,.13F,.62F);
    }
    private static void emitter(float x,float y,PoseStack.Pose pose,VertexConsumer buffer,int light,int overlay,float scale,float yaw,float pitch){
        box(pose,buffer,light,overlay,scale,yaw,pitch,false,x,y,-.72F,.10F,.10F,.12F);
    }
    private static void beam(float x,float y,PoseStack.Pose pose,VertexConsumer buffer,int light,int overlay,float scale,float yaw,float pitch){
        box(pose,buffer,light,overlay,scale,yaw,pitch,false,x,y,-1.10F,.055F,.055F,.72F);
    }

    private static void renderHeadphones(PoseStack.Pose pose,VertexConsumer buffer,int light,int overlay,float scale,float yaw,float pitch){
        box(pose,buffer,light,overlay,scale,yaw,pitch,true,.41F,1.58F,.01F,.16F,.34F,.26F);
        box(pose,buffer,light,overlay,scale,yaw,pitch,true,-.41F,1.58F,.01F,.16F,.34F,.26F);
    }
    private static void renderJetpack(PoseStack.Pose pose,VertexConsumer buffer,int light,int overlay,float scale,float yaw,float pitch){
        box(pose,buffer,light,overlay,scale,yaw,pitch,false,.28F,.88F,.58F,.22F,.92F,.25F);
        box(pose,buffer,light,overlay,scale,yaw,pitch,false,-.28F,.88F,.58F,.22F,.92F,.25F);
    }
    private static void renderEarphone(PoseStack.Pose pose,VertexConsumer buffer,int light,int overlay,float scale,float yaw,float pitch){
        box(pose,buffer,light,overlay,scale,yaw,pitch,true,-.43F,1.53F,-.03F,.08F,.17F,.14F);
        box(pose,buffer,light,overlay,scale,yaw,pitch,true,-.39F,1.43F,-.22F,.055F,.055F,.34F);
    }

    private static void box(PoseStack.Pose pose,VertexConsumer buffer,int light,int overlay,float scale,float yaw,float pitch,boolean head,
        float cx,float cy,float cz,float sx,float sy,float sz){
        float x0=cx-sx*.5F,x1=cx+sx*.5F,y0=cy-sy*.5F,y1=cy+sy*.5F,z0=cz-sz*.5F,z1=cz+sz*.5F;
        face(pose,buffer,light,overlay,scale,yaw,pitch,head,x0,y0,z0,x1,y0,z0,x1,y1,z0,x0,y1,z0,0,0,-1);
        face(pose,buffer,light,overlay,scale,yaw,pitch,head,x1,y0,z1,x0,y0,z1,x0,y1,z1,x1,y1,z1,0,0,1);
        face(pose,buffer,light,overlay,scale,yaw,pitch,head,x0,y0,z1,x0,y0,z0,x0,y1,z0,x0,y1,z1,-1,0,0);
        face(pose,buffer,light,overlay,scale,yaw,pitch,head,x1,y0,z0,x1,y0,z1,x1,y1,z1,x1,y1,z0,1,0,0);
        face(pose,buffer,light,overlay,scale,yaw,pitch,head,x0,y1,z0,x1,y1,z0,x1,y1,z1,x0,y1,z1,0,1,0);
        face(pose,buffer,light,overlay,scale,yaw,pitch,head,x0,y0,z1,x1,y0,z1,x1,y0,z0,x0,y0,z0,0,-1,0);
    }

    private static void face(PoseStack.Pose pose,VertexConsumer b,int l,int o,float s,float y,float p,boolean h,
        float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz,float nx,float ny,float nz){
        v(pose,b,l,o,s,y,p,h,ax,ay,az,0,0,nx,ny,nz); v(pose,b,l,o,s,y,p,h,bx,by,bz,1,0,nx,ny,nz); v(pose,b,l,o,s,y,p,h,cx,cy,cz,1,1,nx,ny,nz);
        v(pose,b,l,o,s,y,p,h,ax,ay,az,0,0,nx,ny,nz); v(pose,b,l,o,s,y,p,h,cx,cy,cz,1,1,nx,ny,nz); v(pose,b,l,o,s,y,p,h,dx,dy,dz,0,1,nx,ny,nz);
    }
    private static void v(PoseStack.Pose pose,VertexConsumer b,int l,int o,float s,float y,float p,boolean h,float x,float yy,float z,float u,float vv,float nx,float ny,float nz){
        SkibidiMesh.emitVertex(pose,b,l,o,s,y,p,h,x,yy,z,u,vv,nx,ny,nz);
    }
}
