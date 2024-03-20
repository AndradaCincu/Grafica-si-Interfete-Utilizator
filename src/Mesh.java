import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.io.ObjectInputStream;
import java.io.FileInputStream;

import com.jogamp.opengl.math.*;

import com.jogamp.opengl.GL2;

public class Mesh {

    public Mesh(GL2 gl, float[] vertexData, int[] indexData) throws Exception {
        this.gl = gl;

        initializeBuffers(vertexData, indexData);
    }

    public Mesh(GL2 gl, String file) throws Exception {
        this.gl = gl;

        // Aici obținem datele mesh prin intermediul obiectelor de date care au fost
        // preluate înainte
        // motivul pentru care facem acest lucru este acela că am folosit o bibliotecă
        // externă pentru a încărca datele
        // meshes
        // Am salvat datele încărcate într-un fișier și am eliminat biblioteca externă
        // din sursă

        FileInputStream fileInput = new FileInputStream(file);
        ObjectInputStream objectInput = new ObjectInputStream(fileInput);

        // deserealizarea obiectelor

        ArrayList<Object> data = (ArrayList<Object>) objectInput.readObject();

        // inchiderea handles-urilor
        fileInput.close();
        objectInput.close();

        // copier datelor
        // primul element din ArrayList este reprezentat de datele de vertex.
        // al doilea element este reprezentat de datele de indexare

        float[] vertexData = (float[]) data.get(0);
        int[] indexData = (int[]) data.get(1);
        this.indexCount = indexData.length;

        initializeBuffers(vertexData, indexData);
    }

    void draw() {
        float[] translation = new float[4 * 4];
        float[] scaling = new float[4 * 4];
        float[] rotation = new float[4 * 4];
        float[] meshMatrix = new float[4 * 4];

        // calculam transformarea mesh
        FloatUtil.makeTranslation(translation, true, x, y, z);
        FloatUtil.makeScale(scaling, true, Sx, Sy, Sz);
        FloatUtil.makeRotationAxis(rotation, 0, angle, Rx, Ry, Rz, new float[3]);

        FloatUtil.multMatrix(translation, scaling, meshMatrix);
        FloatUtil.multMatrix(meshMatrix, rotation, meshMatrix);

        // actualizează matricile de vertex shader
        gl.glUniformMatrix4fv(gl.glGetUniformLocation(MainFrame.currentShaderProgram, "model"), 1, false, meshMatrix,
                0);
        gl.glUniformMatrix4fv(gl.glGetUniformLocation(MainFrame.currentShaderProgram, "view"), 1, false, MainFrame.view,
                0);
        gl.glUniformMatrix4fv(gl.glGetUniformLocation(MainFrame.currentShaderProgram, "projection"), 1, false,
                MainFrame.perspective, 0);

        // legam bufferul nostru de vertex și index pentru desenare
        gl.glBindVertexArray(GLVerexBuffer);
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, GLIndexBuffer);

        // desenam intrigue mesh
        gl.glDrawElements(GL2.GL_TRIANGLES, indexCount, GL2.GL_UNSIGNED_INT, 0);
    }

    void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    void setScale(float Sx, float Sy, float Sz) {
        this.Sx = Sx;
        this.Sy = Sy;
        this.Sz = Sz;
    }

    void setRotation(float angle, float Rx, float Ry, float Rz) {
        this.angle = angle;
        this.Rx = Rx;
        this.Ry = Ry;
        this.Rz = Rz;
    }

    private void initializeBuffers(float[] vertexData, int[] indexData) {
        this.indexCount = indexData.length;

        // generează o matrice de vertexuri goală pentru a o lega mai târziu de bufferul
        // de vertexuri
        // înfășurăm bufferul de vertexuri cu un array de vertexuri, astfel încât să nu
        // mai fie nevoie să setăm
        // aspectul datelor de vertex de fiecare dată când dorim să desenăm

        int tmp[] = new int[1];
        gl.glGenVertexArrays(1, tmp, 0);
        GLVerexBuffer = tmp[0];

        // parted de legare
        gl.glBindVertexArray(GLVerexBuffer);

        // generează un buffer gol pe care îl vom folosi ca buffer de vertexuri
        gl.glGenBuffers(1, tmp, 0);

        // se leagă de matricea de vertexuri și apoi se copiază datele noastre de vertex
        // în ea
        FloatBuffer tmpBuffer1 = FloatBuffer.wrap(vertexData);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, tmp[0]);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, vertexData.length * Float.BYTES, tmpBuffer1, GL2.GL_STATIC_DRAW);

        // setați aspectul datelor pentru bufferul nostru de vertexuri
        // 0: poziție
        // 1: uv
        // 2: normal

        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GL2.GL_FLOAT, false, 8 * Float.BYTES, 0);

        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 2, GL2.GL_FLOAT, false, 8 * Float.BYTES, 3 * Float.BYTES);

        gl.glEnableVertexAttribArray(2);
        gl.glVertexAttribPointer(2, 3, GL2.GL_FLOAT, false, 8 * Float.BYTES, 5 * Float.BYTES);

        // generează un buffer gol pe care îl vom folosi ca buffer de indexare

        gl.glGenBuffers(1, tmp, 0);
        GLIndexBuffer = tmp[0];

        // se leagă și apoi se copiază datele indexului nostru în el
        IntBuffer tmpBuffer2 = IntBuffer.wrap(indexData);
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, GLIndexBuffer);
        gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, indexData.length * Integer.BYTES, tmpBuffer2, GL2.GL_STATIC_DRAW);

    }

    private int GLVerexBuffer;
    private int GLIndexBuffer;
    private int indexCount;
    public float x = 0.0f, y = 0.0f, z = 0.0f, Sx = 1.0f, Sy = 1.0f, Sz = 1.0f, angle = 0.0f, Rx = 0.0f, Ry = 0.0f,
            Rz = 0.0f;
    private GL2 gl;
}