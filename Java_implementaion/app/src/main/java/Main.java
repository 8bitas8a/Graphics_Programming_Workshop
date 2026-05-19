
import org.joml.Matrix4f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

    private long window;
    private int  shaderProgram;
    private int  vao;
    private int  winWidth  = 800;
    private int  winHeight = 600;

    private final Camera camera     = new Camera(0, 0, 3, 0, 1, 0, -90, 0);
    private float        lastX      = 400;
    private float        lastY      = 300;
    private boolean      firstMouse = true;

    // ── Shaders ──────────────────────────────────────────────────────────────

    private static final String VERTEX_SRC = """
            #version 330 core
            layout(location = 0) in vec2 aPos;
            layout(location = 1) in vec3 aColor;

            out vec3 vColor;
            uniform mat4 view;
            uniform mat4 projection;

            void main() {
                vColor      = aColor;
                gl_Position = projection * view * vec4(aPos, 0.0, 1.0);
            }
            """;

    private static final String FRAGMENT_SRC = """
            #version 330 core
            in  vec3 vColor;
            out vec4 fragColor;

            uniform float uTime;

            void main() {
                float brightness = 0.7 + 0.3 * sin(uTime * 2.0);
                fragColor = vec4(vColor * brightness, 1.0);
            }
            """;

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        init();
        loop();

        glDeleteVertexArrays(vao);
        glDeleteProgram(shaderProgram);

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE,   GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(winWidth, winHeight, "Modern OpenGL – Camera", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // capture cursor for mouse-look
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(win, true);
        });

        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            float x = (float) xpos;
            float y = (float) ypos;
            if (firstMouse) {
                lastX = x; lastY = y;
                firstMouse = false;
            }
            float xoffset =  x - lastX;
            float yoffset = lastY - y;  // reversed: screen y grows downward
            lastX = x;
            lastY = y;
            camera.processMouseMovement(xoffset, yoffset);
        });

        glfwSetScrollCallback(window, (win, xoffset, yoffset) ->
            camera.processMouseScroll((float) yoffset));

        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            winWidth  = w;
            winHeight = h;
            glViewport(0, 0, w, h);
        });

        try (MemoryStack stack = stackPush()) {
            IntBuffer pw = stack.mallocInt(1);
            IntBuffer ph = stack.mallocInt(1);
            glfwGetWindowSize(window, pw, ph);
            GLFWVidMode vid = glfwGetVideoMode(glfwGetPrimaryMonitor());
            glfwSetWindowPos(window,
                    (vid.width()  - pw.get(0)) / 2,
                    (vid.height() - ph.get(0)) / 2);
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();

        shaderProgram = buildShaderProgram(VERTEX_SRC, FRAGMENT_SRC);
        vao           = buildTriangleVAO();
    }

    // ── Render loop ───────────────────────────────────────────────────────────

    private void loop() {
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        int uTime       = glGetUniformLocation(shaderProgram, "uTime");
        int uView       = glGetUniformLocation(shaderProgram, "view");
        int uProjection = glGetUniformLocation(shaderProgram, "projection");

        float lastFrame = 0.0f;

        while (!glfwWindowShouldClose(window)) {
            float currentFrame = (float) glfwGetTime();
            float deltaTime    = currentFrame - lastFrame;
            lastFrame          = currentFrame;

            // WASD keyboard movement
            if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
                camera.processKeyboard(Camera.Movement.FORWARD,  deltaTime);
            if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
                camera.processKeyboard(Camera.Movement.BACKWARD, deltaTime);
            if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
                camera.processKeyboard(Camera.Movement.LEFT,     deltaTime);
            if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
                camera.processKeyboard(Camera.Movement.RIGHT,    deltaTime);

            glClear(GL_COLOR_BUFFER_BIT);
            glUseProgram(shaderProgram);
            glUniform1f(uTime, currentFrame);

            try (MemoryStack stack = stackPush()) {
                FloatBuffer view = camera.getViewMatrix().get(stack.mallocFloat(16));
                glUniformMatrix4fv(uView, false, view);

                float aspect = (float) winWidth / winHeight;
                Matrix4f proj = new Matrix4f().perspective(
                    (float) Math.toRadians(camera.zoom), aspect, 0.1f, 100.0f);
                FloatBuffer projection = proj.get(stack.mallocFloat(16));
                glUniformMatrix4fv(uProjection, false, projection);
            }

            glBindVertexArray(vao);
            glDrawArrays(GL_TRIANGLES, 0, 3);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int buildTriangleVAO() {
        // interleaved: x, y,   r, g, b
        float[] vertices = {
             0.0f,  0.5f,   1.0f, 0.0f, 0.0f,
            -0.5f, -0.5f,   0.0f, 1.0f, 0.0f,
             0.5f, -0.5f,   0.0f, 0.0f, 1.0f,
        };

        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer buf = memAllocFloat(vertices.length);
        buf.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);
        memFree(buf);

        int stride = 5 * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        return vao;
    }

    private static int buildShaderProgram(String vertSrc, String fragSrc) {
        int vert = compileShader(GL_VERTEX_SHADER,   vertSrc);
        int frag = compileShader(GL_FRAGMENT_SHADER, fragSrc);

        int program = glCreateProgram();
        glAttachShader(program, vert);
        glAttachShader(program, frag);
        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE)
            throw new RuntimeException("Shader link error:\n" + glGetProgramInfoLog(program));

        glDeleteShader(vert);
        glDeleteShader(frag);

        return program;
    }

    private static int compileShader(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);

        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("Shader compile error:\n" + glGetShaderInfoLog(id));

        return id;
    }
}
