package game.shootergame.Enemy;

public interface Enemy {
    public void update(float delta);
    public void updateFromNetwork(float x, float y, float z, float dx, float dy, float rotation, float health);
    public void tickPathing();
    public void onKill();
    public boolean isAlive();
    public boolean isAggro();
    public float getX();
    public float getY();
    public float getZ();
    public float getDX();
    public float getDY();
    public float getRotation();
    public float getHealth();
    public void doDamage(float damage);
    public String getName();
    //only valid for remote enemies
    public float getRecentDamage();
}
