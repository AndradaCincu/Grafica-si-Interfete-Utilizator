#version 330 core

layout (location = 0) out vec4 outColor;

in vec3 pos;
smooth in vec3 normal;
in vec2 uv;

uniform sampler2D tex;

const vec3 lightPos = vec3(0, 20, 0);
const vec3 ambient = vec3(0, 0, 0.001);

void main()
{
  vec3 norm = normalize(normal);
  vec3 lightDir = normalize(lightPos - pos);

  float amount = max(dot(norm, lightDir), 0.0);

  vec3 diffuse = texture(tex, uv).rgb;

  vec3 result = mix(amount * diffuse, diffuse, 0.4);

  outColor = vec4(result, 1);
}