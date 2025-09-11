package Dragon.models.matches;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TOP {
    private String name;
    private byte gender;
    private short head;
    private short body;
    private short leg;
    private long power;
    private long ki;
    private long hp;
    private long sd;
    private byte nv;
    private byte subnv;
    private int sk;
    private int pvp;
    private int id_player;
    private String info1;
    private String info2;
    public int rank;
}
