/*
 * Copyright (C) 2025 Aleksei Balan
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

import ab.nbsnk.opengl.LwDemo;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL33C.*;

public class EngineLw implements Engine3d {

  public static final boolean FLIP_Y = true;
  private int screenWidth;
  private int screenHeight;
  private BufferedImage screenImage;
  private long windowHandle;
  private int[] pixelInts;
  LwDemo lwDemo;

  @Override
  public EngineLw open(BufferedImage image) {
    if (windowHandle != 0) throw new IllegalStateException();
    if (image.getType() != BufferedImage.TYPE_INT_RGB && image.getType() != BufferedImage.TYPE_INT_ARGB) throw new IllegalArgumentException();
    screenWidth = image.getWidth();
    screenHeight = image.getHeight();
    screenImage = image;
    if (!GLFW.glfwInit()) throw new IllegalStateException();
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
    windowHandle = GLFW.glfwCreateWindow(screenWidth, screenHeight, "", 0, 0);
    GLFW.glfwMakeContextCurrent(windowHandle);
    GL.createCapabilities();
    pixelInts = new int[screenWidth * (FLIP_Y ? 1: screenHeight)];
    lwDemo = new LwDemo();
    return this;
  }

  @Override
  public void close() {
    if (windowHandle == 0) return;
    lwDemo.close();
    GLFW.glfwDestroyWindow(windowHandle);
    GLFW.glfwTerminate();
    windowHandle = 0;
  }

  @Override
  public EngineLw background(BufferedImage image) {
    return this;
  }

  @Override
  public ShapeLw shape(Obj obj) {
    return new ShapeLw();
  }

  @Override
  public GroupLw group() {
    return new GroupLw();
  }

  @Override
  public LightLw light() {
    return new LightLw();
  }

  @Override
  public EngineLw setAmbient(int color) {
    return this;
  }

  @Override
  public NodeLw camera() {
    return new NodeLw();
  }

  @Override
  public EngineLw setFarClip(double value) {
    return this;
  }

  @Override
  public EngineLw setFocalLength(double value) {
    return this;
  }

  @Override
  public void update() {
    lwDemo.draw();
    if (FLIP_Y) {
      for (int i = 0, j = screenHeight - 1; i < screenHeight; i++, j--) {
        glReadPixels(0, i, screenWidth, 1, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, pixelInts);
        screenImage.getRaster().setDataElements(0, j, screenWidth, 1, pixelInts);
      }
    } else {
      glReadPixels(0, 0, screenWidth, screenHeight, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, pixelInts);
      screenImage.getRaster().setDataElements(0, 0, screenWidth, screenHeight, pixelInts);
    }
  }

  @Override
  public void sysex(int i) {

  }

  @Override
  public EngineLw textSupplier(Supplier<String> supplier) {
    return this;
  }

  public static class NodeLw implements Node {
    @Override
    public NodeLw translation(double x, double y, double z) {
      return this;
    }

    @Override
    public NodeLw rotation(double yaw, double pitch, double roll) {
      return this;
    }

    @Override
    public NodeLw setPivot() {
      return this;
    }

    @Override
    public NodeLw connect(Group node) {
      return this;
    }

    @Override
    public NodeLw setVisible(boolean value) {
      return this;
    }
  }

  public static class ShapeLw extends NodeLw implements Shape {
    @Override
    public ShapeLw setColor(int color) {
      return this;
    }

    @Override
    public ShapeLw setSpecular(int color, double power) {
      return this;
    }

    @Override
    public ShapeLw selfIllumination(int color) {
      return this;
    }

    @Override
    public ShapeLw setBumpMap(BufferedImage image) {
      return this;
    }

    @Override
    public ShapeLw setReflectionMap(BufferedImage image, double alpha, Node skybox) {
      return this;
    }
  }

  public static class GroupLw extends NodeLw implements Group {

  }

  public static class LightLw extends NodeLw implements Light {
    @Override
    public LightLw setColor(int color) {
      return this;
    }
  }

}
