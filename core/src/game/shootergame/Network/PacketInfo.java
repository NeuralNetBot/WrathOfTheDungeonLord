package game.shootergame.Network;

public enum PacketInfo {
    UNKNOWN,
    PLAYER_POSITION,
    NEW_PLAYER,
    ENEMY_UPDATE,
    NEW_ENEMY,
    NEW_ITEM,
    PLAYER_UPDATE,
    PLAYER_ATTACK,
    ITEM_INTERACT;

    static PacketInfo getType(byte type) {
        switch (type) {
        case 0x00: return PLAYER_POSITION;
        case 0x01: return NEW_PLAYER;
        case 0x02: return ENEMY_UPDATE;
        case 0x03: return NEW_ENEMY;
        case 0x04: return NEW_ITEM;
        case 0x05: return PLAYER_UPDATE;
        case 0x06: return PLAYER_ATTACK;
        case 0x07: return ITEM_INTERACT;
        default:   return UNKNOWN;
        }
    }
}
