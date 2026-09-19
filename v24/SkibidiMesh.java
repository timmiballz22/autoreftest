package dev.ashenthefox.skibidi.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public final class SkibidiMesh {
    public static final int HEAD_BASE = 0;
    public static final int EYES = 1;
    public static final int PUPILS = 2;
    public static final int INTERIOR = 3;
    public static final int TOILET = 4;

    private static final String RESOURCE = "/assets/skibidi/mesh/skibidi_v2.skbm";

    static final float HEAD_PIVOT_X = 0.0F;
    static final float HEAD_PIVOT_Y = 1.40F;
    static final float HEAD_PIVOT_Z = -0.01F;

    private static final Map<Integer, MeshPart> PARTS;

    static {
        try {
            PARTS = load();
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private SkibidiMesh() {
    }

    public static int materialFor(int group) {
        MeshPart part = PARTS.get(group);
        if (part == null) {
            throw new IllegalArgumentException("Unknown Skibidi mesh group: " + group);
        }
        return part.material;
    }

    public static void renderGroup(
        int group,
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float yawDegrees,
        float pitchDegrees,
        boolean rotateAroundHead
    ) {
        MeshPart part = PARTS.get(group);
        if (part == null || part.vertices.length == 0) {
            return;
        }

        renderVertices(
            part.vertices,
            pose,
            buffer,
            light,
            overlay,
            scale,
            yawDegrees,
            pitchDegrees,
            rotateAroundHead
        );
    }

    /*
     * IMPORTANT:
     * These coordinates are now emitted directly in entity-local world space.
     *
     * The old renderer fed the mesh through LivingEntityRenderer's
     * scale(-1,-1,+1) + translate(-1.501) model-space transform, then attempted
     * to cancel those operations again for every vertex. That made the glTF
     * mesh dependent on vanilla ModelPart coordinate conventions even though
     * the uploaded model is already Y-up and measured in blocks.
     *
     * Keeping one transform chain fixes the model warping/mirroring/offset
     * problems and also keeps all accessories aligned to the same coordinate
     * system.
     */
    static void emitVertex(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float yawDegrees,
        float pitchDegrees,
        boolean rotateAroundHead,
        float x,
        float y,
        float z,
        float u,
        float v,
        float nx,
        float ny,
        float nz
    ) {
        float yaw = (float)Math.toRadians(yawDegrees);
        float pitch = (float)Math.toRadians(pitchDegrees);
        float sinYaw = (float)Math.sin(yaw);
        float cosYaw = (float)Math.cos(yaw);
        float sinPitch = (float)Math.sin(pitch);
        float cosPitch = (float)Math.cos(pitch);

        if (rotateAroundHead) {
            x -= HEAD_PIVOT_X;
            y -= HEAD_PIVOT_Y;
            z -= HEAD_PIVOT_Z;

            float rotatedX = x * cosYaw - z * sinYaw;
            float rotatedZ = x * sinYaw + z * cosYaw;
            float rotatedY = y * cosPitch - rotatedZ * sinPitch;
            float rotatedZ2 = y * sinPitch + rotatedZ * cosPitch;

            x = rotatedX + HEAD_PIVOT_X;
            y = rotatedY + HEAD_PIVOT_Y;
            z = rotatedZ2 + HEAD_PIVOT_Z;

            float rotatedNx = nx * cosYaw - nz * sinYaw;
            float rotatedNz = nx * sinYaw + nz * cosYaw;
            float rotatedNy = ny * cosPitch - rotatedNz * sinPitch;
            float rotatedNz2 = ny * sinPitch + rotatedNz * cosPitch;

            nx = rotatedNx;
            ny = rotatedNy;
            nz = rotatedNz2;
        }

        /*
         * LivingEntityRenderer has already applied:
         *   scale(-1,-1,+1)
         *   translate(0,-1.501,0)
         * before render layers are submitted. The uploaded glTF is ordinary
         * Y-up block-space geometry, so these coordinates deliberately undo
         * that vanilla ModelPart convention. This preserves all normal vanilla
         * living-entity pose/rotation handling while the topology conversion
         * below fixes the actual rendering corruption.
         */
        float modelX = -x * scale;
        float modelY = 1.501F - y * scale;
        float modelZ = z * scale;

        buffer.addVertex(pose, modelX, modelY, modelZ)
            .setColor(0xFFFFFFFF)
            .setUv(u, v)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(pose, -nx, -ny, nz);
    }

    private static void renderVertices(
        float[] vertices,
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float yawDegrees,
        float pitchDegrees,
        boolean rotateAroundHead
    ) {
        /*
         * RenderTypes.entityCutoutNoCull() uses Minecraft's NEW_ENTITY QUADS
         * vertex pipeline. The source glTF is a TRIANGLES mesh.
         *
         * Feeding A,B,C,A,B,C... directly into a QUADS buffer makes Minecraft
         * group unrelated triangle vertices four-at-a-time, visibly shredding
         * the model. Convert each source triangle A,B,C into the degenerate
         * quad A,B,C,C. Minecraft then produces:
         *   triangle 1: A,B,C  (the original face)
         *   triangle 2: C,C,A  (zero-area, invisible)
         * so the imported glTF geometry is preserved exactly.
         */
        for (int i = 0; i < vertices.length; i += 24) {
            emitPackedVertex(vertices, i, pose, buffer, light, overlay, scale, yawDegrees, pitchDegrees, rotateAroundHead);
            emitPackedVertex(vertices, i + 8, pose, buffer, light, overlay, scale, yawDegrees, pitchDegrees, rotateAroundHead);
            emitPackedVertex(vertices, i + 16, pose, buffer, light, overlay, scale, yawDegrees, pitchDegrees, rotateAroundHead);
            emitPackedVertex(vertices, i + 16, pose, buffer, light, overlay, scale, yawDegrees, pitchDegrees, rotateAroundHead);
        }
    }

    private static void emitPackedVertex(
        float[] vertices,
        int i,
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int light,
        int overlay,
        float scale,
        float yawDegrees,
        float pitchDegrees,
        boolean rotateAroundHead
    ) {
        emitVertex(
            pose,
            buffer,
            light,
            overlay,
            scale,
            yawDegrees,
            pitchDegrees,
            rotateAroundHead,
            vertices[i],
            vertices[i + 1],
            vertices[i + 2],
            vertices[i + 3],
            vertices[i + 4],
            vertices[i + 5],
            vertices[i + 6],
            vertices[i + 7]
        );
    }

    private static Map<Integer, MeshPart> load() throws IOException {
        try (InputStream input = SkibidiMesh.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing mesh resource: " + RESOURCE);
            }

            ByteBuffer data = ByteBuffer.wrap(input.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);

            if (data.remaining() < 12
                || data.get() != 'S'
                || data.get() != 'K'
                || data.get() != 'B'
                || data.get() != '2') {
                throw new IOException("Invalid Skibidi v2 mesh header");
            }

            int version = data.getInt();
            if (version != 2) {
                throw new IOException("Unsupported Skibidi v2 mesh version: " + version);
            }

            int groupCount = data.getInt();
            if (groupCount < 1 || groupCount > 32) {
                throw new IOException("Invalid Skibidi mesh group count: " + groupCount);
            }

            Map<Integer, MeshPart> result = new HashMap<>();
            for (int groupIndex = 0; groupIndex < groupCount; groupIndex++) {
                if (data.remaining() < 12) {
                    throw new IOException("Truncated Skibidi mesh group header");
                }

                int group = data.getInt();
                int material = data.getInt();
                int vertexCount = data.getInt();

                if (result.containsKey(group)) {
                    throw new IOException("Duplicate Skibidi mesh group: " + group);
                }

                result.put(group, new MeshPart(material, readVertices(data, vertexCount)));
            }

            if (data.hasRemaining()) {
                throw new IOException("Unexpected trailing bytes in Skibidi mesh");
            }

            return Map.copyOf(result);
        }
    }

    private static float[] readVertices(ByteBuffer data, int vertexCount) throws IOException {
        long floatCount = (long)vertexCount * 8L;
        long byteCount = floatCount * Float.BYTES;

        if (vertexCount < 0
            || vertexCount % 3 != 0
            || byteCount > data.remaining()
            || floatCount > Integer.MAX_VALUE) {
            throw new IOException("Corrupt Skibidi mesh vertex count: " + vertexCount);
        }

        float[] result = new float[(int)floatCount];
        for (int i = 0; i < result.length; i++) {
            result[i] = data.getFloat();
        }
        return result;
    }

    private static final class MeshPart {
        private final int material;
        private final float[] vertices;

        private MeshPart(int material, float[] vertices) {
            this.material = material;
            this.vertices = vertices;
        }
    }
}
