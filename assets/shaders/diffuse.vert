varying vec3 normal, eyeVec;

float rand(vec2 co){
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

void main()
{

    normal = gl_NormalMatrix * gl_Normal;

    vec3 vVertex = vec3(gl_ModelViewMatrix * gl_Vertex);
    eyeVec = -vVertex;

    gl_Position = ftransform();

    gl_TexCoord[0] = gl_MultiTexCoord0;
}
