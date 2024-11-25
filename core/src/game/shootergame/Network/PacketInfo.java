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
        default:              return (byte)0xFF;
        }
    }
}
