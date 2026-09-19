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
        if (rotateAroundHead && (yawDegrees != 0.0F || pitchDegrees != 0.0F)) {
            float yaw = (float)Math.toRadians(yawDegrees);
            float pitch = (float)Math.toRadians(pitchDegrees);
            float sinYaw = (float)Math.sin(yaw);
            float cosYaw = (float)Math.cos(yaw);
            float sinPitch = (float)Math.sin(pitch);
            float cosPitch = (float)Math.cos(pitch);

            x -= HEAD_PIVOT_X;
            y -= HEAD_PIVOT_Y;
            z -= HEAD_PIVOT_Z;

            // Yaw in the model's Y-up coordinate system.
            float yawX = x * cosYaw + z * sinYaw;
            float yawZ = -x * sinYaw + z * cosYaw;

            // Pitch around model-local X.
            float pitchY = y * cosPitch - yawZ * sinPitch;
            float pitchZ = y * sinPitch + yawZ * cosPitch;

            x = yawX + HEAD_PIVOT_X;
            y = pitchY + HEAD_PIVOT_Y;
            z = pitchZ + HEAD_PIVOT_Z;

            float normalYawX = nx * cosYaw + nz * sinYaw;
            float normalYawZ = -nx * sinYaw + nz * cosYaw;
            float normalPitchY = ny * cosPitch - normalYawZ * sinPitch;
            float normalPitchZ = ny * sinPitch + normalYawZ * cosPitch;

            nx = normalYawX;
            ny = normalPitchY;
            nz = normalPitchZ;
        }

        buffer.addVertex(pose, x * scale, y * scale, z * scale)
            .setColor(0xFFFFFFFF)
            .setUv(u, v)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(pose, nx, ny, nz);
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
        for (int i = 0; i < vertices.length; i += 8) {
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
