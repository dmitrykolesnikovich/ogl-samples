#version 300 es
#include semantic.glsl

precision highp float;
precision highp int;
layout(std140, column_major) uniform;

uniform sampler2D Diffuse;

in vec4 gl_FragCoord;
layout(location = FRAG_COLOR) out vec4 FragColor;

void main()
{
	vec2 TextureSize = vec2(textureSize(Diffuse, 0));

	FragColor = texture(Diffuse, gl_FragCoord.xy * 2.0 / TextureSize);
}