package org.elfen.controls;

/** Presentation geometry only: shared by rendering and touch hit testing. */
public final class CombatLayout {
    public final float left, top, right, bottom;
    public final float stickX, stickY, stickRadius, buttonsX, buttonsY, buttonRadius;
    public final boolean compact;
    private final float width, canonicalButtonsX;
    private final boolean mirror;

    public CombatLayout(float width, float height, float size, boolean mirror,
                        float stickNX, float stickNY, float buttonsNX, float buttonsNY) {
        this.width = Math.max(1, width);
        height = Math.max(1, height);
        this.mirror = mirror;
        size = clamp(size, .65f, 1.5f);
        float header = height * .10f;
        // Wider phones keep the RC1 viewport exactly. Reserve usable control
        // space on narrower screens, keeping the original 640:480 aspect ratio.
        float minimumSide = Math.min(height * .30f, this.width * .23f);
        float scale = Math.min((this.width - minimumSide * 2) / 640f,
                              (height - header) / 480f);
        left = (this.width - 640 * scale) / 2;
        right = this.width - left;
        top = header + (height - header - 480 * scale) / 2;
        bottom = top + 480 * scale;
        float gap = height * .018f, sideWidth = left - gap * 2;
        float requestedRadius = height * .06f * size;
        // Include the full 1.12-radius hit targets, not just the circles.
        compact = requestedRadius * 6.94f > sideWidth;
        float upper = compact ? 9.22f : 4.27f, lower = 2.64f;
        buttonRadius = Math.min(requestedRadius, Math.min(sideWidth / (compact ? 4.64f : 6.94f),
                                (height * .86f - gap) / (upper + lower)));
        float halfWidth = (compact ? 2.32f : 3.47f) * buttonRadius;
        float defaultBX = (right + this.width) / 2;
        canonicalButtonsX = clamp(value(buttonsNX, defaultBX / this.width) * this.width,
                                   right + gap + halfWidth, this.width - gap - halfWidth);
        buttonsX = reflect(canonicalButtonsX);
        // Reserve D/E/F too, so A/B/C never move when a character/scene changes.
        buttonsY = clamp(value(buttonsNY, .75f) * height,
                         height * .14f + upper * buttonRadius, height - gap - lower * buttonRadius);
        stickRadius = Math.min(height * .17f * size, sideWidth / 2);
        stickX = reflect(clamp(value(stickNX, .15f) * this.width,
                               gap + stickRadius, left - gap - stickRadius));
        stickY = clamp(value(stickNY, .75f) * height,
                       height * .14f + stickRadius, height - gap - stickRadius);
    }

    public float buttonX(int index) {
        float step = compact ? 1.2f : 2.35f;
        return reflect(canonicalButtonsX + (index % 3 - 1) * step * buttonRadius);
    }
    public float buttonY(int index) {
        float y = index % 3 == 1 ? (compact ? -2.1f : .45f) : .8f;
        if (index >= 3) y -= compact ? 6f : 3.6f;
        return buttonsY + y * buttonRadius;
    }
    private float reflect(float x) { return mirror ? width - x : x; }
    private static float value(float x, float fallback) {
        return Float.isNaN(x) || Float.isInfinite(x) ? fallback : x;
    }
    private static float clamp(float x, float low, float high) {
        return Math.max(low, Math.min(high, x));
    }
}
