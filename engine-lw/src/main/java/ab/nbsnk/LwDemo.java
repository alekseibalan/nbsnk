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

package ab.nbsnk;

import ab.nbsnk.lw.Mesh;
import ab.nbsnk.lw.Program;
import ab.nbsnk.lw.Texture;
import org.joml.Matrix4f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL33C.*;

// FIXME: 2026-09-16 slop
public class LwDemo implements AutoCloseable {
  Mesh mesh;
  Program program;
  Matrix4f projectionMatrix = new Matrix4f()
      .setPerspective((float) Math.toRadians(60.0f), (float) 4 / 3, 0.01f, 1000.f);
  Matrix4f modelMatrix = new Matrix4f().identity().rotateY(2.5f);
  Matrix4f viewMatrix = new Matrix4f().identity().translate(0, 0, -2);

  public static BufferedImage img(String file) {
    try {
      return ImageIO.read(Files.newInputStream(Paths.get(file)));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public LwDemo() {
    mesh = new Mesh(Obj.load(Paths.get("assets/cow.obj")));

    new Texture(img("assets/cow.png"));
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
//    program.set("material.diffuse", new Vector4f(0.0f, 1.0f, 0.0f, 1.0f));
    mesh.draw();
  }
}
