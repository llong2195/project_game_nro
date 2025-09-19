package Dragon.data;

import Dragon.models.Template.*;
import Dragon.models.skill.NClass;
import Dragon.models.skill.Skill;
import Dragon.server.Controller;
import com.girlkun.network.io.Message;
import com.girlkun.network.session.ISession;
import Dragon.server.Manager;
import Dragon.server.io.MySession;
import Dragon.services.Service;
import Dragon.utils.FileIO;
import Dragon.utils.Logger;
import Dragon.utils.Util;
import Dragon.utils.IconResizer;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class DataGame {

    public static byte vsData = 83;
    public static byte vsMap = 80;
    public static byte vsSkill = 12;
    public static byte vsItem = 88;
    public static int vsRes = 752012;

    public static String LINK_IP_PORT = "NRO DAU PHA:36.50.135.62:13579:0,0,0";
    private static final String MOUNT_NUM = "733:1,734:2,735:3,743:4,744:5,746:6,795:7,849:8,897:9,920:10,1092:11,1141:15,1445:17,1446:18,1447:19,1448:20,1449:23,1450:24,1451:26,1452:32,1465:33,1473:28,1474:29,1475:30,1476:31,1539:37"
            + ",1540:38,1541:39,1542:40,1629:41,1632:42,1665:55,1666:56,1667:57,1668:58";
    public static final Map MAP_MOUNT_NUM = new HashMap();

    static {
        String[] array = MOUNT_NUM.split(",");
        for (String str : array) {
            String[] data = str.split(":");
            short num = (short) (Short.parseShort(data[1]) + 30000);
            MAP_MOUNT_NUM.put(data[0], num);
        }
        smallVersion.get();
    }

    private DataGame() {

    }

    public static void sendVersionGame(MySession session) {
        Message msg;
        try {
            msg = Service.gI().messageNotMap((byte) 4);
            msg.writer().writeByte(vsData);
            msg.writer().writeByte(vsMap);
            msg.writer().writeByte(vsSkill);
            msg.writer().writeByte(vsItem);
            msg.writer().writeByte(0);
            // Phước Level theo sức mạnh hiện có
            double[] smtieuchuan = {100000D, 950000D, 1625000D, 5562500D, 6906200D, 19765600D, 244140000D, 610350000D,
                1525875000D, 3814688000D, 9536719000D, 23841797000D, 59604492000D, 149011230000D, 372528075000D,
                931320188000D, 2328300470000D, 5820751175000D, 14551877937000D, 36379694842000D, 90949237105000D,
                227373092762000D, 427373092762000D, 647373092762000D, 8473730927000000062L};
            msg.writer().writeByte(smtieuchuan.length);
            for (double l : smtieuchuan) {
                msg.writer().writeDouble(l);
            }
            session.sendMessage(msg);
            if (msg != null) {
                msg.cleanup();
            }
        } catch (Exception e) {
            System.err.print("\nError at 14\n");
            e.printStackTrace();
        }
    }

    // vcData
    public static void updateData(MySession session) {
        Message msg;
        try {
            File dartFile = new File("data/girlkun/update_data/dart");
            File arrowFile = new File("data/girlkun/update_data/arrow");
            File effectFile = new File("data/girlkun/update_data/effect");
            File imageFile = new File("data/girlkun/update_data/image");
            File partFile = new File("data/girlkun/update_data/part");
            File skillFile = new File("data/girlkun/update_data/skill");

            if (dartFile.exists() && arrowFile.exists() && effectFile.exists() && imageFile.exists()
                    && partFile.exists() && skillFile.exists()) {
                byte[] dart = FileIO.readFile(dartFile.getPath());
                byte[] arrow = FileIO.readFile(arrowFile.getPath());
                byte[] effect = FileIO.readFile(effectFile.getPath());
                byte[] image = FileIO.readFile(imageFile.getPath());
                byte[] part = FileIO.readFile(partFile.getPath());
                byte[] skill = FileIO.readFile(skillFile.getPath());

                msg = new Message(-87);
                msg.writer().writeByte(vsData);
                msg.writer().writeInt(dart.length);
                msg.writer().write(dart);
                msg.writer().writeInt(arrow.length);
                msg.writer().write(arrow);
                msg.writer().writeInt(effect.length);
                msg.writer().write(effect);
                msg.writer().writeInt(image.length);
                msg.writer().write(image);
                msg.writer().writeInt(part.length);
                msg.writer().write(part);
                msg.writer().writeInt(skill.length);
                msg.writer().write(skill);

                // Controller.debug("SIZE DATA MSG ??? >>>> " + msg.getData().length);
                session.doSendMessage(msg);
                if (msg != null) {
                    msg.cleanup();
                }
            }
        } catch (Exception e) {
            System.err.print("\nError at 15\n");
            e.printStackTrace();
        }
    }

    // vcMap
    public static void updateMap(MySession session) {
        Message msg;
        try {
            msg = Service.gI().messageNotMap((byte) 6);
            msg.writer().writeByte(vsMap);
            msg.writer().writeByte(Manager.MAP_TEMPLATES.length);
            for (MapTemplate temp : Manager.MAP_TEMPLATES) {
                msg.writer().writeUTF(temp.name);
            }
            msg.writer().writeByte(Manager.NPC_TEMPLATES.size());
            for (NpcTemplate temp : Manager.NPC_TEMPLATES) {
                msg.writer().writeUTF(temp.name);
                msg.writer().writeShort(temp.head);
                msg.writer().writeShort(temp.body);
                msg.writer().writeShort(temp.leg);
                msg.writer().writeByte(0);
            }
            msg.writer().writeByte(Manager.MOB_TEMPLATES.size());
            for (MobTemplate temp : Manager.MOB_TEMPLATES) {
                msg.writer().writeByte(temp.type);
                msg.writer().writeUTF(temp.name);
                msg.writer().writeDouble(temp.hp);
                msg.writer().writeByte(temp.rangeMove);
                msg.writer().writeByte(temp.speed);
                msg.writer().writeByte(temp.dartType);
            }
            session.sendMessage(msg);
            if (msg != null) {
                msg.cleanup();
            }
        } catch (Exception e) {
            System.err.print("\nError at 16\n");
            e.printStackTrace();
        }
    }

    // vcSkill
    public static void updateSkill(MySession session) {
        // System.out.println("update skill");
        Message msg;
        try {
            msg = new Message(-28);

            msg.writer().writeByte(7);
            msg.writer().writeByte(vsSkill);
            msg.writer().writeByte(0); // count skill option

            msg.writer().writeByte(Manager.NCLASS.size());
            for (NClass nClass : Manager.NCLASS) {
                msg.writer().writeUTF(nClass.name);

                msg.writer().writeByte(nClass.skillTemplatess.size());
                for (SkillTemplate skillTemp : nClass.skillTemplatess) {
                    msg.writer().writeByte(skillTemp.id);
                    msg.writer().writeUTF(skillTemp.name);
                    msg.writer().writeByte(skillTemp.maxPoint);
                    msg.writer().writeByte(skillTemp.manaUseType);
                    msg.writer().writeByte(skillTemp.type);
                    msg.writer().writeShort(skillTemp.iconId);
                    msg.writer().writeUTF(skillTemp.damInfo);
                    msg.writer().writeUTF("Bảng Kỹ Năng");
                    if (skillTemp.id != 0) {
                        msg.writer().writeByte(skillTemp.skillss.size());
                        for (Skill skill : skillTemp.skillss) {
                            msg.writer().writeShort(skill.skillId);
                            msg.writer().writeByte(skill.point);
                            msg.writer().writeLong(skill.powRequire);
                            msg.writer().writeShort(skill.manaUse);
                            msg.writer().writeInt(skill.coolDown);
                            msg.writer().writeShort(skill.dx);
                            msg.writer().writeShort(skill.dy);
                            msg.writer().writeByte(skill.maxFight);
                            msg.writer().writeShort(skill.damage);
                            msg.writer().writeShort(skill.price);
                            msg.writer().writeUTF(skill.moreInfo);
                        }
                    } else {
                        // Thêm 2 skill trống 105, 106
                        msg.writer().writeByte(skillTemp.skillss.size() + 2);
                        for (Skill skill : skillTemp.skillss) {
                            msg.writer().writeShort(skill.skillId);
                            msg.writer().writeByte(skill.point);
                            msg.writer().writeLong(skill.powRequire);
                            msg.writer().writeShort(skill.manaUse);
                            msg.writer().writeInt(skill.coolDown);
                            msg.writer().writeShort(skill.dx);
                            msg.writer().writeShort(skill.dy);
                            msg.writer().writeByte(skill.maxFight);
                            msg.writer().writeShort(skill.damage);
                            msg.writer().writeShort(skill.price);
                            msg.writer().writeUTF(skill.moreInfo);
                        }
                        for (int i = 105; i <= 106; i++) {
                            msg.writer().writeShort(i);
                            msg.writer().writeByte(0);
                            msg.writer().writeLong(0);
                            msg.writer().writeShort(0);
                            msg.writer().writeInt(0);
                            msg.writer().writeShort(0);
                            msg.writer().writeShort(0);
                            msg.writer().writeByte(0);
                            msg.writer().writeShort(0);
                            msg.writer().writeShort(0);
                            msg.writer().writeUTF("");
                        }
                    }
                }
            }
            session.doSendMessage(msg);
            if (msg != null) {
                msg.cleanup();
            }
        } catch (Exception e) {
            System.err.print("\nError at 17\n");
            e.printStackTrace();
        }
    }

    public static void sendDataImageVersion(MySession session) {
        Message msg = null;
        try {
            File imgVersionFile = new File("data/girlkun/data_img_version/x" + session.zoomLevel + "/img_version");
            if (imgVersionFile.exists()) {
                msg = new Message(-111);
                msg.writer().write(FileIO.readFile(imgVersionFile.getPath()));
                session.doSendMessage(msg);
                if (msg != null) {
                    msg.cleanup();
                }
            } else {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        } catch (Exception e) {
            System.err.print("\nError at 18\n");
            e.printStackTrace();
        }
    }

    // --------------------------------------------------------------------------
    public static void sendEffectTemplate(MySession session, int id) {
        Message msg = null;
        try {
            File effFile = new File("data/girlkun/effdata/x" + session.zoomLevel + "/" + id);

            if (effFile.exists()) { // Kiểm tra xem tệp có tồn tại hay không
                byte[] eff_data = FileIO.readFile(effFile.getPath());
                msg = new Message(-66);
                msg.writer().write(eff_data);
                session.sendMessage(msg);
                if (msg != null) {
                    msg.cleanup();
                }
            } else {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        } catch (Exception e) {
            System.err.print("\nError at 18\n");
            e.printStackTrace();
        }

    }

    public static void effData(MySession session, int id, int... idtemp) {
        if (id >= 205 && id <= 209) {
            sendEffectTemplate(session, id);
            return;
        }
        int idT = id;
        if (idtemp.length > 0 && idtemp[0] != 0) {
            idT = idtemp[0];
        }
        Message msg = null;
        try {
            File effDataFile = new File("data/girlkun/effect/x" + session.zoomLevel + "/data/DataEffect_" + idT);
            File effImgFile = new File("data/girlkun/effect/x" + session.zoomLevel + "/img/ImgEffect_" + idT + ".png");

            if (effDataFile.exists() && effImgFile.exists()) { // Kiểm tra xem cả hai tệp có tồn tại hay không
                byte[] effData = FileIO.readFile(effDataFile.getPath());
                byte[] effImg = FileIO.readFile(effImgFile.getPath());

                msg = new Message(-66);
                msg.writer().writeShort(id);
                msg.writer().writeInt(effData.length);
                msg.writer().write(effData);
                msg.writer().writeByte(0);
                msg.writer().writeInt(effImg.length);
                msg.writer().write(effImg);

                session.sendMessage(msg);
                if (msg != null) {
                    msg.cleanup();
                }
            } else {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        } catch (Exception e) {
            System.err.print("\nError at 19\n");
            e.printStackTrace();
        }

    }

    public static void sendItemBGTemplate(MySession session, int id) {
        Message msg = null;
        try {
            File bgTempFile = new File("data/girlkun/item_bg_temp/x" + session.zoomLevel + "/" + id + ".png");

            if (bgTempFile.exists()) { // Kiểm tra xem tệp có tồn tại hay không
                byte[] bg_temp = FileIO.readFile(bgTempFile.getPath());
                if (bg_temp != null) {
                    msg = new Message(-32);
                    msg.writer().writeShort(id);
                    msg.writer().writeInt(bg_temp.length);
                    msg.writer().write(bg_temp);
                    session.sendMessage(msg);
                    if (msg != null) {
                        msg.cleanup();
                    }
                }
            } else {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        } catch (Exception e) {
            System.err.print("\nError at 20\n");
            e.printStackTrace();
        }

    }

    public static void sendDataItemBG(MySession session) {
        Message msg = null;
        try {
            File itemBgFile = new File("data/girlkun/item_bg_temp/item_bg_data");

            if (itemBgFile.exists()) { // Kiểm tra xem tệp có tồn tại hay không
                byte[] item_bg = FileIO.readFile(itemBgFile.getPath());
                if (item_bg != null) {
                    msg = new Message(-31);
                    msg.writer().write(item_bg);
                    session.sendMessage(msg);
                    if (msg != null) {
                        msg.cleanup();
                    }
                }
            } else {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        } catch (Exception e) {
            System.err.print("\nError at 21\n");
            e.printStackTrace();
        }

    }

    public static void antiKeoRes(MySession session, short from, short to) {
        session.isRIcon = true;
        Message msg;
        try {
            for (int i = from; i < to; i++) {
                try {
                    byte[] icon = Util.randomImg();
                    msg = new Message(-67);
                    msg.writer().writeInt(i);
                    msg.writer().writeInt(icon.length);
                    msg.writer().write(icon);
                    session.doSendMessage(msg);
                    msg.cleanup();
                } catch (Exception ex) {
                }
            }
            session.disconnect();
        } catch (Exception e) {
        }
    }

    public static void sendIcon(MySession session, int id) {
        Message msg = null;
        try {
            // Sử dụng IconResizer để lấy icon từ folder x4 và resize theo zoomLevel
            byte[] icon = IconResizer.getIcon(id, session.zoomLevel);
            if (icon != null) {
                msg = new Message(-67);
                msg.writer().writeInt(id);
                msg.writer().writeInt(icon.length);
                msg.writer().write(icon);
                session.sendMessage(msg);
                if (msg != null) {
                    msg.cleanup();
                }
            } else {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        } catch (Exception e) {
            System.err.print("\nError at sendIcon for ID " + id + "\n");
            e.printStackTrace();
        }
    }

    public static void sendSmallVersion(MySession session) {
        Message msg = null;
        try {
            File imgVersionFile = new File("data/girlkun/data_img_version/x" + session.zoomLevel + "/img_version");

            if (imgVersionFile.exists()) { // Kiểm tra xem tệp có tồn tại hay không
                byte[] data = FileIO.readFile(imgVersionFile.getPath());
                if (data != null) {
                    msg = new Message(-77);
                    msg.writer().write(data);
                }
                session.sendMessage(msg);
            }
            if (msg != null) {
                msg.cleanup();
            }
        } catch (Exception e) {
            System.err.print("\nError at 22\n");
            e.printStackTrace();
        }

    }

    private static List<Integer> list = new ArrayList<>();

    public static void requestMobTemplate(MySession session, int id) {
        Message msg;
        try {
            byte[] mob = FileIO.readFile("data/girlkun/mob/x" + session.zoomLevel + "/" + id);
            msg = new Message(11);
            if (id >= 90 && id <= 93) {
                msg.writer().writeByte(id);
                msg.writer().writeByte(0);
            } else if (id < 82) {
                msg.writer().writeByte(id);
            }
            msg.writer().write(mob);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            System.out.println("        loi 11");
        }

    }

    public static void sendTileSetInfo(MySession session) {
        Message msg = null;
        try {
            File tileSetFile = new File("data/girlkun/map/tile_set_info");

            if (tileSetFile.exists()) { // Kiểm tra xem tệp có tồn tại hay không
                msg = new Message(-82);
                msg.writer().write(FileIO.readFile(tileSetFile.getPath()));
                session.sendMessage(msg);
            }
            if (msg != null) {
                msg.cleanup();
            }
        } catch (Exception e) {
            System.err.print("\nError at 24\n");
            e.printStackTrace();
        }
    }

    // data vẽ map
    public static void sendMapTemp(MySession session, int id) {
        Message msg = null;
        try {
            File mapDataFile = new File("data/girlkun/map/tile_map_data/" + id);

            if (mapDataFile.exists()) { // Kiểm tra xem tệp có tồn tại hay không
                msg = new Message(-28);
                msg.writer().writeByte(10);
                msg.writer().write(FileIO.readFile(mapDataFile.getPath()));
                session.sendMessage(msg);

            }
            if (msg != null) {
                msg.cleanup();
            }
        } catch (Exception e) {
            System.err.print("\nError at 25\n");
            e.printStackTrace();
        }
    }

    // head-avatar
    public static void sendHeadAvatar(Message msg) {
        try {
            msg.writer().writeShort(Manager.HEAD_AVATARS.size());
            for (HeadAvatar ha : Manager.HEAD_AVATARS) {
                msg.writer().writeShort(ha.headId);
                msg.writer().writeShort(ha.avatarId);
            }
        } catch (Exception e) {
            System.err.print("\nError at 26\n");
            e.printStackTrace();
        }
    }

    public static void sendImageByName(MySession session, String imgName) {
        Message msg;
        try {
            File file = new File("data/girlkun/img_by_name/x" + session.zoomLevel + "/" + imgName + ".png");
            if (file.exists()) { // Kiểm tra xem tệp tồn tại hay không
                msg = new Message(66);
                msg.writer().writeUTF(imgName);
                msg.writer().writeByte(Manager.getNFrameImageByName(imgName));

                byte[] data = FileIO.readFile(file.getPath());
                assert data != null;
                msg.writer().writeInt(data.length);
                msg.writer().write(data);

                session.sendMessage(msg);
                if (msg != null) {
                    msg.cleanup();
                }
            }
        } catch (Exception e) {
            System.err.print("\nError at 27\n");
            e.printStackTrace();
        }
    }

    // download data res --------------------------------------------------------
    public static void sendVersionRes(ISession session) {
        Message msg;
        try {
            msg = new Message(-74);
            msg.writer().writeByte(0);
            msg.writer().writeInt(vsRes);
            session.sendMessage(msg);
            if (msg != null) {
                msg.cleanup();
            }
        } catch (Exception e) {
            System.err.print("\nError at 28\n");
            e.printStackTrace();
        }
    }

    public static void sendSizeRes(MySession session) {
        Message msg = null;
        try {
            File resFolder = new File("data/girlkun/res/x" + session.zoomLevel);

            if (resFolder.exists() && resFolder.isDirectory()) {
                File[] files = resFolder.listFiles();
                if (files != null) {
                    msg = new Message(-74);
                    msg.writer().writeByte(1);
                    msg.writer().writeShort(files.length);
                    session.sendMessage(msg);

                }
            }
            if (msg != null) {
                msg.cleanup();
            }
        } catch (Exception e) {
            System.err.print("\nError at 29\n");
            e.printStackTrace();
        }
    }

    public static void sendRes(MySession session) {
        if (session != null && session.zoomLevel != 0) {
            new Thread(() -> {
                Message msg;
                try {
                    for (final File fileEntry : new File("data/girlkun/res/x" + session.zoomLevel).listFiles()) {
                        String original = fileEntry.getName();
                        byte[] res = FileIO.readFile(fileEntry.getAbsolutePath());
                        msg = new Message(-74);
                        msg.writer().writeByte(2);
                        msg.writer().writeUTF(original);
                        msg.writer().writeInt(res.length);
                        msg.writer().write(res);
                        msg.writer().flush();
                        session.sendMessage(msg);
                        msg.cleanup();
                    }
                    msg = new Message(-74);
                    msg.writer().writeByte(3);
                    msg.writer().writeInt(vsRes);
                    session.sendMessage(msg);
                    msg.cleanup();
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                }
            }).start();
        }
    }

    public static void sendLinkIP(MySession session) {
        Message msg;
        try {
            msg = new Message(-29);
            msg.writer().writeByte(2);
            msg.writer().writeUTF(LINK_IP_PORT + ",0,0");
            msg.writer().writeByte(1);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            System.out.println("        loi 20");
        }
    }

    /**
     * -server -Xms24G -Xmx24G -XX:PermSize=512m -XX:+UseG1GC
     * -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=20 -XX:ConcGCThreads=5
     * -XX:InitiatingHeapOccupancyPercent=70
     */
}
