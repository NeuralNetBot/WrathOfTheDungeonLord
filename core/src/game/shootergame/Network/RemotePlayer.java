package game.shootergame.Network;

public class RemotePlayer {
    float x, y;
    float dx, dy;

    RemotePlayer() {

    }

    public void update(float delta) {
        x = x + dx * delta;
        y = y + dy * delta;

        System.out.println(x + " " + y);
    }

    public void updateNetwork(float x, float y, float dx, float dy) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
    }

    public void kill() {

    }
}
