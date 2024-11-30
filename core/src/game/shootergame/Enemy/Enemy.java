package game.shootergame.Enemy;

public interface Enemy {
    public void update(float delta);
    public void tickPathing();
    public void onKill();
    public boolean isAlive();
    public boolean isAggro();
}
