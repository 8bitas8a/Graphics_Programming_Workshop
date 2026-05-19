import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {

    public enum Movement { FORWARD, BACKWARD, LEFT, RIGHT }

    private static final float YAW         = -90.0f;
    private static final float PITCH       =   0.0f;
    private static final float SPEED       =   2.5f;
    private static final float SENSITIVITY =   0.1f;
    private static final float ZOOM        =  45.0f;

    public Vector3f position;
    public Vector3f front;
    public Vector3f up;
    public Vector3f right;
    public Vector3f worldUp;

    public float yaw;
    public float pitch;
    public float movementSpeed;
    public float mouseSensitivity;
    public float zoom;

    public Camera(Vector3f position, Vector3f up, float yaw, float pitch) {
        this.position        = position;
        this.worldUp         = up;
        this.yaw             = yaw;
        this.pitch           = pitch;
        this.front           = new Vector3f(0, 0, -1);
        this.movementSpeed   = SPEED;
        this.mouseSensitivity = SENSITIVITY;
        this.zoom            = ZOOM;
        this.up              = new Vector3f();
        this.right           = new Vector3f();
        updateCameraVectors();
    }

    public Camera() {
        this(new Vector3f(0, 0, 0), new Vector3f(0, 1, 0), YAW, PITCH);
    }

    public Camera(float posX, float posY, float posZ,
                  float upX,  float upY,  float upZ,
                  float yaw,  float pitch) {
        this(new Vector3f(posX, posY, posZ), new Vector3f(upX, upY, upZ), yaw, pitch);
    }

    public Matrix4f getViewMatrix() {
        Vector3f center = new Vector3f(position).add(front);
        return new Matrix4f().lookAt(position, center, up);
    }

    public void processKeyboard(Movement direction, float deltaTime) {
        float velocity = movementSpeed * deltaTime;
        switch (direction) {
            case FORWARD  -> position.add(new Vector3f(front).mul(velocity));
            case BACKWARD -> position.sub(new Vector3f(front).mul(velocity));
            case LEFT     -> position.sub(new Vector3f(right).mul(velocity));
            case RIGHT    -> position.add(new Vector3f(right).mul(velocity));
        }
    }

    public void processMouseMovement(float xoffset, float yoffset, boolean constrainPitch) {
        yaw   += xoffset * mouseSensitivity;
        pitch += yoffset * mouseSensitivity;

        if (constrainPitch) {
            pitch = Math.max(-89.0f, Math.min(89.0f, pitch));
        }

        updateCameraVectors();
    }

    public void processMouseMovement(float xoffset, float yoffset) {
        processMouseMovement(xoffset, yoffset, true);
    }

    public void processMouseScroll(float yoffset) {
        zoom -= yoffset;
        zoom = Math.max(1.0f, Math.min(45.0f, zoom));
    }

    private void updateCameraVectors() {
        double yawRad   = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        front = new Vector3f(
            (float)(Math.cos(yawRad) * Math.cos(pitchRad)),
            (float)(Math.sin(pitchRad)),
            (float)(Math.sin(yawRad) * Math.cos(pitchRad))
        ).normalize();

        right = new Vector3f(front).cross(worldUp).normalize();
        up    = new Vector3f(right).cross(front).normalize();
    }
}
