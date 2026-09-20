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
    public static final int EYES = 0;
    public static final int PUPILS = 0;
    public static final int INTERIOR = 1;
    public static final int TOILET = 2;

    private static final String RESOURCE = "/assets/skibidi/mesh/skibidi_v3.skbm";
    private static final float MODEL_Y_OFFSET = 1.501F;

    private static final float HEAD_PIVOT_X = 0.0F;
    private static final float HEAD_PIVOT_Y = 1.40F;
    private static final float HEAD_PIVOT_Z = -0.01F;

    private static final Map<Integer, MeshPart> PARTS;

    static {
        try {
            PARTS = load();
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private SkibidiMesh() {}

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
        if (part == null) {
            return;
        }
        renderVertices(
            part.vertices, pose, buffer, light, overlay, scale,
            yawDegrees, pitchDegrees, rotateAroundHead
        );
    }

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

        if (rotateAroundHead && (yawDegrees != 0.0F || pitchDegrees != 0.0F)) {
            x -= HEAD_PIVOT_X;
            y -= HEAD_PIVOT_Y;
            z -= HEAD_PIVOT_Z;

            float yawX = x * cosYaw - z * sinYaw;
            float yawZ = x * sinYaw + z * cosYaw;
            float pitchY = y * cosPitch - yawZ * sinPitch;
            float pitchZ = y * sinPitch + yawZ * cosPitch;

            x = yawX + HEAD_PIVOT_X;
            y = pitchY + HEAD_PIVOT_Y;
            z = pitchZ + HEAD_PIVOT_Z;

            float normalYawX = nx * cosYaw - nz * sinYaw;
            float normalYawZ = nx * sinYaw + nz * cosYaw;
            float normalPitchY = ny * cosPitch - normalYawZ * sinPitch;
            float normalPitchZ = ny * sinPitch + normalYawZ * cosPitch;

            nx = normalYawX;
            ny = normalPitchY;
            nz = normalPitchZ;
        }

        float modelX = -x * scale;
        float modelY = MODEL_Y_OFFSET - y * scale;
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
        for (int i = 0; i < vertices.length; i += 8) {
            emitVertex(
                pose, buffer, light, overlay, scale,
                yawDegrees, pitchDegrees, rotateAroundHead,
                vertices[i], vertices[i + 1], vertices[i + 2],
                vertices[i + 3], vertices[i + 4],
                vertices[i + 5], vertices[i + 6], vertices[i + 7]
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
                || data.get() != '3') {
                throw new IOException("Invalid Skibidi v3 mesh header");
            }

            int version = data.getInt();
            if (version != 3) {
                throw new IOException("Unsupported Skibidi mesh version: " + version);
            }

            int groupCount = data.getInt();
            if (groupCount != 3) {
                throw new IOException("Expected exactly 3 Skibidi material groups, got " + groupCount);
            }

            Map<Integer, MeshPart> parts = new HashMap<>();
            for (int i = 0; i < groupCount; i++) {
                if (data.remaining() < 12) {
                    throw new IOException("Truncated Skibidi mesh group header");
                }
                int group = data.getInt();
                int material = data.getInt();
                int vertexCount = data.getInt();
                if (parts.containsKey(group)) {
                    throw new IOException("Duplicate Skibidi mesh group: " + group);
                }
                parts.put(group, new MeshPart(material, readVertices(data, vertexCount)));
            }

            if (data.hasRemaining()) {
                throw new IOException("Unexpected trailing bytes in Skibidi v3 mesh");
            }
            if (!parts.containsKey(HEAD_BASE)
                || !parts.containsKey(INTERIOR)
                || !parts.containsKey(TOILET)) {
                throw new IOException("Skibidi v3 mesh is missing a required material group");
            }
            return Map.copyOf(parts);
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

    private record MeshPart(int material, float[] vertices) {}
}
