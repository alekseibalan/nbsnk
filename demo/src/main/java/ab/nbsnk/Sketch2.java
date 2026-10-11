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

import ab.jnc3.Screen;
import ab.nbsnk.nodes.Shapes;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Sketch2 {

  public static void renderNoise(BufferedImage image) {
    int w = image.getWidth();
    int h = image.getHeight();
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int i = Math.max(0, 0x7F - x / 5 - y);
        int r = (int) (0x1F * (Math.sin((x + y) / 40.0) + 1));
        int g = (int) (0x1F * (Math.sin((x - y) / 40.0) + 1));
        int b = 0x3F - r;
        image.setRGB(x, y, (r << 16 | g << 8 | b | 0xFF404040) + i * 0x010101);
      }
    }
  }

  public static Obj photosphere(double size) {
    Obj sphere = Obj.load(Paths.get("assets/blender_uv_sphere.obj"));
    // invert normals
    for (int i = 0; i < sphere.face.length; i += 9) {
      int v = sphere.face[i + 3];
      sphere.face[i + 3] = sphere.face[i + 6];
      sphere.face[i + 6] = v;
      int t = sphere.face[i + 5];
      sphere.face[i + 5] = sphere.face[i + 8];
      sphere.face[i + 8] = t;
    }
    // rotate so texture starts at +z
    for (int i = 0; i < sphere.vertex.length; i += 3) {
      double x = sphere.vertex[i];
      double z = sphere.vertex[i + 2];
      sphere.vertex[i] = z;
      sphere.vertex[i + 2] = -x;
    }
    Obj.flatNormal(sphere);
    Obj.interpolateNormal(sphere);
    for (int i = 0; i < sphere.texture.length; i += 2) sphere.texture[i] = 1 - sphere.texture[i];
    for (int i = 0; i < sphere.vertex.length; i++) sphere.vertex[i] *= size;
    return sphere;
  }

  public static void main(String[] args) {
    Obj teapot = Obj.load(Paths.get("assets/teapot.obj"));
    //Obj.flatNormal(teapot);
    Obj cow = Obj.load(Paths.get("assets/cow.obj"));
    Obj.fixNormal(cow);
    BufferedImage cowImage = Obj.image(Paths.get("assets/cow.png"));
    boolean useSphere = false;
    Obj sphere = null;
    BufferedImage sphereImage = null;
    if (useSphere) {
      sphere = photosphere(80);
      sphereImage = Obj.image(Paths.get("assets/photosphere.jpg"));
    }

    Screen screen = new Screen();
//    screen.image = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
//    screen.preferredSize = new Dimension(1920, 1080);
    screen.image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
    screen.preferredSize = new Dimension(640, 360);
    int screenHeight = screen.image.getHeight();
    int screenWidth = screen.image.getWidth();
    BufferedImage background = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_ARGB);
    renderNoise(background);
//    Engine3d engine3d = new EngineFx().open(screen.image);
//    Engine3d engine3d = new EngineNbs().open(screen.image);
    FpsMeter fpsMeter = new FpsMeter();
    Engine3d engine3d = new EngineDual(new EngineNbs(), new EngineLw()).open(screen.image)
        .textSupplier(() -> String.format("fps: %.0f", fpsMeter.getFps())).setFarClip(80)
        .setAmbient(0xFF221100).background(background);
    // teapot test
    engine3d.shape(teapot).translation(-10, 8, -40);
    engine3d.shape(teapot).translation(-10, 4, -40).rotation(0.0, 0.0, 0.1); // positive roll
    engine3d.shape(teapot).translation(-10, 0, -40).rotation(0.0, 0.1, 0.0); // positive pitch
    engine3d.shape(teapot).setSpecular(0xFFFFFFFF, 20).translation(-10, -4, -40).rotation(0.1, 0.0, 0.0); // positive yaw
    engine3d.shape(teapot).setSpecular(0xFF0000FF, 1).translation(-10, -8, -40).rotation(0.25, 0.1, 0.0); // yaw 1/4 then pitch
    engine3d.shape(cow).setDiffuseMap(cowImage).setSpecular(0xFF003FFF, 20).translation(5, 4, -20);
    engine3d.shape(teapot).translation(10, 4, -40).rotation(0.25, 0.0, 0.1); // yaw 1/4 then roll
    engine3d.shape(teapot).translation(10, 0, -40).rotation(0.0, 0.25, 0.25); // pitch 1/4 then roll 1/4
    Engine3d.Shape sphere0 = null;
    if (useSphere) sphere0 = engine3d.shape(sphere).setDiffuseMap(sphereImage).selfIllumination(-1);
    Engine3d.Node t9 = engine3d.shape(teapot).translation(10, -8, -40);
    // pivot test
    //Engine3d.Node superCow = engine3d.shape(cow).setDiffuseMap(cowImage)
    //    .setColor(0xFF80FF40).translation(5, 0, 0).rotation(0.5, 0, 0).setPivot()
    //    .translation(0, -4, 0).rotation(0, 0.5, 0.5).setPivot().translation(0, 0, -20);
    // pivoting was resetting the zero point (origin), it can be achieved by creating a new group
    // and connecting the current node to it, therefore deprecated
    Engine3d.Node sc1 = engine3d.shape(cow).setDiffuseMap(cowImage)
        .setColor(0xFF80FF40).translation(5, 0, 0).rotation(0.5, 0, 0);
    Engine3d.Group sc2 = engine3d.group();
    sc1.connect(sc2);
    sc2.translation(0, -4, 0).rotation(0, 0.5, 0.5);
    Engine3d.Group sc3 = engine3d.group();
    sc2.connect(sc3);
    sc3.translation(0, 0, -20);
    Engine3d.Group superCow = sc3;
    // more cubes
    Obj cube = new Shapes.Cube();
    engine3d.shape(cube).translation(20, 0, 0);
    engine3d.shape(cube).translation(-20, 0, 0);
    engine3d.shape(cube).translation(0, 0, 20);
    // light
    engine3d.light().setColor(-1).translation(-100, 0, 15);
    Engine3d.Node light1 = engine3d.light().setColor(0xFFFFFF00).translation(0, 0, -20);
    Engine3d.Shape light1o = engine3d.shape(new Shapes.Icosahedron())
        .setDiffuseMap(Shapes.image()).selfIllumination(0xFFFFFF00);

    // legacy test
    Engine3d.Shape c0 = engine3d.shape(cube);
    c0.translation(-4, 0, -20);
    c0.rotation(0.0, 0.0, 10 / 360.0);

    Engine3d.Group g0 = engine3d.group();
    g0.translation(4, 0, -20);
    engine3d.shape(cube).setColor(0xFFFF4080).translation(0, 1.5, 0).connect(g0);
    engine3d.shape(cube).translation(0, -1.5, 0).connect(g0);
    g0.rotation(0.0, 0.0, 10 / 360.0);

    boolean[] open = {true};
    Queue<Integer> sysex = new LinkedBlockingQueue<>();

    AtomicInteger cameraTx = new AtomicInteger(-130);
    AtomicInteger cameraTy = new AtomicInteger(200);
    AtomicInteger cameraTz = new AtomicInteger(-22);
    AtomicInteger cameraRy = new AtomicInteger(3600);
    AtomicInteger cameraRp = new AtomicInteger();
    AtomicInteger cameraRr = new AtomicInteger();
    //screen.enablePointer();
    screen.gameController = true;
    boolean[] mouseButton = new boolean[10];
    screen.keyListener = key -> {
      if (key.equals("Esc")) open[0] = false;
      if (key.length() == 1) sysex.add((int) key.charAt(0));
      if (key.startsWith("Mouse")) {
        char bw = key.charAt(6);
        int button = key.charAt(7) - '0';
        boolean buttonOn = key.charAt(5) == '+';
        if (bw == 'B') {
          mouseButton[button] = buttonOn;
          if (buttonOn && button == 4) cameraRr.addAndGet(-1);
          if (buttonOn && button == 5) cameraRr.addAndGet(1);
        }
        if (bw == 'W') cameraTz.addAndGet(button * (buttonOn ? 1 : -1));
        if (bw <= '9') {
          String[] xys = key.substring(5).split(",");
          if (mouseButton[1]) {
            cameraRy.addAndGet(Integer.parseInt(xys[0]));
            cameraRp.addAndGet(Integer.parseInt(xys[1]));
          }
          if (mouseButton[3]) {
            cameraTx.addAndGet(Integer.parseInt(xys[0]));
            cameraTy.addAndGet(Integer.parseInt(xys[1]));
          }
        }
      }
    };

    while (open[0]) {
      long m = Instant.now().toEpochMilli();
      g0.rotation(0.0, 0.0, m % 3600 / 10.0 / 360.0);
      t9.rotation(0.0, 0.0, m % 3600 / 10.0 / 360.0);
      superCow.rotation(m % 3600 / 10.0 / 360.0, 0, 0);
      double osc1 = Math.cos(m % 7200 / 3600.0 * Math.PI);
      c0.translation(-4, osc1, -20);
      light1.translation(osc1 * 5, osc1 * 5, -40);
      light1o.translation(osc1 * 5, osc1 * 5, -40);
      engine3d.camera().translation(cameraTx.get() / -50.0, cameraTy.get() / 50.0, cameraTz.get())
          .rotation(cameraRy.get() / 10000.0, cameraRp.get() / -10000.0, cameraRr.get() / 16.0);
      if (useSphere) sphere0.translation(cameraTx.get() / -50.0, cameraTy.get() / 50.0, cameraTz.get());
      while (!sysex.isEmpty()) engine3d.sysex(sysex.remove());
      engine3d.update();
      screen.update();
      try { Thread.sleep(20); } catch (InterruptedException ignore) {}
    }
    engine3d.close();
    screen.close();
  }

}
