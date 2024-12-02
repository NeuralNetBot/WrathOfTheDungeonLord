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
    ITEM_INTERACT,
    ENEMY_DAMAGE,
    LOAD_MAP,
    READY_PLAY;

    public static PacketInfo getType(byte type) {
        switch (type) {
        case 0x00: return PLAYER_POSITION;
        case 0x01: return NEW_PLAYER;
        case 0x02: return ENEMY_UPDATE;
        case 0x03: return NEW_ENEMY;
        case 0x04: return NEW_ITEM;
        case 0x05: return PLAYER_UPDATE;
        case 0x06: return PLAYER_ATTACK;
        case 0x07: return ITEM_INTERACT;
        case 0x08: return ENEMY_DAMAGE;
        case 0x10: return LOAD_MAP;
        case 0x11: return READY_PLAY;
        default:   return UNKNOWN;
        }
    }

    public static byte getByte(PacketInfo type) {
        switch (type) {
        case PLAYER_POSITION: return 0x00;
        case NEW_PLAYER:      return 0x01;
        case ENEMY_UPDATE:    return 0x02;
        case NEW_ENEMY:       return 0x03;
        case NEW_ITEM:        return 0x04;
        case PLAYER_UPDATE:   return 0x05;
        case PLAYER_ATTACK:   return 0x06;
        case ITEM_INTERACT:   return 0x07;
        case ENEMY_DAMAGE:    return 0x08;
        case LOAD_MAP:        return 0x10;
        case READY_PLAY:      return 0x11;
        default:              return (byte)0xFF;
        }
    }
}
