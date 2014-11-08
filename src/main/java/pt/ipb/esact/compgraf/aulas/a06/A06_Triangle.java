package pt.ipb.esact.compgraf.aulas.a06;

import java.nio.FloatBuffer;

import pt.ipb.esact.compgraf.tools.Camera;
import pt.ipb.esact.compgraf.tools.Cameras;
import pt.ipb.esact.compgraf.tools.DefaultGLWindow;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

public class A06_Triangle extends DefaultGLWindow {

	// Array com a posição da luz
	FloatBuffer positionLitght0 = newFloatBuffer(-10.0f, 20.0f, -10.0f, 1.0f);
	
	private Texture TEX_STONE;

	public A06_Triangle() {
		super("A06 Triangle", true);
		setMousePan(true);
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

		// Configurar as luzes
		configureLighting();
		
		// Configurar os materiais
		configureMaterials();
		
		// Configurar as texturas
		configureTextures();
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
		glLightModelfv(GL_LIGHT_MODEL_AMBIENT, newFloatBuffer(0.5f, 0.5f, 0.5f, 1.0f));
	}
	
	private void configureTextures() {
		glEnable(GL_TEXTURE_2D);
		try {
			TEX_STONE = TextureIO.newTexture(packageFile("stone.png"), true, TextureIO.PNG);
		} catch (Exception e) {
			// Ocorreu um erro --> Terminar o programa
			exit("Foi impossivel carregar a imagem 'stone.png'");
		}
	}

	@Override
	public void release() {
		// Libertar recursos
		TEX_STONE.destroy(this);
	}
	
	@Override
	public void render(int width, int height) {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		glColor3f(1.0f, 1.0f, 1.0f);
		
		glPushAttrib(GL_ENABLE_BIT);
			glDisable(GL_TEXTURE_2D);
			demo().drawFloor(12, 10);
		glPopAttrib();

		// Desenhar a Parede da Direita
		TEX_STONE.bind(this);
		glBegin(GL_TRIANGLES);
			// Vertice inferior direito
			glTexCoord2f(1.0f, 0.0f);
			glVertex3f(1, 0.1f, 0);
			
			// Vertice superior
			glTexCoord2f(0.5f, 1.0f);
			glVertex3f(0, 1.87f, 0);
			
			// Vertice inferior esquerdo
			glTexCoord2f(0.0f, 0.0f);
			glVertex3f(-1, 0.1f, 0);
		glEnd();
	}
	

	@Override
	public void resize(int width, int height) {
		setProjectionPerspective(width, height, 100.0f, 0.001f, 30.0f);
		Camera camera = new Camera(0, 1, 2);
		camera.at.y = 1.0f;
		Cameras.setCurrent(camera);
		setupCamera();
	}

	// Função main confere capacidade de executável ao .java atual
	public static void main(String[] args) {
		new A06_Triangle();
	}

}