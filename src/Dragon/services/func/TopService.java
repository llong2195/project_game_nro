package Dragon.services.func;

import com.girlkun.database.GirlkunDB;
import Dragon.server.Manager;
import Dragon.utils.Logger;
import java.sql.Connection;

public class TopService implements Runnable {
    
    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
                if (Manager.timeRealTop + (5 * 60 * 1000) < System.currentTimeMillis()) {
                    Manager.timeRealTop = System.currentTimeMillis();
                    try (Connection con = GirlkunDB.getConnection()) {
//                        Manager.topNV = Manager.realTop(Manager.queryTopNV, con);
                        Manager.topSM = Manager.realTop(Manager.queryTopSM, con);
//                        System.err.print(Manager.topSM.size());
//                        Manager.topSK = Manager.realTop(Manager.queryTopSK, con);
//                        Manager.topRUBY = Manager.realTop(Manager.queryTopRUBY, con);
//                        Manager.topNHS = Manager.realTop(Manager.queryTopNHS, con);
//                        Manager.topSieuHang = Manager.realTopSieuHang(con);
                    } catch (Exception e) {
                        Logger.error("Lỗi đọc top");
                    }
                }
            } catch (Exception e) {
                Logger.error("Lỗi đọc top");
            }
        }
    }
    
}
