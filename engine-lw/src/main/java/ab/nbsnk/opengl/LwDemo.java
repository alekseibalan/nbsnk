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

import static org.lwjgl.opengl.GL33C.*;

// FIXME: 2026-09-16 slop
public class LwDemo implements AutoCloseable {
  public static final String SCENE_VERT = "#version 330\n" +
      "\n" +
      "layout (location=0) in vec3 inPosition;\n" +
      "\n" +
      "void main()\n" +
      "{\n" +
      "    gl_Position = vec4(inPosition, 1.0);\n" +
      "}";
  public static final String SCENE_FRAG = "#version 330\n" +
      "\n" +
      "out vec4 fragColor;\n" +
      "\n" +
      "void main()\n" +
      "{\n" +
      "    fragColor = vec4(0.0, 1.0, 1.0, 1.0);\n" +
      "}";
  Mesh mesh;
  Program program;

  public LwDemo() {
    float[] positions = new float[]{
        0.0f, 0.5f, 0.0f,
        -0.5f, -0.5f, 0.0f,
        0.5f, -0.5f, 0.0f
    };
    mesh = new Mesh(positions);
    program = new Program(SCENE_VERT, SCENE_FRAG);
  }

  @Override
  public void close() {
    program.close();
    mesh.close();
  }

  public void draw() {
    glClearColor(0.5f, 0.5f, 0, 0);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    program.use();
    mesh.draw();
  }
}
