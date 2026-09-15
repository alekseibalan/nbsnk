/*
 * Copyright (C) 2026 Aleksei Balan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ab.nbsnk.opengl;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL33C.*;

// FIXME: 2026-09-16 slop
public class Program implements AutoCloseable {

    final int program;

    public Program(String vs, String fs) {
        program = glCreateProgram();
        if (program == 0) throw new IllegalStateException();
        List<Integer> shaders = new ArrayList<>();
        if (vs != null) shaders.add(createShader(vs, GL_VERTEX_SHADER));
        if (fs != null) shaders.add(createShader(fs, GL_FRAGMENT_SHADER));
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == 0) throw new IllegalStateException();
        for (int shader : shaders) glDetachShader(program, shader);
        for (int shader : shaders) glDeleteShader(shader);
    }

    @Override
    public void close() {
        glUseProgram(0);
        glDeleteProgram(program);
    }

    public void use() {
        glUseProgram(program);
    }

    int createShader(String source, int type) {
        int shader = glCreateShader(type);
        if (shader == 0) throw new IllegalStateException();
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) throw new IllegalStateException();
        glAttachShader(program, shader);
        return shader;
    }
}
