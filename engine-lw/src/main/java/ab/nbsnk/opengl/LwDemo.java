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

import ab.nbsnk.Obj;
import org.joml.Matrix4f;

import java.nio.file.Paths;

import static org.lwjgl.opengl.GL33C.*;

// FIXME: 2026-09-16 slop
public class LwDemo implements AutoCloseable {
  public static final String VS = "#version 330\n" +
      "\n" +
      "layout (location=0) in vec3 position;\n" +
      "layout (location=1) in vec3 color;\n" +
      "\n" +
      "out vec3 outColor;\n" +
      "\n" +
      "uniform mat4 projectionMatrix;\n" +
      "uniform mat4 viewMatrix;\n" +
      "uniform mat4 modelMatrix;\n" +
      "\n" +
      "void main()\n" +
      "{\n" +
      "    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);\n" +
      "    outColor = color;\n" +
      "}";
  public static final String FS = "#version 330\n" +
      "\n" +
      "in  vec3 outColor;\n" +
      "out vec4 fragColor;\n" +
      "\n" +
      "void main()\n" +
      "{\n" +
      "    fragColor = vec4(outColor, 1.0);\n" +
      "}";
  Mesh mesh;
  Program program;
  Matrix4f projectionMatrix = new Matrix4f()
      .setPerspective((float) Math.toRadians(60.0f), (float) 4 / 3, 0.01f, 1000.f);
  Matrix4f modelMatrix = new Matrix4f().identity();
  Matrix4f viewMatrix = new Matrix4f().identity().translate(0, 0, -5);

  public LwDemo() {
    Obj obj = Obj.load(Paths.get("assets/teapot.obj"));
    float[] vertex = new float[obj.vertex.length];
    for (int i = 0; i < vertex.length; i++) vertex[i] = (float) obj.vertex[i] / 4f;
    mesh = new Mesh(vertex, Obj.copy(obj.face));
    program = new Program(VS, FS, "projectionMatrix", "modelMatrix", "viewMatrix");
  }

  @Override
  public void close() {
    program.close();
    mesh.close();
  }

  public void draw() {
    glClearColor(0.5f, 0.5f, 0, 0);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    program.use(projectionMatrix, modelMatrix, viewMatrix);
    mesh.draw();
  }
}
