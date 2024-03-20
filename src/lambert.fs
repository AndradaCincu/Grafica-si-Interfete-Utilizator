#version 330 core

layout(location=0)out vec4 outColor;

in vec3 pos;
smooth in vec3 normal;
in vec2 uv;

uniform sampler2D tex;

const vec3 lightPos=vec3(0,20,0);
const vec3 ambient=vec3(0,0,.001);

void main()
{
  // normalizarea pixelilor si directia luminii
  vec3 norm=normalize(normal);
  vec3 lightDir=normalize(lightPos-pos);
  
  // calcularea cantitatii de umbra lambert
  float amount=max(dot(norm,lightDir),0.);
  
  // esantionarea texturii
  vec3 diffuse=texture(tex,uv).rgb;
  
  // suprapunem umbra lambert cu textura
  vec3 result=mix(amount*diffuse,diffuse,.4);
  
  outColor=vec4(result,1);
}