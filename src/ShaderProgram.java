import java.nio.file.Files;
import java.nio.file.Paths;

import com.jogamp.opengl.*;

//nu putem folosi clasa com.jogamp.opengl.util.glsl.ShaderProgram ca clasă de program de umbrire, deoarece este pentru GLES2.
public class ShaderProgram {

    // încarcam vertex shader-ul și fragment shader-ul
    // 'String name' este numele comun al fișierului atât pentru vertex shader cât
    // și pentru fragment shader, fără extensie.
    // constructorul caută două fișiere cu extensiile .vs și .fs
    public ShaderProgram(GL2 gl, String name) throws Exception {
        this.gl = gl;

        // cream shaderele
        int vs = createShader(name + ".vs", GL2.GL_VERTEX_SHADER);
        int fs = createShader(name + ".fs", GL2.GL_FRAGMENT_SHADER);

        // ne creăm programul și legăm shaderele
        GLProgram = gl.glCreateProgram();
        gl.glAttachShader(GLProgram, vs);
        gl.glAttachShader(GLProgram, fs);
        gl.glLinkProgram(GLProgram);

        // nu mai avem nevoie de shaders
        gl.glDeleteShader(vs);
        gl.glDeleteShader(fs);
    }

    // destructor
    protected void finalize() {
        // ștergem programul nostru
        gl.glDeleteProgram(GLProgram);
    }

    public void use() {
        // utilizam programul nostru
        gl.glUseProgram(GLProgram);

        // actualizam programul de shader utilizat în prezent
        MainFrame.currentShaderProgram = GLProgram;
    }

    private int createShader(String file, int shaderType) throws Exception {
        int shader = gl.glCreateShader(shaderType);
        String shaderCode = new String(Files.readAllBytes(Paths.get(file)));

        // check if shaderCode is empty
        if (shaderCode.isEmpty())
            throw new Exception("no code found in '" + file + "'.");

        // încărcăm și compilăm shaderul nostru
        gl.glShaderSource(shader, 1, new String[] { shaderCode }, new int[] { shaderCode.length() }, 0);
        gl.glCompileShader(shader);

        // verifică dacă compilarea shaderilor a eșuat prin obținerea statusului
        // GL_COMPILE_STATUS
        int tmp[] = new int[1];
        gl.glGetShaderiv(shader, GL2.GL_COMPILE_STATUS, tmp, 0);

        if (tmp[0] != GL2.GL_TRUE) {
            // obțineținem erorile compilatorului
            gl.glGetShaderiv(shader, GL2.GL_INFO_LOG_LENGTH, tmp, 0);
            byte errorLog[] = new byte[tmp[0]];
            gl.glGetShaderInfoLog(shader, tmp[0], tmp, 0, errorLog, 0);

            throw new Exception("could not compile shader '" + file + "'.\n" + new String(errorLog) + "error code: "
                    + gl.glGetError());
        }

        return shader;
    }

    int getProgram() {
        return GLProgram;
    }

    private int GLProgram;
    private GL2 gl;
}