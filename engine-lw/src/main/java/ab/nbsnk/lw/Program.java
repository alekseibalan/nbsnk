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

package ab.nbsnk.lw;

import org.joml.Matrix4f;
import org.joml.Vector3f; // FIXME joml coupling
import org.joml.Vector4f;

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
    for (String uniform : uniforms) getUniformLocation(uniform);
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
    return new Program(r.apply("ovs.txt"), r.apply("ofs.txt"));
  }

  public void use(Object... values) {
    glUseProgram(program);
    int i = 0;
    for (int uniform : uniforms.values()) {
      if (i >= values.length) break;
      set(uniform, values[i++]);
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

  void set(int location, Object v) {
    if (v instanceof Matrix4f) {
      float[] floats = new float[16];
      glUniformMatrix4fv(location, false, ((Matrix4f) v).get(floats));
    } else if (v instanceof Vector4f) glUniform4f(location, ((Vector4f) v).x, ((Vector4f) v).y, ((Vector4f) v).z, ((Vector4f) v).w);
    else if (v instanceof Vector3f) glUniform3f(location, ((Vector3f) v).x, ((Vector3f) v).y, ((Vector3f) v).z);
    else if (v instanceof Integer) glUniform1i(location, (Integer) v);
    else throw new IllegalStateException(v.getClass().getSimpleName());
  }

  int getUniformLocation(String uniformName) {
    return uniforms.computeIfAbsent(uniformName, name -> {
      int location = glGetUniformLocation(program, name);
      if (location < 0) throw new IllegalStateException(name);
      return location;
    });
  }

  public void set(String uniformName, Object value) {
    set(getUniformLocation(uniformName), value);
  }

}
