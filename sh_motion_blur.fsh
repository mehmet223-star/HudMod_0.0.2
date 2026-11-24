#version 330

uniform sampler2D InSampler;
uniform sampler2D PrevSampler;

in vec2 texCoord;

out vec4 fragColor;

layout(std140) uniform BlurConfig {
    float BlendFactor;
};

void main() {
    vec4 now = texture(InSampler, texCoord);
    vec4 old = texture(PrevSampler, texCoord);
    fragColor = vec4(mix(now.rgb, old.rgb, BlendFactor), 1.0);
}
