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

import java.awt.image.BufferedImage;

import static org.lwjgl.opengl.GL33C.*;

public class Texture implements AutoCloseable {

  private int texture;

  public Texture(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    int[] raster = new int[width * height];
    for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
      int rgb = image.getRGB(x, y);
      raster[y * width + x] = rgb & 0xFF00FF00 | rgb << 16 & 0xFF0000 | rgb >> 16 & 0xFF;
    }
    texture = glGenTextures();
    glBindTexture(GL_TEXTURE_2D, texture);
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, raster);
    glGenerateMipmap(GL_TEXTURE_2D);
  }

  @Override
  public void close() {
    glDeleteTextures(texture);
  }

  public void bind() {
    glBindTexture(GL_TEXTURE_2D, texture);
  }

  public static void unbind() {
    glBindTexture(GL_TEXTURE_2D, 0);
  }

}
