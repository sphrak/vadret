package se.vadretapp.vadret.forecastwarning.ui

import androidx.compose.ui.graphics.Shader

internal const val RESOLUTION_UNIFORM = "resolution"
internal const val TIME_UNIFORM = "time"

// The night sky over the horizon, with stars that twinkle and three curtains of northern lights that drift, sway and
// shimmer in rays, green at their foot and violet at their top. It is one source for both phones: Android runs it as
// AGSL and iOS as SkSL, which read the same.
internal const val AURORA_SKY_SHADER = """
uniform float2 resolution;
uniform float time;

float random(float2 position) {
    position = fract(position * float2(123.34, 456.21));
    position += dot(position, position + 45.32);
    return fract(position.x * position.y);
}

float valueNoise(float2 position) {
    float2 cell = floor(position);
    float2 local = fract(position);
    float2 fade = local * local * (3.0 - 2.0 * local);
    float lower = mix(random(cell), random(cell + float2(1.0, 0.0)), fade.x);
    float upper = mix(random(cell + float2(0.0, 1.0)), random(cell + float2(1.0, 1.0)), fade.x);
    return mix(lower, upper, fade.y);
}

float layeredNoise(float2 position) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int octave = 0; octave < 4; octave++) {
        value += amplitude * valueNoise(position);
        position *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / resolution;
    float altitude = 1.0 - uv.y;
    float x = uv.x * resolution.x / resolution.y;

    float3 color = mix(float3(0.035, 0.075, 0.13), float3(0.004, 0.012, 0.035), altitude);

    float2 starField = float2(x, altitude) * 90.0;
    float starSeed = random(floor(starField));
    float starShape = smoothstep(0.25, 0.0, length(fract(starField) - 0.5));
    float twinkle = 0.6 + 0.4 * sin(time * 1.5 + starSeed * 40.0);
    float star = step(0.985, starSeed) * starShape * twinkle * smoothstep(0.15, 0.5, altitude);

    float3 aurora = float3(0.0);
    for (int curtain = 0; curtain < 3; curtain++) {
        float depth = float(curtain);
        float drift = time * (0.06 + 0.03 * depth);
        float foot = 0.42 + 0.1 * depth
            + 0.09 * sin(x * (1.3 + 0.4 * depth) + drift * 3.0 + depth * 1.7)
            + 0.12 * (layeredNoise(float2(x * 0.9 + depth * 4.0, drift)) - 0.5);
        float above = altitude - foot;
        float glow = smoothstep(-0.07, 0.0, above) * exp(-max(above, 0.0) * (5.0 + 2.0 * depth));
        float rays = 0.35 + 0.65 * smoothstep(0.35, 0.75, layeredNoise(float2(x * 14.0 + depth * 9.0, time * 0.35 + depth)));
        float3 tint = mix(float3(0.16, 0.95, 0.55), float3(0.55, 0.32, 0.92), clamp(above * 2.5, 0.0, 1.0));
        aurora += tint * glow * rays * (0.75 - 0.2 * depth);
    }
    color += aurora + float3(star) * (1.0 - clamp(length(aurora), 0.0, 1.0));

    float horizon = 0.06 + 0.06 * layeredNoise(float2(x * 3.0, 0.0));
    color = mix(color, float3(0.005, 0.01, 0.02), smoothstep(horizon + 0.005, horizon, altitude));
    return half4(half3(color), 1.0);
}
"""

internal interface AuroraSkyShader {
    fun shader(
        width: Float,
        height: Float,
        seconds: Float
    ): Shader
}

internal expect fun auroraSkyShaderOrNull(): AuroraSkyShader?