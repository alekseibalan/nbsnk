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

import ab.nbsnk.Obj;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL33C.*;

public class Mesh implements AutoCloseable {

  private int points;
  private int vertexArray;
  private List<Integer> buffers = new ArrayList<>();

  public Mesh(Obj obj) {
    int length = obj.face.length / 3;
    int[] face = new int[length];
    float[] vertex = new float[length * 3];
    float[] texture = new float[length * 2];
    for (int i = 0, iv = 0, it = 0; i < length; i++) {
      // FIXME: 2026-09-20 shader must understand obj natively
      int sv = obj.face[i * 3] * 3;
      int st = obj.face[i * 3 + 2] * 2;
      vertex[iv++] = (float) obj.vertex[sv++];
      vertex[iv++] = (float) obj.vertex[sv++];
      vertex[iv++] = (float) obj.vertex[sv++];
      texture[it++] = (float) obj.texture[st++];
      texture[it++] = (float) (1 - obj.texture[st++]);
      face[i] = i;
    }

    this.points = face.length;
    vertexArray = glGenVertexArrays();
    glBindVertexArray(vertexArray);

    int buffer = glGenBuffers();
    buffers.add(buffer);
    glBindBuffer(GL_ARRAY_BUFFER, buffer);
    glBufferData(GL_ARRAY_BUFFER, vertex, GL_STATIC_DRAW);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

    buffer = glGenBuffers();
    buffers.add(buffer);
    glBindBuffer(GL_ARRAY_BUFFER, buffer);
    glBufferData(GL_ARRAY_BUFFER, texture, GL_STATIC_DRAW);
    glEnableVertexAttribArray(1);
    glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);

    buffer = glGenBuffers();
    buffers.add(buffer);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, face, GL_STATIC_DRAW);

    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
  }

  @Override
  public void close() {
    for (int buffer : buffers) glDeleteBuffers(buffer);
    glDeleteVertexArrays(vertexArray);
  }

  public void draw() {
    glBindVertexArray(vertexArray);
    glDrawElements(GL_TRIANGLES, points, GL_UNSIGNED_INT, 0);
    glBindVertexArray(0);
  }
}
