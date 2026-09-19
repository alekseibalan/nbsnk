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

import org.joml.Matrix4f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.function.UnaryOperator;

import static org.lwjgl.opengl.GL33C.*;

public class Program implements AutoCloseable {

  final int program;
  Map<String, Integer> uniforms = new LinkedHashMap<>();

  public Program(String vs, String fs, String... uniforms) {
    program = glCreateProgram();
    if (program == 0) throw new IllegalStateException();
    List<Integer> shaders = new ArrayList<>();
    if (vs != null) shaders.add(createShader(vs, GL_VERTEX_SHADER));
    if (fs != null) shaders.add(createShader(fs, GL_FRAGMENT_SHADER));
    glLinkProgram(program);
    if (glGetProgrami(program, GL_LINK_STATUS) == 0) throw new IllegalStateException();
    for (int shader : shaders) glDetachShader(program, shader);
    for (int shader : shaders) glDeleteShader(shader);
    for (String uniform : uniforms) {
      int uniformLocation = glGetUniformLocation(program, uniform);
      if (uniformLocation < 0) throw new IllegalStateException();
      this.uniforms.put(uniform, uniformLocation);
    }
  }

  @Override
  public void close() {
    glUseProgram(0);
    glDeleteProgram(program);
    uniforms = null;
  }

  public static Program newDefault() {
    UnaryOperator<String> r = path -> {
      try {
        return new String(Program.class.getResourceAsStream(path).readAllBytes());
      } catch (IOException | NullPointerException e) {
        throw new MissingResourceException("", "", path);
      }
    };
    return new Program(r.apply("vs.txt"), r.apply("fs.txt"), "projectionMatrix", "viewMatrix", "modelMatrix");
  }

  public void use(Matrix4f... values) {
    glUseProgram(program);
    float[] floats = new float[16];
    int i = 0;
    for (int uniform : uniforms.values()) {
      if (i >= values.length) break;
      glUniformMatrix4fv(uniform, false, values[i++].get(floats));
    }
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
