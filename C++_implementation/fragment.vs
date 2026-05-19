#version 330 core
out vec4 FragColor;

in vec2 textCoord;

uniform sampler2D ourTexture0;
uniform sampler2D ourTexture1;
uniform float vis;

void main()
{
    FragColor = mix(texture(ourTexture0, textCoord), texture(ourTexture1, vec2(textCoord.x, textCoord.y)), vis);
}