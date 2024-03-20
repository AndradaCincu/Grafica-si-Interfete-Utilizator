#version 330 core

layout (location = 0) out vec4 outColor;

in vec2 uv;

uniform sampler2D tex;

void main()
{
  outColor = vec4(texture(tex, uv).rgb, 1);
}