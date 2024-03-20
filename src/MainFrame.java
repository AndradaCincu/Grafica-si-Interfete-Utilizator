import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

import java.awt.event.MouseWheelEvent;
import java.io.File;

import javax.swing.*;

public class MainFrame extends JFrame
		implements GLEventListener, java.awt.event.MouseMotionListener, java.awt.event.MouseWheelListener {
	// Punctul de intrare principal al aplicației
	public static void main(String args[]) {
		new MainFrame();
	}

	// Constructorul principal;
	public MainFrame() {
		super("Java OpenGL");

		// Înregistrarea unui event listener window pentru închidere.
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		this.setSize(800, 600);

		this.initializeJogl();

		// afisarea ferestrei in centru
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}

	private void initializeJogl() {
		// Crearea unui nou profil GL.
		GLProfile glprofile = GLProfile.getDefault();
		// Crearea unui obiect pentru a manipula parametrii OpenGL.
		GLCapabilities capabilities = new GLCapabilities(glprofile);

		// Setarea unor parametri OpenGL.
		capabilities.setHardwareAccelerated(true);
		capabilities.setDoubleBuffered(true);

		// Try to enable 2x anti aliasing. It should be supported on most hardware.
		capabilities.setNumSamples(2);
		capabilities.setSampleBuffers(true);

		// Crearea unui widget de afișare OpenGL -- canvas.
		this.canvas = new GLCanvas(capabilities);

		// Adăugarea canvasului în centrul cadrului.
		this.getContentPane().add(this.canvas);

		// Adăugarea unui event listener OpenGL la pânză.
		this.canvas.addGLEventListener(this);

		// Adaugarea unui movement listener al mouse-ului
		this.canvas.addMouseMotionListener(this);

		// Adaugarea unui listener pentru rotita mouse-ului
		this.canvas.addMouseWheelListener(this);

		// Crearea unei animati care va redesena scena de 40 de ori pe secundă.
		this.animator = new Animator(this.canvas);

		// Pornirea animatiei.
		this.animator.start();
	}

	public void init(GLAutoDrawable canvas) {
		GL2 gl = canvas.getGL().getGL2();

		try {
			// incaracarea shader-elor
			lambert = new ShaderProgram(gl, "lambert");
			water = new ShaderProgram(gl, "water");
			noshading = new ShaderProgram(gl, "noshading");
			cliplambert = new ShaderProgram(gl, "cliplambert");

			// incarcarea texturilor
			lionTex = TextureIO.newTexture(new File("lion.png"), true);
			tigerTex = TextureIO.newTexture(new File("tiger.png"), true);
			alligatorTex = TextureIO.newTexture(new File("alligator.png"), true);
			landTex = TextureIO.newTexture(new File("grass.png"), true);
			distortionTex = TextureIO.newTexture(new File("distortion.png"), true);
			skyboxTex = TextureIO.newTexture(new File("skybox.png"), true);
			treeTex = TextureIO.newTexture(new File("tree.png"), true);

			// pentru o textură ok, setăm parametrul de eșantionare pentru textura terenului
			// la GL_REPEAT
			distortionTex.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
			distortionTex.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
			landTex.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
			landTex.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);

			// incarcarea mesh-urilor
			lion = new Mesh(gl, "LionMeshData");
			tiger = new Mesh(gl, "TigerMeshData");
			alligator = new Mesh(gl, "AlligatorMeshData");
			land = new Mesh(gl, "LandMeshData");
			tree = new Mesh(gl, "TreeMeshData");
			cube = new Mesh(gl, "CubeMeshData");

			// Patratul mesh pentru apa
			float[] squareVertecies = new float[] {
					1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
					1.0f, -1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f,
					-1.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f,
					-1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
			};

			int[] squareIndicies = new int[] {
					0, 1, 3,
					1, 2, 3
			};
			square = new Mesh(gl, squareVertecies, squareIndicies);

			// texturile noastre Render-To-Texture
			reflectionTex = new RenderToTexture(gl, getWidth(), getHeight());
			refractionTex = new RenderToTexture(gl, getWidth(), getHeight());
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e, "Error!", JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		}

		// Setarea culorii clare - culoarea care va fi folosită pentru a șterge pânza.
		gl.glClearColor(.8f, .8f, 1.f, 1.f);

		// Activarea eliminării feței din spate
		gl.glEnable(GL2.GL_CULL_FACE);
		gl.glCullFace(GL2.GL_BACK);
		gl.glFrontFace(GL2.GL_CCW);

		// Activarea verificării adâncimii
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LESS);

		// Activare Clipping
		// Clipping ne permite să eliminăm pixeli deasupra sau sub o anumită înălțime.
		// gl.glEnable(GL3.GL_CLIP_DISTANCE0);

		// inițializarea matricei de perspectivă
		FloatUtil.makePerspective(perspective, 0, true, 3.14159265358979323846f / 4,
				(float) getWidth() / (float) getHeight(), 0.1f, 10000.0f);
	}

	float distortionMove = 0;

	// In aceasta sectiune incepem sa construim obiectele
	public void display(GLAutoDrawable canvas) {
		GL2 gl = canvas.getGL().getGL2();

		// Actualizarea variabilei de deplasare a distorsiunii
		distortionMove += 0.002f;

		/************************** Scena Reflexiei *********************************/
		lookAt(x, y - (y - square.y) * 2, z, 0.0f, square.y, 0.0f, 0.0f, 1.0f, 0.0f);
		scale(Sx, Sy, Sz);
		rotate(Yangle, 0.0f, 1.0f, 0.0f);

		reflectionTex.bindBuffer();

		drawSkybox(gl);

		// utilizarea shaderului cliplambert
		cliplambert.use();

		// cliplambert shader nu rendeaza nimic sub apa
		// din moment ce este vorba de reflexia apei, nu dorim să rendam nimic sub apa

		gl.glUniform4f(gl.glGetUniformLocation(currentShaderProgram, "clipPlane"), 0, 1, 0, square.y);

		drawScene(gl);

		reflectionTex.unbindBuffer(getWidth(), getHeight());
		/************************** Scena Reflexiei *********************************/

		/************************** Scena Refractiei *********************************/
		lookAt(x, y, z, 0.0f, square.y, 0.0f, 0.0f, 1.0f, 0.0f);
		scale(Sx, Sy, Sz);
		rotate(Yangle, 0.0f, 1.0f, 0.0f);

		refractionTex.bindBuffer();

		drawSkybox(gl);

		// folosim cliplambert shader
		cliplambert.use();

		// cliplambert shader nu rendeaza nimic deasupra de apa
		// din moment ce este refracția apei, nu vrem să redăm nimic deasupra

		gl.glUniform4f(gl.glGetUniformLocation(currentShaderProgram, "clipPlane"), 0, -1, 0, square.y);

		drawScene(gl);

		refractionTex.unbindBuffer(getWidth(), getHeight());
		/************************** Scena Refractiei *********************************/

		/************************** Scena Actuala *********************************/
		lookAt(x, y, z, 0.0f, square.y, 0.0f, 0.0f, 1.0f, 0.0f);
		scale(Sx, Sy, Sz);
		rotate(Yangle, 0.0f, 1.0f, 0.0f);

		drawSkybox(gl);

		// folosim lambert shader
		lambert.use();

		drawScene(gl);
		/************************** Scena Actuala *********************************/

		/************************** Apa *********************************/
		// folosim water shader
		water.use();

		// se leagă textura refexiei
		gl.glActiveTexture(GL2.GL_TEXTURE0);
		reflectionTex.bindTexture();

		// se leagă textura refractiei
		gl.glActiveTexture(GL2.GL_TEXTURE1);
		refractionTex.bindTexture();

		// se leagă textura distorsionarii
		gl.glActiveTexture(GL2.GL_TEXTURE2);
		distortionTex.bind(gl);

		// restaurarea slotului de textură activ
		gl.glActiveTexture(GL2.GL_TEXTURE0);

		// setarea ID-urilor sloturilor de texturi
		gl.glUniform1i(gl.glGetUniformLocation(currentShaderProgram, "reflectionTex"), 0);
		gl.glUniform1i(gl.glGetUniformLocation(currentShaderProgram, "refractionTex"), 1);
		gl.glUniform1i(gl.glGetUniformLocation(currentShaderProgram, "distortionTex"), 2);

		// Actualizarea variabilei distorsionMove
		gl.glUniform1f(gl.glGetUniformLocation(currentShaderProgram, "distortionMove"), distortionMove);

		square.setPosition(0.0f, 3.0f, 0.0f);
		square.setScale(20.f, 20.f, 20.f);
		square.setRotation(3.14159265358979323846f / 2, 1.0f, 0.0f, 0.0f);
		square.draw();

		// dezasocierea texturilor de reflexie și refracție pentru a evita erorile de
		// framebuffer
		reflectionTex.unbindTexture();
		refractionTex.unbindTexture();
		/************************** Water *********************************/

		// Forțarea scenei pentru a fi rendata.
		gl.glFlush();
	}

	public void reshape(GLAutoDrawable canvas, int left, int top, int width, int height) {
		GL2 gl = canvas.getGL().getGL2();

		// Selectarea viewportului - zona de afișare - ca fiind întregul widget.
		gl.glViewport(0, 0, width, height);

		// Actualizarea perspectivei
		FloatUtil.makePerspective(perspective, 0, true, 3.14159265358979323846f / 4.0f, (float) width / (float) height,
				0.1f, 10000.0f);
	}

	public void displayChanged(GLAutoDrawable canvas, boolean modeChanged, boolean deviceChanged) {
		return;
	}

	@Override
	public void dispose(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub

	}

	void lookAt(float x, float y, float z, float Cx, float Cy, float Cz, float Ux, float Uy, float Uz) {
		// Actualizarea matricei de vizualizare/camera

		FloatUtil.makeLookAt(
				view, 0,
				new float[] { x, y, z }, 0,
				new float[] { Cx, Cy, Cz }, 0,
				new float[] { Ux, Uy, Uz }, 0,
				new float[4 * 4]);
	}

	void scale(float Sx, float Sy, float Sz) {
		// prin scalarea matricei de vizualizare/camera scalăm lumea

		float[] scaling = new float[4 * 4];

		FloatUtil.makeScale(scaling, true, Sx, Sy, Sz);
		FloatUtil.multMatrix(view, scaling, view);
	}

	void rotate(float angle, float Rx, float Ry, float Rz) {
		// Rotirea matricei de vizualizare/camera

		float[] rotation = new float[4 * 4];

		FloatUtil.makeRotationAxis(rotation, 0, angle, Rx, Ry, Rz, new float[3]);
		FloatUtil.multMatrix(view, rotation, view);
	}

	void drawSkybox(GL2 gl) {
		// desenăm skybox-ul separat de scenă pentru a rezolva problemele de reflexie
		// aceasta trebuie să fie apelată înainte de metoda drawScene

		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

		// activarea clockwise culling
		gl.glFrontFace(GL2.GL_CW);

		// folosirea no shading shader
		noshading.use();

		skyboxTex.bind(gl);
		cube.setRotation(-3.14159265358979323846f / 2, 1.0f, 0.0f, 0.0f);
		cube.setScale(100f / Sx, 100f / Sy, 100f / Sz);
		cube.setPosition(x, y, z);
		cube.draw();

		// restabilirea counter-clockwise culling
		gl.glFrontFace(GL2.GL_CCW);
	}

	void drawScene(GL2 gl) {
		// desenarea leu
		lionTex.bind(gl);
		lion.setPosition(40f, 3.7f, 3.0f);
		lion.setScale(2f, 2f, 2f);
		lion.setRotation(0.05f, 1.0f, 0.0f, 0.0f);
		lion.draw();

		// desenare tigru
		tigerTex.bind(gl);
		tiger.setPosition(-40.0f, 4.5f, 40.0f);
		tiger.setRotation(-0.15f, 1.0f, 0.0f, 0.0f);
		tiger.setScale(2f, 2f, 2f);
		tiger.draw();

		// desenare crocodil
		alligatorTex.bind(gl);
		alligator.setPosition(-20.0f, 3.8f, -4.0f);
		alligator.setScale(2.5f, 2.5f, 2.5f);
		alligator.setRotation(-3.14159265358979323846f / 2, 0.4f, 1.0f, 0.4f);
		alligator.draw();

		// desenare pamant-land
		landTex.bind(gl);
		land.setScale(.5f, 0.5f, 0.5f);
		land.draw();

		// desenare pomi
		drawTree(gl, 0, 8, -40);
		drawTree(gl, 20, 8, -25);
		drawTree(gl, 37, 4, 46);
		drawTree(gl, -54, 4, 42);
		drawTree(gl, -45, 4, 10);
		drawTree(gl, 45, 4, 20);
		drawTree(gl, 40, 4, -40);
		drawTree(gl, -42, 4, -36);
		// drawTree(gl, 4, 4, 30);
	}

	void drawTree(GL gl, float x, float y, float z) {
		treeTex.bind(gl);
		tree.setPosition(x, y, z);
		tree.setScale(2.f, 2.f, 2.f);
		tree.draw();
	}

	@Override
	public void mouseDragged(java.awt.event.MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			float amountX = (mouseLastX - e.getX()) * 0.01f;
			float amountY = (mouseLastY - e.getY()) * 0.7f;

			// deplasarea camerei în jurul scenei
			Yangle += amountX;
			y -= amountY;
		}
		mouseLastX = e.getX();
		mouseLastY = e.getY();
	}

	@Override
	public void mouseMoved(java.awt.event.MouseEvent e) {
		mouseLastX = e.getX();
		mouseLastY = e.getY();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {

		// am dimensionat întreaga lume pentru a da iluzia de a intra și ieși din ea.
		float amount = e.getScrollAmount() * 0.03f;
		if (e.getWheelRotation() == -1) {
			// zoom in
			Sx += amount;
			Sy += amount;
			Sz += amount;
		} else {
			// zoom out
			Sx -= amount;
			Sy -= amount;
			Sz -= amount;
		}
	}

	// variabilă utilizată pentru a calcula valoarea mișcării mouse-ului
	int mouseLastX = 0;
	int mouseLastY = 0;

	// avem nevoie de aceste variabile pentru a calcula transformarea camerei
	// Se pot modifica valorile implicite ale coordonatelor xyz.
	float x = 30, y = 20, z = 30,
			Sx = 1.0f, Sy = 1.0f, Sz = 1.0f,
			Yangle = 0.0f;

	private GLCanvas canvas;
	private Animator animator;

	// shaders
	private ShaderProgram lambert;
	private ShaderProgram water;
	private ShaderProgram noshading;
	private ShaderProgram cliplambert;

	// texturi
	private Texture lionTex;
	private Texture tigerTex;
	private Texture alligatorTex;
	private Texture landTex;
	private Texture distortionTex;
	private Texture skyboxTex;
	private Texture treeTex;

	// meshes
	private Mesh lion;
	private Mesh tiger;
	private Mesh alligator;
	private Mesh land;
	private Mesh tree;
	private Mesh square;
	private Mesh cube;

	// texturi de reflexie și refracție a apei
	private RenderToTexture reflectionTex;
	private RenderToTexture refractionTex;

	// matriciile de perspectivă și de vizualizare sunt transmise programului de
	// vertexuri de către programul Mesh
	// clasa
	static public float[] perspective = new float[4 * 4];
	static public float[] view = new float[4 * 4];

	// referință publică statică la programul de umbrire utilizat în prezent
	// necesar pentru clasa Mesh
	static public int currentShaderProgram;
}