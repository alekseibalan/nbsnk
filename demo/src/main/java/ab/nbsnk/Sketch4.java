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

import ab.jnc3.Screen;
import ab.nbsnk.nodes.Shapes;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Sketch4 {

  public static void main(String[] args) {
    boolean[] open = {true};
    Screen screen = new Screen();
    screen.image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
    screen.preferredSize = new Dimension(640, 360);
    Engine3d engine3d = new EngineDual(new EngineNbs(), new EngineFx()).open(screen.image)
        .setFarClip(80);
    engine3d.shape(new Shapes.Sphere().interpolateNormal()).setSpecular(0xFFFFFFFF, 200).translation(0, 0, -5);
    engine3d.shape(new Shapes.Square().scale(0.5)).setColor(0x80FF0000).translation(0.5, 0.5, -4);
    engine3d.shape(new Shapes.Square()).setColor(0x8000FF00).translation(1.2, 0.9, -4.1);
    int lights = 3;
    double r = 20;
    for (int i = 0; i < lights; i++) {
      double a = i * 2 * Math.PI / lights;
      engine3d.light().setColor(0xFF404040).translation(r * Math.sin(a), r * Math.cos(a), 15);
    }
    screen.gameController = true;
    screen.keyListener = key -> {
      if (key.equals("Esc")) open[0] = false;
    };
    while (open[0]) {
      engine3d.update();
      screen.update();
      try { Thread.sleep(20); } catch (InterruptedException ignore) {}
    }
    engine3d.close();
    screen.close();
  }

}
