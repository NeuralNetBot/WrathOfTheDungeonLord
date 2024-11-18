package game.shootergame.Renderer;

import java.util.ArrayList;

public class RegionIndexCuller {
    static public class Region {
        public float minX, minY, maxX, maxY;
        public ArrayList<Integer> indices;
        public Region(float minX, float minY, float maxX, float maxY) { this.minX = minX; this.minY = minY; this.maxX = maxX; this.maxY = maxY; indices = new ArrayList<>(); }
    };

    public ArrayList<Region> regions;

    public RegionIndexCuller() {
        this.regions = new ArrayList<>();
    }

    public ArrayList<Region> getContainedRegions(float x, float y) {
        ArrayList<Region> containedRegions = new ArrayList<>();

        for (Region region : regions) {
            if(x > region.minX && x < region.maxX && y > region.minY && y < region.maxY) {
                containedRegions.add(region);
            }
        }

        return containedRegions;
    }
}
