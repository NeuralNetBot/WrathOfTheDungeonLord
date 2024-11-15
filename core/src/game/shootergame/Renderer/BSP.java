package game.shootergame.Renderer;

import java.util.ArrayList;

import game.shootergame.Wall;

public class BSP {
    public class PartitionLine {
        public float pointX, pointY, dirX, dirY;
    }
    
    public class BSPNode {
        public PartitionLine line;
        public BSPNode front = null;
        public BSPNode back = null;
        ArrayList<Integer> walls;
    }

    public BSP(ArrayList<Wall> walls, ArrayList<PartitionLine> lines) {
        
    }
}
