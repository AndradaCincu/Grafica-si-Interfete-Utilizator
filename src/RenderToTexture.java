import com.jogamp.opengl.GL2;

// avem nevoie de această clasă pentru texturile noastre de reflexie și refracție
public class RenderToTexture {

    public RenderToTexture(GL2 gl, int width, int height) {
        this.gl = gl;
        this.width = width;
        this.height = height;

        // generează spatiul pentru o noua scena
        // frame-ul stochează suprafețele pe care le redăm
        int tmp[] = new int[1];
        gl.glGenFramebuffers(1, tmp, 0);
        GLBuffer = tmp[0];

        // bind it
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, GLBuffer);

        // aici creăm textura care va fi legată de buffer

        gl.glGenTextures(1, tmp, 0);
        GLTexture = tmp[0];

        gl.glBindTexture(GL2.GL_TEXTURE_2D, GLTexture);

        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGB, width, height, 0, GL2.GL_RGB, GL2.GL_UNSIGNED_BYTE, null);

        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);

        // legam textura noastră de buffer-ul scenei
        gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL2.GL_TEXTURE_2D, GLTexture, 0);

        // legam buffer-ul de scena la locația 0 pentru ieșirea pixelului umberi la
        // output
        tmp[0] = GL2.GL_COLOR_ATTACHMENT0;
        gl.glDrawBuffers(1, tmp, 0);

        // restabilește bufferul implicit de cadre
        // nu vrem să redăm din greșeală pe textura noastră
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
    }

    void bindTexture() {
        gl.glBindTexture(GL2.GL_TEXTURE_2D, GLTexture);
    }

    void unbindTexture() {
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
    }

    void bindBuffer() {
        // legam bufferul de scena al texturii noastre cu dimensiunile acesteia
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, GLBuffer);
        gl.glViewport(0, 0, width, height);
    }

    void unbindBuffer(int width, int height) {
        // restabilim bufferul de scena implicit cu dimensiunile sale
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
        gl.glViewport(0, 0, width, height);
    }

    int GLTexture, GLBuffer;
    int width, height;
    GL2 gl;
}