#version 330 core

layout(location=0)out vec4 outColor;

in vec4 pos;
in vec2 uv;

uniform sampler2D reflectionTex;
uniform sampler2D refractionTex;
uniform sampler2D distortionTex;

uniform float distortionMove;

void main()
{
    // calculam coordonatele texturii noastre in raport cu ecranul
    vec2 ndc=(pos.xy/pos.w)/2.+.5;
    
    //copiem aceste coordonate in coordonatele de refexie si refractie
    // coordonata y a coordonatelor texturii de refexie este inversata,deoarece coordonata y din sistemul de coordonate al ecranului scade atunci cand este incrementata
    // inmultim coordonatele cu 0.95 deoarce valorile originale sunt putin deplasate
    vec2 reflectionUV=vec2(ndc.x,-ndc.y)*.95;
    vec2 refractionUV=ndc*.95;
    
    // calculam doua tipuri diferite de distorsiuni care se schimba in timp cu distorsionMove
    // combinam aceste doua distorsiuni in una singura pentrua o face mai realista
    // inmultimcu 0.5 pentrua slabi efectul de distorsiune
    vec2 distortionUV1=((texture(distortionTex,vec2(uv.x*5+distortionMove,uv.y*5)).rg*2.-1.))*.05;
    vec2 distortionUV2=((texture(distortionTex,vec2(-uv.x*5+distortionMove,uv.y*5+distortionMove)).rg*2.-1.))*.05;
    vec2 distortionUV=distortionUV1+distortionUV2;
    
    // aplicam distorsiunile atat la coordonatele de refexie cat si la cele de refractie
    reflectionUV+=distortionUV;
    refractionUV+=distortionUV;
    
    //fixam coordonatele intre 0.001 si 0.999 pentru a elimina defectiunea de distrosiune care apare la marginea ecranului
    // inversam coordonatele y pentru coordonatele de reflexie
    reflectionUV.x=clamp(reflectionUV.x,.001,.999);
    reflectionUV.y=clamp(reflectionUV.y,-.999,-.001);
    refractionUV=clamp(refractionUV,.001,.999);
    
    // esantionarea texturilor
    vec3 reflection=texture(reflectionTex,reflectionUV).rgb;
    vec3 refraction=texture(refractionTex,refractionUV).rgb;
    
    // combinarea culorii de reflexie si refractie
    vec3 result=mix(reflection,refraction,.5);
    
    // adaugarea unei nuante la culoarea finala
    result=mix(result,vec3(0.,.3,.5),.2);
    
    outColor=vec4(result,1);
}