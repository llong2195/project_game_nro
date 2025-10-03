package Dragon.models.player;

import Dragon.models.item.Item;
import Dragon.services.Service;

public class SetClothes {

    private Player player;

    public SetClothes(Player player) {
        this.player = player;
    }


    public static int BONUS_DAMAGE_SONGOKU = 100;           // Sát thương x2 = +100%
    public static int BONUS_STUN_THIENXINHANG = 100;        // Choáng x2 = +100%  
    public static int BONUS_DAMAGE_KIRIN = 100;             // Sát thương x2 = +100%
    public static int BONUS_DAMAGE_OCTIEU = 100;            // Sát thương x2 = +100%
    public static int BONUS_DAMAGE_PIKKORO_DAIMAO = 100;    // Sát thương x2 = +100%
    public static int BONUS_DAMAGE_PICOLO = 100;            // Sát thương x2 = +100%
    public static int BONUS_DAMAGE_NAPPA = 100;             // Sát thương x2 = +100%
    public static int BONUS_DAMAGE_KAKAROT = 100;           // Sát thương x2 = +100%
    public static int BONUS_DAMAGE_CADIC = 100;             // Sát thương x2 = +100%

    public byte songoku;
    public int songokuParam; 
    public byte thienXinHang;
    public int thienXinHangParam;
    public byte kirin;
    public int kirinParam;

    public byte ocTieu;
    public int ocTieuParam;
    public byte pikkoroDaimao;
    public int pikkoroDaimaoParam;
    public byte picolo;
    public int picoloParam;

    public byte kakarot;
    public int kakarotParam; 
    public byte cadic;
    public int cadicParam;
    public byte nappa;
    public int nappaParam;

    public byte setBom;
    public byte set8;

    public byte tromcho;

    public byte worldcup;
    public byte setDHD;
    public byte thienSuClothes = 0;
    public boolean godClothes;
    public int ctHaiTac = -1;

    public void setup() {
        setDefault();
        setupSKT();
        this.godClothes = true;
        for (int i = 0; i < 5; i++) {
            Item item = this.player.inventory.itemsBody.get(i);
            if (item.isNotNullItem()) {
                if (item.template.id > 567 || item.template.id < 555) {
                    this.godClothes = false;
                    break;
                }
            } else {
                this.godClothes = false;
                break;
            }
        }
        Item ct = this.player.inventory.itemsBody.get(5);
        if (ct.isNotNullItem()) {
            switch (ct.template.id) {
                case 618:
                case 619:
                case 620:
                case 621:
                case 622:
                case 623:
                case 624:
                case 626:
                case 627:
                    this.ctHaiTac = ct.template.id;
                    break;

            }
        }
    }

    public boolean IsSetThienSu() {
        int[][] DoThienSu = new int[][]{
            {1048, 1051, 1054, 1057, 1060}, // td
            {1049, 1052, 1055, 1058, 1060}, // namec
            {1050, 1053, 1056, 1059, 1060},// xayda
        };
        int z = 0;
        for (int i = 0; i < 5; i++) {
            Item item = this.player.inventory.itemsBody.get(i);
            if (item.isNotNullItem()) {
                if (item.template.id == DoThienSu[this.player.gender][i]) {
                    z++;
                } else {
                    return false;
                }
            } else {
                return false;
            }

        }
        return z == 5;
    }

    public boolean IsSetHuyDiet() {
        int[][] DoHuyDiet = new int[][]{
            {650, 651, 657, 658, 656}, // td
            {652, 653, 659, 660, 656}, // namec
            {654, 655, 661, 662, 656},// xayda
        };
        int z = 0;
        for (int i = 0; i < 5; i++) {
            Item item = this.player.inventory.itemsBody.get(i);
            if (item.isNotNullItem()) {
                if (item.template.id == DoHuyDiet[this.player.gender][i]) {
                    z++;
                } else {
                    return false;
                }
            } else {
                return false;
            }

        }
        return z == 5;
    }

    private void setupSKT() {
        for (int i = 0; i < 5; i++) {
            Item item = this.player.inventory.itemsBody.get(i);
            if (item.isNotNullItem()) {
                boolean isActSet = false;
                for (Item.ItemOption io : item.itemOptions) {
                    switch (io.optionTemplate.id) {
                        case 141:
                            isActSet = true;
                            songoku++;
                            songokuParam = Math.min(songokuParam, io.param);
                            break;
                        case 139:
                            isActSet = true;
                            thienXinHang++;
                            thienXinHangParam = Math.min(thienXinHangParam, io.param);
                            break;
                        case 140:
                            isActSet = true;
                            kirin++;
                            kirinParam = Math.min(kirinParam, io.param);
                            break;
                        case 143:
                            isActSet = true;
                            ocTieu++;
                            ocTieuParam = Math.min(ocTieuParam, io.param);
                            break;
                        case 144:
                            isActSet = true;
                            pikkoroDaimao++;
                            pikkoroDaimaoParam = Math.min(pikkoroDaimaoParam, io.param);
                            break;
                        case 142:
                            isActSet = true;
                            picolo++;
                            picoloParam = Math.min(picoloParam, io.param);
                            break;
                        case 138:
                            isActSet = true;
                            nappa++;
                            nappaParam = Math.min(nappaParam, io.param);
                            break;
                        case 136:
                            isActSet = true;
                            kakarot++;
                            kakarotParam = Math.min(kakarotParam, io.param);
                            break;
                        case 137:
                            isActSet = true;
                            cadic++;
                            cadicParam = Math.min(cadicParam, io.param);
                            break;
                        case 189:
                            isActSet = true;
                            tromcho++;
                            break;
                        case 72:
                            if (io.param == 8) {
                                isActSet = true;
                                set8++;
                            }
                            break;
                        case 21:
                            if (io.param == 80) {
                                setDHD++;
                            }
                            break;
                        case 190: 
                            isActSet = true;
                            setBom++;
                            break;
                    }

                    if (isActSet) {
                        break;
                    }
                }
            } else {
                break;
            }
        }
    }

    private void setDefault() {
        this.songoku = 0;
        this.songokuParam = Integer.MAX_VALUE;
        this.thienXinHang = 0;
        this.thienXinHangParam = Integer.MAX_VALUE;
        this.kirin = 0;
        this.kirinParam = Integer.MAX_VALUE;
        this.ocTieu = 0;
        this.ocTieuParam = Integer.MAX_VALUE;
        this.pikkoroDaimao = 0;
        this.pikkoroDaimaoParam = Integer.MAX_VALUE;
        this.picolo = 0;
        this.picoloParam = Integer.MAX_VALUE;
        this.kakarot = 0;
        this.kakarotParam = Integer.MAX_VALUE;
        this.cadic = 0;
        this.cadicParam = Integer.MAX_VALUE;
        this.nappa = 0;
        this.nappaParam = Integer.MAX_VALUE;
        this.setDHD = 0;
        this.worldcup = 0;
        this.godClothes = false;
        this.ctHaiTac = -1;
        this.tromcho = 0;
        this.set8 = 0;
        this.setBom = 0;
    }

    public boolean isSetBomComplete() {
        return setBom == 5;
    }

    public double getBomDamageMultiplier() {
        return isSetBomComplete() ? 3.0 : 1.0; // 200% increase = 3x damage
    }

    public void dispose() {
        this.player = null;
    }
}
