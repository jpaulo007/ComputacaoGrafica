package pt.ipb.esact.compgraf.aulas.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ipb.esact.compgraf.engine.obj.ObjLoader;
import pt.ipb.esact.compgraf.tools.Camera;
import pt.ipb.esact.compgraf.tools.Cameras;
import pt.ipb.esact.compgraf.tools.DefaultGLWindow;
import pt.ipb.esact.compgraf.tools.Shader;
import pt.ipb.esact.compgraf.tools.math.GlMath;
import pt.ipb.esact.compgraf.tools.math.Vectors;

import javax.vecmath.Vector3f;
import java.awt.event.KeyEvent;

public class MovementAndZoom extends DefaultGLWindow {

    private static final Logger logger = LoggerFactory.getLogger(MovementAndZoom.class);

    // .obj loaders
    private ObjLoader wheatley;
    private ObjLoader floor;

    // Vetores de movimento

    // Limites de velocidade (linear e angular)
    private static final float MAX_LINEAR_VELOCITY = 3.0f;
    private static final float MAX_ANGULAR_VELOCITY = 90.0f;

    // Armazena a posição atual do personagem
    Vector3f position = new Vector3f(0, 1, 0);

    // Armazena o vetor "FORWARD" da personagem
    Vector3f forward = Vectors.forward();

    // Armazena a velocidade atual do personagem
    Vector3f velocity = new Vector3f(0, 0, 0);

    // Armazena a orientação atual do personagem
    private float orientation = 0.0f;
    private Shader diffuseShader;

    // skybox
    public MovementAndZoom() {
        super("A10 Shaders 2", true);
        setMouseZoom(true);
    }

    @Override
    public void init() {
        // Definir a cor de background (RGBA={0, 0, 0, 255})
        glClearColor(0.0f, 0.0f, 0.0f, 1f);

        // Activar o teste de profundidade
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_MULTISAMPLE);
        glEnable(GL_CULL_FACE);

        glCullFace(GL_BACK);

        configureLighting();
        configureMaterials();
        configureModels();
        configureShaders();

    }

    private void configureShaders() {
        diffuseShader = new Shader();
        diffuseShader.load("assets/shaders/diffuse.vert", "assets/shaders/diffuse.frag");
    }

    private void configureModels() {
        wheatley = new ObjLoader(this);
        wheatley.load("assets/models/wheatley/wheatley.obj", "assets/models/wheatley/wheatley.mtl");
        floor = new ObjLoader(this);
        floor.load("assets/models/floor/floor.obj", "assets/models/floor/floor.mtl");
    }

    private void configureMaterials() {
        // Configurar Color Tracking
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
        glMateriali(GL_FRONT, GL_SHININESS, 100);

        // Especularidade do material definida explicitamente
        glMaterialfv(GL_FRONT, GL_SPECULAR, newFloatBuffer(1.0f, 1.0f, 1.0f, 1.0f));
    }

    private void configureLighting() {
        // Ativar a Lighting globalmente
        glEnable(GL_LIGHTING);

        // Definição do Modelo de luz para a luz ambiente
        glLightModelfv(GL_LIGHT_MODEL_AMBIENT, newFloatBuffer(0.1f, 0.1f, 0.1f, 1.0f));

        // Configurar uma point light
        glLightfv(GL_LIGHT0, GL_AMBIENT, newFloatBuffer(.1f, .1f, .1f, 1));
        glLightfv(GL_LIGHT0, GL_DIFFUSE, newFloatBuffer(1, 1, 1, 1));
        glLightfv(GL_LIGHT0, GL_SPECULAR, newFloatBuffer(1, 1, 1, 1));
        glEnable(GL_LIGHT0);

        // Configurar uma spot light
        glLightfv(GL_LIGHT1, GL_AMBIENT, newFloatBuffer(.1f, .1f, .1f, 1));
        glLightfv(GL_LIGHT1, GL_DIFFUSE, newFloatBuffer(1, 1, 1, 1));
        glLightfv(GL_LIGHT1, GL_SPECULAR, newFloatBuffer(1, 1, 1, 1));
        glLightf(GL_LIGHT1, GL_SPOT_CUTOFF, 25.0f);
        glLightf(GL_LIGHT1, GL_SPOT_EXPONENT, 0.1f);
        glEnable(GL_LIGHT1);

    }

    @Override
    public void release() {
        // Libertar as texturas (GPU)z
        diffuseShader.release();
    }

    float spotx = 0.0f;

    @Override
    public void render(int width, int height) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // se a tecla 'f' está premida desativa o shader
        if (!isKeyPressed('f'))
            diffuseShader.bind();
        else
            diffuseShader.unbind();

        spotx += GL_PI * timeElapsed();
        spotx %= GL_PI * 2.0;

        // Posicionar as luzes e ajustar a direção do SPOT
        glLightfv(GL_LIGHT0, GL_POSITION, newFloatBuffer(0.0f, 5.0f, 0.0f, 1.0f));
        glLightfv(GL_LIGHT1, GL_POSITION, newFloatBuffer(0.0f, 5.0f, 0.0f, 1.0f));
        glLightfv(GL_LIGHT1, GL_SPOT_DIRECTION, newFloatBuffer(sinf(spotx), -1, 0));

        // Desenhar o Wheatley
        glPushMatrix();
        glTranslatef(position.x, position.y, position.z);
        glRotatef(orientation, 0, 1, 0);
        wheatley.render();
        glPopMatrix();

        floor.render();

        // Atualizar o Movimento
        boolean isRotated = updateRotation();
        boolean isMoved = updateMovement();

        // Atualizar a Camera se houver movimento/rotação
//		if(isMoved || isRotated)
        updateCameraPosition();

        renderText("" + orientation, 10, 20);
    }

    /**
     * Atualiza a orientação do objeto
     *
     * @return @c TRUE no caso de ter havido alteração na orientação
     */
    private boolean updateRotation() {
        if (isKeyPressed("left")) {
            // Aumentar o valor do ângulo da orientação
            orientation += MAX_ANGULAR_VELOCITY * timeElapsed();

            // Aplicar esse ângulo ao vetor FORWARD atual
            forward = GlMath.rotate(orientation, Vectors.up(), Vectors.forward());

            // foi gerado movimento
            return true;
        }

        if (isKeyPressed("right")) {
            // Aumentar o valor do ângulo da orientação
            orientation -= MAX_ANGULAR_VELOCITY * timeElapsed();
            // Aplicar esse ângulo ao vetor FORWARD atual
            forward = GlMath.rotate(orientation, Vectors.up(), Vectors.forward());

            // foi gerado movimento
            return true;
        }

        // _não_ foi gerado movimento
        return false;
    }

    @Override
    protected void onKeyUp(KeyEvent e) {
        if (e.getKeyChar() == 'c') {
            logger.info("Recompiling shader...");
            diffuseShader.load("assets/shaders/diffuse.vert", "assets/shaders/diffuse.frag");
            configureLighting();
        }
    }

    /**
     * Efetua os cálculos da velocidade com base no user input
     *
     * @return @c TRUE no caso de ser gerado movimento (velocity>0)
     */
    private boolean updateMovement() {
        // Colocar a velocidade a 0s
        velocity.set(0, 0, 0);

        // Dar um passo para a esquerda
        if (isKeyPressed('a') || isKeyPressed('d')) {
            // Calcular o vector LEFT com base no vector FORWARD
            Vector3f strafe = GlMath.rotate(90.0f, Vectors.up(), forward);

            // Adicionar esse movimento ao vetor velocidade
            velocity.add(strafe);

            // O strafe é para a direita, negar a 'esquerda'
            if (isKeyPressed('d'))
                velocity.negate();
        }


        if (isKeyPressed('w') || isKeyPressed('s')) {
            // Adicionar o vetor FORWARD ao vetor velocidade
            velocity.add(forward);

            // o movimento é para trás, negar o 'forward'
            if (isKeyPressed('s'))
                velocity.negate();
        }

        // Aplicar a MAX_VELOCITY definida ao vetor velocidade
        velocity.scale(timeElapsed() * MAX_LINEAR_VELOCITY);

        // Somar essa velocidade à nossa posição atual
        position.add(velocity);

        // Se a velocidade > 0 houve movimento
        return velocity.lengthSquared() > 0;
    }

    float zoom = 0.0f;

    float timer = 0.0f;

    private void updateCameraPosition() {

        timer += timeElapsed();

        if (timer >= 4.0f) {
            float increment = 5.0f;
            if (isShiftPressed())
                increment *= -1;
            zoom += increment * timeElapsed();
            if (zoom > 9.0f) zoom = 9.0f;
            if (zoom < 0.0f) zoom = 0.0f;

        }

        // Obter a camera atual
        Camera camera = Cameras.getCurrent();

        if (isKeyPressed('z')) {
        }

        // O novo eye da camera vai ser relativa à posição do wheatley
        // Movê-lo para trás na direção do "FORWARD"
        // Um pouco para cima (y+)
        camera.eye = new Vector3f(position);
        camera.eye.sub(forward);
        camera.eye.y += 10.0f - zoom;

        // Olhar um pouco à frente do wheatley
        camera.at = new Vector3f(position);
        camera.at.add(forward);

        // Forçar a atualização da camera
        setupCamera();
    }

    @Override
    public void resize(int width, int height) {
        setProjectionPerspective(width, height, 100.0f, 0.001f, 500.0f);
        Cameras.setCurrent(new Camera());
        updateCameraPosition();
        setupCamera();
    }

    // Função main confere capacidade de executável ao .java atual
    public static void main(String[] args) {
        new MovementAndZoom();
    }

}
