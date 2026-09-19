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
  Mesh mesh;
  Program program;
  Matrix4f projectionMatrix = new Matrix4f()
      .setPerspective((float) Math.toRadians(60.0f), (float) 4 / 3, 0.01f, 1000.f);
  Matrix4f modelMatrix = new Matrix4f().identity();
  Matrix4f viewMatrix = new Matrix4f().identity().translate(0, 0, -5);

  public LwDemo() {
    mesh = new Mesh(Obj.load(Paths.get("assets/teapot.obj")));
    program = Program.newDefault();
  }

  @Override
  public void close() {
    program.close();
    mesh.close();
  }

  public void draw() {
    glClearColor(0.5f, 0.5f, 0, 0);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    program.use(projectionMatrix, viewMatrix, modelMatrix);
    mesh.draw();
  }
}
