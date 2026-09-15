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
public class Mesh implements AutoCloseable {

    private int points;
    private int vertexArray;
    private int buffer;

    public Mesh(float[] points) {
        this.points = points.length / 3;
        vertexArray = glGenVertexArrays();
        glBindVertexArray(vertexArray);
        buffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        glBufferData(GL_ARRAY_BUFFER, points, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    @Override
    public void close() {
        glDeleteBuffers(buffer);
        glDeleteVertexArrays(vertexArray);
    }

    public void draw() {
        glBindVertexArray(vertexArray);
        glDrawArrays(GL_TRIANGLES, 0, points);
        glBindVertexArray(0);
    }
}
